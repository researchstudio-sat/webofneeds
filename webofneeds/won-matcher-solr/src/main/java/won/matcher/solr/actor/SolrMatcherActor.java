package won.matcher.solr.actor;

import akka.actor.OneForOneStrategy;
import akka.actor.SupervisorStrategy;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Function;
import com.github.jsonldjava.core.JsonLdError;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Statement;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocumentList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import scala.concurrent.duration.Duration;
import won.matcher.service.common.event.BulkHintEvent;
import won.matcher.service.common.event.BulkNeedEvent;
import won.matcher.service.common.event.NeedEvent;
import won.matcher.solr.config.SolrMatcherConfig;
import won.matcher.solr.hints.HintBuilder;
import won.matcher.solr.index.NeedIndexer;
import won.matcher.solr.query.DefaultMatcherQueryExecuter;
import won.matcher.solr.query.SolrMatcherQueryExecutor;
import won.matcher.solr.query.TestMatcherQueryExecutor;
import won.matcher.solr.query.factory.*;
import won.protocol.util.NeedModelWrapper;
import won.protocol.vocabulary.WON;

import java.io.IOException;
import java.time.temporal.ChronoUnit;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

/**
 * Siren/Solr based abstract matcher with all implementations for querying as well as indexing needs.
 */
@Component
@Scope("prototype")
public class SolrMatcherActor extends UntypedActor {
    private LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    @Autowired
    private SolrMatcherConfig config;

    @Autowired
    private HintBuilder hintBuilder;

    @Autowired
    private NeedIndexer needIndexer;

    @Autowired
    @Qualifier("defaultMatcherQueryExecuter")
    DefaultMatcherQueryExecuter defaultQueryExecuter;

    @Autowired
    TestMatcherQueryExecutor testQueryExecuter;

    @Override
    public void onReceive(final Object o) throws Exception {
        String eventTypeForLogging = "unknown";
        Optional<String> uriForLogging = Optional.empty();
        try {
            if (o instanceof NeedEvent) {
                eventTypeForLogging = "NeedEvent";
                NeedEvent needEvent = (NeedEvent) o;
                uriForLogging = Optional.ofNullable(needEvent.getUri());
                if (needEvent.getEventType().equals(NeedEvent.TYPE.ACTIVE)) {
                    processActiveNeedEvent(needEvent);
                } else if (needEvent.getEventType().equals(NeedEvent.TYPE.INACTIVE)) {
                    processInactiveNeedEvent(needEvent);
                } else {
                    unhandled(o);
                }
            } else if (o instanceof BulkNeedEvent) {
                eventTypeForLogging = "BulkNeedEvent";
                log.info("received bulk need event, processing {} need events ...",
                        ((BulkNeedEvent) o).getNeedEvents().size());
                for (NeedEvent event : ((BulkNeedEvent) o).getNeedEvents()) {
                    processActiveNeedEvent(event);
                }
            } else {
                eventTypeForLogging = "unhandled";
                unhandled(o);
            }
        } catch (Exception e) {
            log.info(String.format("Caught exception when processing %s event %s. More info on loglevel 'debug'",
                    eventTypeForLogging, uriForLogging.orElse("[no uri available]")));
            log.debug("caught exception", e);
        }
    }

    protected void processInactiveNeedEvent(NeedEvent needEvent) throws IOException, JsonLdError {
        log.info("Add inactive need event content {} to solr index", needEvent);
        needIndexer.index(needEvent.deserializeNeedDataset());
    }

    protected void processActiveNeedEvent(NeedEvent needEvent)
            throws IOException, SolrServerException, JsonLdError {

        log.info("Start processing active need event {}", needEvent);

        // check if the need has doNotMatch flag, then do not use it for querying or indexing
        Dataset dataset = needEvent.deserializeNeedDataset();
        NeedModelWrapper needModelWrapper = new NeedModelWrapper(dataset);
        if (needModelWrapper.hasFlag(WON.NO_HINT_FOR_ME) && needModelWrapper.hasFlag(WON.NO_HINT_FOR_COUNTERPART)) {
            log.info("Discarding received need due to flags won:NoHintForMe and won:NoHintForCounterpart: {}", needEvent);
            return;
        }
        
        // check if need has a sparql query attached
        if (needModelWrapper.hasQuery()) {
            log.debug("Need {} has a sparql query, omitting this need in Solr matcher", needModelWrapper.getNeedUri());
            return;
        }        

        // check if need is usedForTesting only
        boolean usedForTesting = needModelWrapper.hasFlag(WON.USED_FOR_TESTING);
        SolrMatcherQueryExecutor queryExecutor = (usedForTesting ? testQueryExecuter : defaultQueryExecuter);

        // create another query depending if the current need is "WhatsAround" or a default need
        String queryString = null;
        if (needModelWrapper.hasFlag(WON.WHATS_AROUND)) {
            // WhatsAround doesnt match on terms only other needs in close location are boosted
            WhatsAroundQueryFactory qf = new WhatsAroundQueryFactory(dataset);
            queryString = qf.createQuery();
        } else if(needModelWrapper.hasFlag(WON.WHATS_NEW)){
            WhatsNewQueryFactory qf = new WhatsNewQueryFactory(dataset);
            queryString = qf.createQuery();
        }else {
            // default query matches content terms (of fields title, description and tags) with different weights
            // and gives an additional multiplicative boost for geographically closer needs
            DefaultNeedQueryFactory qf = new DefaultNeedQueryFactory(dataset);
            queryString = qf.createQuery();
        }

        // add filters to the query: default filters are
        // - need status active
        // - creation date overlap 1 month
        // - OR-filtering for matching contexts if any were specified

        // now create three slightly different queries for different lists of needs:
        // 1) needs without NoHintForCounterpart => hints for current need
        // 2) needs without NoHintForSelf, excluding WhatsAround needs => hints for needs in index that are not WhatsAround
        // 3) needs without NoHintForSelf that are only WhatsAround needs => hints for needs in index that are WhatsAround
        // to achieve this use a different filters for these queries

        // case 1) needs without NoHintForCounterpart => hints for current need
        List<String> filterQueries = new LinkedList<>();
        filterQueries.add(new NeedStateQueryFactory(dataset).createQuery());
        filterQueries.add(new CreationDateQueryFactory(dataset, 1, ChronoUnit.MONTHS).createQuery());
        filterQueries.add(new BooleanQueryFactory(BooleanQueryFactory.BooleanOperator.NOT, new HasFlagQueryFactory(HasFlagQueryFactory.FLAGS.NO_HINT_FOR_COUNTERPART)).createQuery());
        if (needModelWrapper.getMatchingContexts() != null && needModelWrapper.getMatchingContexts().size() > 0) {
            filterQueries.add(new MatchingContextQueryFactory(needModelWrapper.getMatchingContexts()).createQuery());
        }
        if (!needModelWrapper.hasFlag(WON.NO_HINT_FOR_ME)) {

            // execute the query
            log.info("query Solr endpoint {} for need {} and need list 1 (without NoHintForCounterpart)", config.getSolrEndpointUri(usedForTesting), needEvent.getUri());
            SolrDocumentList docs = queryExecutor.executeNeedQuery(queryString, config.getMaxHints(),null, filterQueries.toArray(new String[filterQueries.size()]));
            if (docs != null) {

                // perform knee detection depending on current need is WhatsAround/WhatsNew or not)
                boolean kneeDetection = needModelWrapper.hasFlag(WON.WHATS_NEW) || needModelWrapper.hasFlag(WON.WHATS_AROUND) ? false : true;

                // generate hints for current need (only generate hints for current need, suppress hints for matched needs,
                BulkHintEvent events = hintBuilder.generateHintsFromSearchResult(docs, needEvent, needModelWrapper, false, true, kneeDetection);

                log.info("Create {} hints for need {} and need list 1 (without NoHintForCounterpart)", events.getHintEvents().size(), needEvent);

                // publish hints to current need
                if (events.getHintEvents().size() != 0) {
                    getSender().tell(events, getSelf());
                }
            } else {
                log.warning("No results found for need list 1 (without NoHintForCounterpart) query of need ", needEvent);
            }
        }

        // case 2) needs without NoHintForSelf, excluding WhatsAround needs => hints for needs in index that are not WhatsAround
        filterQueries = new LinkedList<>();
        filterQueries.add(new NeedStateQueryFactory(dataset).createQuery());
        filterQueries.add(new CreationDateQueryFactory(dataset, 1, ChronoUnit.MONTHS).createQuery());
        filterQueries.add(new BooleanQueryFactory(BooleanQueryFactory.BooleanOperator.NOT, new HasFlagQueryFactory(HasFlagQueryFactory.FLAGS.NO_HINT_FOR_ME)).createQuery());
        filterQueries.add(new BooleanQueryFactory(BooleanQueryFactory.BooleanOperator.NOT, new HasFlagQueryFactory(HasFlagQueryFactory.FLAGS.WHATS_AROUND)).createQuery());
        filterQueries.add(new BooleanQueryFactory(BooleanQueryFactory.BooleanOperator.NOT, new HasFlagQueryFactory(HasFlagQueryFactory.FLAGS.WHATS_NEW)).createQuery());
        if (needModelWrapper.getMatchingContexts() != null && needModelWrapper.getMatchingContexts().size() > 0) {
            filterQueries.add(new MatchingContextQueryFactory(needModelWrapper.getMatchingContexts()).createQuery());
        }
        if (!needModelWrapper.hasFlag(WON.NO_HINT_FOR_COUNTERPART)) {

            // execute the query
            log.info("query Solr endpoint {} for need {} and need list 2 (without NoHintForSelf, excluding WhatsAround needs)", config.getSolrEndpointUri(usedForTesting), needEvent.getUri());
            SolrDocumentList docs = queryExecutor.executeNeedQuery(queryString, config.getMaxHintsForCounterparts(), null, filterQueries.toArray(new String[filterQueries.size()]));
            if (docs != null) {

                // generate hints for matched needs (suppress hints for current need, only generate hints for matched needs, perform knee detection)
                BulkHintEvent events = hintBuilder.generateHintsFromSearchResult(docs, needEvent, needModelWrapper, true, false, true);
                log.info("Create {} hints for need {} and need list 2 (without NoHintForSelf, excluding WhatsAround needs)", events.getHintEvents().size(), needEvent);

                // publish hints to current need
                if (events.getHintEvents().size() != 0) {
                    getSender().tell(events, getSelf());
                }
            } else {
                log.warning("No results found for need list 2 (without NoHintForSelf, excluding WhatsAround needs) query of need ", needEvent);
            }
        }

        // case 3) needs without NoHintForSelf that are only WhatsAround needs => hints for needs in index that are WhatsAround
        filterQueries = new LinkedList<>();
        filterQueries.add(new NeedStateQueryFactory(dataset).createQuery());
        filterQueries.add(new CreationDateQueryFactory(dataset, 1, ChronoUnit.MONTHS).createQuery());
        filterQueries.add(new BooleanQueryFactory(BooleanQueryFactory.BooleanOperator.NOT, new HasFlagQueryFactory(HasFlagQueryFactory.FLAGS.NO_HINT_FOR_ME)).createQuery());
        filterQueries.add(
        		new BooleanQueryFactory(BooleanQueryFactory.BooleanOperator.OR, 
        				new HasFlagQueryFactory(HasFlagQueryFactory.FLAGS.WHATS_AROUND),
        				new HasFlagQueryFactory(HasFlagQueryFactory.FLAGS.WHATS_NEW)).createQuery());
        if (needModelWrapper.getMatchingContexts() != null && needModelWrapper.getMatchingContexts().size() > 0) {
            filterQueries.add(new MatchingContextQueryFactory(needModelWrapper.getMatchingContexts()).createQuery());
        }
        if (!needModelWrapper.hasFlag(WON.NO_HINT_FOR_COUNTERPART)) {

            // hints for WhatsAround Needs should not have the keywords from title, description, tags etc.
            // this can prevent to actually find WhatsAround needs.
            // Instead create a WhatsAround query (query without keywords, just location) to find other WhatsAround needs
            queryString = (new WhatsAroundQueryFactory(dataset)).createQuery();

            // execute the query
            log.info("query Solr endpoint {} for need {} and need list 3 (without NoHintForSelf that are only WhatsAround needs)", config.getSolrEndpointUri(usedForTesting), needEvent.getUri());
            SolrDocumentList docs = queryExecutor.executeNeedQuery(queryString, config.getMaxHintsForCounterparts(), null, filterQueries.toArray(new String[filterQueries.size()]));
            if (docs != null) {

                // generate hints for matched needs (suppress hints for current need, only generate hints for matched needs, do not perform knee detection)
                BulkHintEvent events = hintBuilder.generateHintsFromSearchResult(docs, needEvent, needModelWrapper, true, false, false);
                log.info("Create {} hints for need {} and need list 3 (without NoHintForSelf that are only WhatsAround needs)", events.getHintEvents().size(), needEvent);

                // publish hints to current need
                if (events.getHintEvents().size() != 0) {
                    getSender().tell(events, getSelf());
                }
            } else {
                log.warning("No results found for need list 3 (without NoHintForSelf that are only WhatsAround needs) query of need ", needEvent);
            }
        }

        // index need
        log.info("Add need event content {} to solr index", needEvent);
        needIndexer.index(dataset);
    }

    @Override
    public SupervisorStrategy supervisorStrategy() {

        SupervisorStrategy supervisorStrategy = new OneForOneStrategy(
                0, Duration.Zero(), new Function<Throwable, SupervisorStrategy.Directive>() {

            @Override
            public SupervisorStrategy.Directive apply(Throwable t) throws Exception {

                log.warning("Actor encountered error: {}", t);
                // default behaviour
                return SupervisorStrategy.escalate();
            }
        });

        return supervisorStrategy;
    }

}

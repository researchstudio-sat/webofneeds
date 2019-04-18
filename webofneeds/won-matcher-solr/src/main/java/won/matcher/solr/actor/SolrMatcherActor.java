package won.matcher.solr.actor;

import java.io.IOException;
import java.time.temporal.ChronoUnit;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.apache.jena.query.Dataset;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocumentList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.github.jsonldjava.core.JsonLdError;

import akka.actor.OneForOneStrategy;
import akka.actor.SupervisorStrategy;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Function;
import scala.concurrent.duration.Duration;
import won.matcher.service.common.event.BulkHintEvent;
import won.matcher.service.common.event.BulkAtomEvent;
import won.matcher.service.common.event.AtomEvent;
import won.matcher.solr.config.SolrMatcherConfig;
import won.matcher.solr.hints.HintBuilder;
import won.matcher.solr.index.AtomIndexer;
import won.matcher.solr.query.DefaultMatcherQueryExecuter;
import won.matcher.solr.query.SolrMatcherQueryExecutor;
import won.matcher.solr.query.TestMatcherQueryExecutor;
import won.matcher.solr.query.factory.BooleanQueryFactory;
import won.matcher.solr.query.factory.CreationDateQueryFactory;
import won.matcher.solr.query.factory.DefaultAtomQueryFactory;
import won.matcher.solr.query.factory.HasFlagQueryFactory;
import won.matcher.solr.query.factory.MatchingContextQueryFactory;
import won.matcher.solr.query.factory.AtomStateQueryFactory;
import won.matcher.solr.query.factory.WhatsAroundQueryFactory;
import won.matcher.solr.query.factory.WhatsNewQueryFactory;
import won.protocol.util.AtomModelWrapper;
import won.protocol.vocabulary.WON;

/**
 * Siren/Solr based abstract matcher with all implementations for querying as
 * well as indexing atoms.
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
    private AtomIndexer atomIndexer;
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
            if (o instanceof AtomEvent) {
                eventTypeForLogging = "AtomEvent";
                AtomEvent atomEvent = (AtomEvent) o;
                uriForLogging = Optional.ofNullable(atomEvent.getUri());
                if (atomEvent.getEventType().equals(AtomEvent.TYPE.ACTIVE)) {
                    processActiveAtomEvent(atomEvent);
                } else if (atomEvent.getEventType().equals(AtomEvent.TYPE.INACTIVE)) {
                    processInactiveAtomEvent(atomEvent);
                } else {
                    unhandled(o);
                }
            } else if (o instanceof BulkAtomEvent) {
                eventTypeForLogging = "BulkAtomEvent";
                log.info("received bulk atom event, processing {} atom events ...",
                                ((BulkAtomEvent) o).getAtomEvents().size());
                for (AtomEvent event : ((BulkAtomEvent) o).getAtomEvents()) {
                    processActiveAtomEvent(event);
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

    protected void processInactiveAtomEvent(AtomEvent atomEvent) throws IOException, JsonLdError {
        log.info("Add inactive atom event content {} to solr index", atomEvent);
        atomIndexer.index(atomEvent.deserializeAtomDataset());
    }

    protected void processActiveAtomEvent(AtomEvent atomEvent) throws IOException, SolrServerException, JsonLdError {
        log.info("Start processing active atom event {}", atomEvent);
        // check if the atom has doNotMatch flag, then do not use it for querying or
        // indexing
        Dataset dataset = atomEvent.deserializeAtomDataset();
        AtomModelWrapper atomModelWrapper = new AtomModelWrapper(dataset);
        if (atomModelWrapper.flag(WON.NoHintForMe) && atomModelWrapper.flag(WON.NoHintForCounterpart)) {
            log.info("Discarding received atom due to flags won:NoHintForMe and won:NoHintForCounterpart: {}",
                            atomEvent);
            return;
        }
        // check if atom has a sparql query attached
        if (atomModelWrapper.sparqlQuery()) {
            log.debug("Atom {} has a sparql query, omitting this atom in Solr matcher", atomModelWrapper.getAtomUri());
            return;
        }
        // check if atom is usedForTesting only
        boolean usedForTesting = atomModelWrapper.flag(WON.UsedForTesting);
        SolrMatcherQueryExecutor queryExecutor = (usedForTesting ? testQueryExecuter : defaultQueryExecuter);
        // create another query depending if the current atom is "WhatsAround" or a
        // default atom
        String queryString = null;
        if (atomModelWrapper.flag(WON.WhatsAround)) {
            // WhatsAround doesnt match on terms only other atoms in close location are
            // boosted
            WhatsAroundQueryFactory qf = new WhatsAroundQueryFactory(dataset);
            queryString = qf.createQuery();
        } else if (atomModelWrapper.flag(WON.WhatsNew)) {
            WhatsNewQueryFactory qf = new WhatsNewQueryFactory(dataset);
            queryString = qf.createQuery();
        } else {
            // default query matches content terms (of fields title, description and tags)
            // with different weights
            // and gives an additional multiplicative boost for geographically closer atoms
            DefaultAtomQueryFactory qf = new DefaultAtomQueryFactory(dataset);
            queryString = qf.createQuery();
        }
        // add filters to the query: default filters are
        // - atom status active
        // - creation date overlap 1 month
        // - OR-filtering for matching contexts if any were specified
        // now create three slightly different queries for different lists of atoms:
        // 1) atoms without NoHintForCounterpart => hints for current atom
        // 2) atoms without NoHintForSelf, excluding WhatsAround atoms => hints for
        // atoms in index that are not WhatsAround
        // 3) atoms without NoHintForSelf that are only WhatsAround atoms => hints for
        // atoms in index that are WhatsAround
        // to achieve this use a different filters for these queries
        // case 1) atoms without NoHintForCounterpart => hints for current atom
        List<String> filterQueries = new LinkedList<>();
        filterQueries.add(new AtomStateQueryFactory(dataset).createQuery());
        filterQueries.add(new CreationDateQueryFactory(dataset, 1, ChronoUnit.MONTHS).createQuery());
        filterQueries.add(new BooleanQueryFactory(BooleanQueryFactory.BooleanOperator.NOT,
                        new HasFlagQueryFactory(HasFlagQueryFactory.FLAGS.NO_HINT_FOR_COUNTERPART)).createQuery());
        if (atomModelWrapper.getMatchingContexts() != null && atomModelWrapper.getMatchingContexts().size() > 0) {
            filterQueries.add(new MatchingContextQueryFactory(atomModelWrapper.getMatchingContexts()).createQuery());
        }
        if (!atomModelWrapper.flag(WON.NoHintForMe)) {
            // execute the query
            log.info("query Solr endpoint {} for atom {} and atom list 1 (without NoHintForCounterpart)",
                            config.getSolrEndpointUri(usedForTesting), atomEvent.getUri());
            SolrDocumentList docs = queryExecutor.executeAtomQuery(queryString, config.getMaxHints(), null,
                            filterQueries.toArray(new String[filterQueries.size()]));
            if (docs != null) {
                // perform knee detection depending on current atom is WhatsAround/WhatsNew or
                // not)
                boolean kneeDetection = atomModelWrapper.flag(WON.WhatsNew) || atomModelWrapper.flag(WON.WhatsAround)
                                ? false
                                : true;
                // generate hints for current atom (only generate hints for current atom,
                // suppress hints for matched atoms,
                BulkHintEvent events = hintBuilder.generateHintsFromSearchResult(docs, atomEvent, atomModelWrapper,
                                false, true, kneeDetection);
                log.info("Create {} hints for atom {} and atom list 1 (without NoHintForCounterpart)",
                                events.getHintEvents().size(), atomEvent);
                // publish hints to current atom
                if (events.getHintEvents().size() != 0) {
                    getSender().tell(events, getSelf());
                }
            } else {
                log.warning("No results found for atom list 1 (without NoHintForCounterpart) query of atom ",
                                atomEvent);
            }
        }
        // case 2) atoms without NoHintForSelf, excluding WhatsAround atoms => hints for
        // atoms in index that are not WhatsAround
        filterQueries = new LinkedList<>();
        filterQueries.add(new AtomStateQueryFactory(dataset).createQuery());
        filterQueries.add(new CreationDateQueryFactory(dataset, 1, ChronoUnit.MONTHS).createQuery());
        filterQueries.add(new BooleanQueryFactory(BooleanQueryFactory.BooleanOperator.NOT,
                        new HasFlagQueryFactory(HasFlagQueryFactory.FLAGS.NO_HINT_FOR_ME)).createQuery());
        filterQueries.add(new BooleanQueryFactory(BooleanQueryFactory.BooleanOperator.NOT,
                        new HasFlagQueryFactory(HasFlagQueryFactory.FLAGS.WHATS_AROUND)).createQuery());
        filterQueries.add(new BooleanQueryFactory(BooleanQueryFactory.BooleanOperator.NOT,
                        new HasFlagQueryFactory(HasFlagQueryFactory.FLAGS.WHATS_NEW)).createQuery());
        if (atomModelWrapper.getMatchingContexts() != null && atomModelWrapper.getMatchingContexts().size() > 0) {
            filterQueries.add(new MatchingContextQueryFactory(atomModelWrapper.getMatchingContexts()).createQuery());
        }
        if (!atomModelWrapper.flag(WON.NoHintForCounterpart)) {
            // execute the query
            log.info("query Solr endpoint {} for atom {} and atom list 2 (without NoHintForSelf, excluding WhatsAround atoms)",
                            config.getSolrEndpointUri(usedForTesting), atomEvent.getUri());
            SolrDocumentList docs = queryExecutor.executeAtomQuery(queryString, config.getMaxHintsForCounterparts(),
                            null, filterQueries.toArray(new String[filterQueries.size()]));
            if (docs != null) {
                // generate hints for matched atoms (suppress hints for current atom, only
                // generate hints for matched atoms, perform knee detection)
                BulkHintEvent events = hintBuilder.generateHintsFromSearchResult(docs, atomEvent, atomModelWrapper,
                                true, false, true);
                log.info("Create {} hints for atom {} and atom list 2 (without NoHintForSelf, excluding WhatsAround atoms)",
                                events.getHintEvents().size(), atomEvent);
                // publish hints to current atom
                if (events.getHintEvents().size() != 0) {
                    getSender().tell(events, getSelf());
                }
            } else {
                log.warning("No results found for atom list 2 (without NoHintForSelf, excluding WhatsAround atoms) query of atom ",
                                atomEvent);
            }
        }
        // case 3) atoms without NoHintForSelf that are only WhatsAround atoms => hints
        // for atoms in index that are WhatsAround
        filterQueries = new LinkedList<>();
        filterQueries.add(new AtomStateQueryFactory(dataset).createQuery());
        filterQueries.add(new CreationDateQueryFactory(dataset, 1, ChronoUnit.MONTHS).createQuery());
        filterQueries.add(new BooleanQueryFactory(BooleanQueryFactory.BooleanOperator.NOT,
                        new HasFlagQueryFactory(HasFlagQueryFactory.FLAGS.NO_HINT_FOR_ME)).createQuery());
        filterQueries.add(new BooleanQueryFactory(BooleanQueryFactory.BooleanOperator.OR,
                        new HasFlagQueryFactory(HasFlagQueryFactory.FLAGS.WHATS_AROUND),
                        new HasFlagQueryFactory(HasFlagQueryFactory.FLAGS.WHATS_NEW)).createQuery());
        if (atomModelWrapper.getMatchingContexts() != null && atomModelWrapper.getMatchingContexts().size() > 0) {
            filterQueries.add(new MatchingContextQueryFactory(atomModelWrapper.getMatchingContexts()).createQuery());
        }
        if (!atomModelWrapper.flag(WON.NoHintForCounterpart)) {
            // hints for WhatsAround Atoms should not have the keywords from title,
            // description, tags etc.
            // this can prevent to actually find WhatsAround atoms.
            // Instead create a WhatsAround query (query without keywords, just location) to
            // find other WhatsAround atoms
            queryString = (new WhatsAroundQueryFactory(dataset)).createQuery();
            // execute the query
            log.info("query Solr endpoint {} for atom {} and atom list 3 (without NoHintForSelf that are only WhatsAround atoms)",
                            config.getSolrEndpointUri(usedForTesting), atomEvent.getUri());
            SolrDocumentList docs = queryExecutor.executeAtomQuery(queryString, config.getMaxHintsForCounterparts(),
                            null, filterQueries.toArray(new String[filterQueries.size()]));
            if (docs != null) {
                // generate hints for matched atoms (suppress hints for current atom, only
                // generate hints for matched atoms, do not perform knee detection)
                BulkHintEvent events = hintBuilder.generateHintsFromSearchResult(docs, atomEvent, atomModelWrapper,
                                true, false, false);
                log.info("Create {} hints for atom {} and atom list 3 (without NoHintForSelf that are only WhatsAround atoms)",
                                events.getHintEvents().size(), atomEvent);
                // publish hints to current atom
                if (events.getHintEvents().size() != 0) {
                    getSender().tell(events, getSelf());
                }
            } else {
                log.warning("No results found for atom list 3 (without NoHintForSelf that are only WhatsAround atoms) query of atom ",
                                atomEvent);
            }
        }
        // index atom
        log.info("Add atom event content {} to solr index", atomEvent);
        atomIndexer.index(dataset);
    }

    @Override
    public SupervisorStrategy supervisorStrategy() {
        SupervisorStrategy supervisorStrategy = new OneForOneStrategy(0, Duration.Zero(),
                        new Function<Throwable, SupervisorStrategy.Directive>() {
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

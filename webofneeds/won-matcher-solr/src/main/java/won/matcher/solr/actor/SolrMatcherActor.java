package won.matcher.solr.actor;

import akka.actor.OneForOneStrategy;
import akka.actor.SupervisorStrategy;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Function;
import com.github.jsonldjava.core.JsonLdError;
import org.apache.jena.query.Dataset;
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
import won.matcher.solr.query.factory.CreationDateQueryFactory;
import won.matcher.solr.query.factory.DefaultNeedQueryFactory;
import won.matcher.solr.query.factory.NeedStateQueryFactory;
import won.protocol.util.NeedModelWrapper;
import won.protocol.vocabulary.WON;

import java.io.IOException;
import java.time.temporal.ChronoUnit;

/**
 * Siren/Solr based abstract matcher with all implementations for querying as well as indexing needs.
 */
@Component
@Scope("prototype")
public abstract class SolrMatcherActor extends UntypedActor
{
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

    if (o instanceof NeedEvent) {
        NeedEvent needEvent = (NeedEvent) o;
        processNeedEvent(needEvent);
    } else if (o instanceof BulkNeedEvent) {
        log.info("received bulk need event, processing {} need events ...", ((BulkNeedEvent) o).getNeedEvents().size());
        for (NeedEvent event : ((BulkNeedEvent) o).getNeedEvents()) {
          processNeedEvent(event);
        }
    } else {
      unhandled(o);
    }
  }

  protected void processNeedEvent(NeedEvent needEvent)
    throws IOException, SolrServerException, JsonLdError {

    log.info("Start processing need event {}", needEvent);

    // check if the need has doNotMatch flag, then do not use it for querying or indexing
    Dataset dataset = needEvent.deserializeNeedDataset();
    NeedModelWrapper needModelWrapper = new NeedModelWrapper(dataset);
    if (needModelWrapper.hasFlag(WON.NO_HINT_FOR_ME) && needModelWrapper.hasFlag(WON.NO_HINT_FOR_COUNTERPART)) {
      log.info("Discarding received need due to flags won:NoHintForMe and won:NoHintForCounterpart: {}", needEvent);
      return;
    }

    // check if need is usedForTesting only
    boolean usedForTesting = needModelWrapper.hasFlag(WON.USED_FOR_TESTING);
    SolrMatcherQueryExecutor queryExecutor = (usedForTesting ? testQueryExecuter : defaultQueryExecuter);


    // default query matches content terms (of fields title, description and tags) with different weights
    // and gives an additional multiplicative boost for geographically closer needs
    DefaultNeedQueryFactory needQueryFactory = new DefaultNeedQueryFactory(dataset);
    String queryString = needQueryFactory.createQuery();

    // add filters to the query: need status active and creation date overlap 1 month
    String[] filterQueries = new String[2];
    filterQueries[0] = new NeedStateQueryFactory(dataset).createQuery();
    filterQueries[1] = new CreationDateQueryFactory(dataset, 1, ChronoUnit.MONTHS).createQuery();

    log.info("query Solr endpoint {} for need {}", config.getSolrEndpointUri(usedForTesting), needEvent.getUri());
    SolrDocumentList docs = queryExecutor.executeNeedQuery(queryString, null, filterQueries);

      // publish hints
    if (docs != null) {
      BulkHintEvent events = hintBuilder.generateHintsFromSearchResult(docs, needEvent, needModelWrapper);
        log.info("Create {} hints for need {}", events.getHintEvents().size(), needEvent);
        if (events.getHintEvents().size() != 0) {
            log.debug("Publish bulk hint event: " + events);
            getSender().tell(events, getSelf());
        }
    } else {
      log.warning("No results found for query of need ", needEvent);
    }

      // index need
      log.info("Add need event content {} to solr index", needEvent);
      needIndexer.index(dataset);
  }

  @Override
  public SupervisorStrategy supervisorStrategy() {

    SupervisorStrategy supervisorStrategy = new OneForOneStrategy(
      0, Duration.Zero(), new Function<Throwable, SupervisorStrategy.Directive>()
    {

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

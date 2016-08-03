package won.matcher.solr.actor;

import akka.actor.OneForOneStrategy;
import akka.actor.SupervisorStrategy;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Function;
import com.github.jsonldjava.core.JsonLdError;
import com.hp.hpl.jena.query.Dataset;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import scala.concurrent.duration.Duration;
import won.matcher.service.common.event.BulkHintEvent;
import won.matcher.service.common.event.NeedEvent;
import won.matcher.solr.config.SolrMatcherConfig;
import won.matcher.solr.hints.HintBuilder;
import won.matcher.solr.index.NeedIndexer;
import won.matcher.solr.query.BasicNeedQueryFactory;

import java.io.IOException;

/**
 * Siren/Solr based abstract matcher with all implementations for querying as well as indexing needs.
 */
@Component
@Scope("prototype")
public abstract class AbstractSolrMatcherActor extends UntypedActor
{
  private LoggingAdapter log = Logging.getLogger(getContext().system(), this);

  @Autowired
  private SolrMatcherConfig config;

  @Autowired
  private HintBuilder hintBuilder;

  @Autowired
  private NeedIndexer needIndexer;

  SolrClient solrClient;

  @Override
  public void preStart() {
    solrClient = new HttpSolrClient.Builder(config.getSolrServerUri()).build();
  }

  @Override
  public void onReceive(final Object o) throws Exception {

    if (o instanceof NeedEvent) {
        NeedEvent needEvent = (NeedEvent) o;
        processNeedEvent(needEvent);
    } else {
      unhandled(o);
    }
  }

  protected void processNeedEvent(NeedEvent needEvent)
    throws IOException, SolrServerException, JsonLdError {

    Dataset dataset = deserializeNeed(needEvent);

    BasicNeedQueryFactory needQuery = new BasicNeedQueryFactory(dataset);
    needQuery.addTermsToTitleQuery(needQuery.getTitleTerms(), 4);
    needQuery.addTermsToTitleQuery(needQuery.getTagTerms(), 2);
    needQuery.addTermsToTagQuery(needQuery.getTagTerms(), 4);
    needQuery.addTermsToTagQuery(needQuery.getTitleTerms(), 2);
    needQuery.addTermsToDescriptionQuery(needQuery.getTitleTerms(), 1);
    needQuery.addTermsToDescriptionQuery(needQuery.getTagTerms(), 1);
    needQuery.addTermsToDescriptionQuery(needQuery.getDescriptionTerms(), 1);

    String query = needQuery.createQuery();
    SolrDocumentList docs = executeQuery(query);

    if (docs != null) {
      log.debug("{} results found for query", docs.size());
      BulkHintEvent events = produceHints(docs, needEvent);
      publishHints(events, needEvent);
    } else {
      log.warning("No results found for query {}", query);
    }

    indexNeedEvent(needEvent, dataset);
  }

  protected Dataset deserializeNeed(NeedEvent needEvent) throws IOException {
    Dataset dataset = needEvent.deserializeNeedDataset();
    return dataset;
  }

  protected SolrDocumentList executeQuery(String queryString) throws IOException, SolrServerException {

    SolrQuery query = new SolrQuery();
    log.debug("query endpoint {} with query: {}", config.getSolrServerUri(), query);
    query.setQuery(queryString);
    query.setFields("id", "score", HintBuilder.WON_NODE_SOLR_FIELD);
    query.setRows(config.getMaxHints());
    QueryResponse response = solrClient.query(query);
    return response.getResults();
  }

  protected BulkHintEvent produceHints(SolrDocumentList docs, NeedEvent needEvent) {

    BulkHintEvent bulkHintEvent = hintBuilder.generateHintsFromSearchResult(docs, needEvent);
    return bulkHintEvent;
  }

  protected void publishHints(BulkHintEvent bulkHintEvent, NeedEvent needEvent) {

    log.info("Create {} hints for need {}", bulkHintEvent.getHintEvents().size(), needEvent);
    if (bulkHintEvent.getHintEvents().size() != 0) {
      log.debug("Publish bulk hint event: " + bulkHintEvent);
      getSender().tell(bulkHintEvent, getSelf());
    }
  }

  protected void indexNeedEvent(NeedEvent needEvent, Dataset dataset) throws IOException, JsonLdError {

    log.info("Add need event content to solr index: " + needEvent);
    needIndexer.index(needEvent, dataset);
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

package actor;

import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.github.jsonldjava.core.JsonLdError;
import com.hp.hpl.jena.query.Dataset;
import common.event.BulkHintEvent;
import common.event.NeedEvent;
import config.SirenMatcherConfig;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocumentList;
import org.javasimon.SimonManager;
import org.javasimon.Split;
import org.javasimon.Stopwatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import siren.indexer.NeedIndexer;
import siren.matcher.*;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by Soheilk on 24.08.2015.
 *
 * Siren/Solr based matcher implementation for querying as well as indexing needs.
 */
@Component
@Scope("prototype")
public class SirenMatcherActor extends UntypedActor
{
  private LoggingAdapter log = Logging.getLogger(getContext().system(), this);

  @Autowired
  private SolrServer solrServer;

  @Autowired
  private SirenMatcherConfig config;

  @Autowired
  private HintsBuilder hintBuilder;

  @Autowired
  private SIREnQueryExecutor sIREnQueryExecutor;

  @Autowired
  private SIREnTitleBasedQueryBuilder titleQueryBuilder;

  @Autowired
  private SIREnDescriptionBasedQueryBuilder descriptionQueryBuilder;

  @Autowired
  private SIREnTitleAndDescriptionBasedQueryBuilder titleDescriptionQueryBuilder;

  @Autowired
  private SIREnTitleAndDescriptionAndTagBasedQueryBuilder titleDescriptionTagQueryBuilder;

  @Autowired
  private NeedIndexer needIndexer;

  @Override
  public void onReceive(final Object o) throws Exception {

    if (o instanceof NeedEvent) {
      if (!config.isMonitoringEnabled()){

        NeedEvent needEvent = (NeedEvent) o;
        Dataset dataset = needEvent.deserializeNeedDataset();
        queryNeedEvent(needEvent, dataset);
        indexNeedEvent(needEvent, dataset);
      }
      else {
        NeedEvent needEvent = (NeedEvent) o;

        String matcherOverallName = "Siren-Matcher-Overall";
        Stopwatch matcherOverallStopwatch = SimonManager.getStopwatch(matcherOverallName);
        Split matcherOverallSplit = matcherOverallStopwatch.start();
        String deserlializeStopwatchName = "Deserialization";
        Stopwatch deserializeStopwatch = SimonManager.getStopwatch(deserlializeStopwatchName);
        Split deserializeSplit = deserializeStopwatch.start();
        Dataset dataset = needEvent.deserializeNeedDataset();
        deserializeSplit.stop();
        String overallQueryingStopwatchName = "Querying-Overall";
        Stopwatch overallQueryingStopwatch = SimonManager.getStopwatch(overallQueryingStopwatchName);
        Split overallQueryingSplit = overallQueryingStopwatch.start();
        queryNeedEvent(needEvent, dataset);
        overallQueryingSplit.stop();
        String overallIndexingStopwatchName = "Indexing-Overall";
        Stopwatch overallIndexingStopwatch = SimonManager.getStopwatch(overallIndexingStopwatchName);
        Split overallIndexinggSplit = overallIndexingStopwatch.start();
        indexNeedEvent(needEvent, dataset);
        overallIndexinggSplit.stop();
        matcherOverallSplit.stop();
      }
    } else {
      unhandled(o);
    }
  }

  private void queryNeedEvent(NeedEvent needEvent, Dataset dataset)
    throws QueryNodeException, SolrServerException, IOException, JsonLdError {

    // Reading the need that has to be used for making queries

    WoNNeedReader woNNeedReader = new WoNNeedReader();
    NeedObject needObject = woNNeedReader.readWoNNeedFromDeserializedNeedDataset(dataset, solrServer);

    ArrayList<SolrDocumentList> solrHintDocumentList = new ArrayList<SolrDocumentList>();

    //Here we start to build and execute different SIREn Query Builders
    if (config.isUseTitleQuery()) {
      String solrQuery = titleQueryBuilder.sIRENQueryBuilder(needObject);
      SolrDocumentList docs = sIREnQueryExecutor.execute(solrQuery, solrServer);
      solrHintDocumentList.add(docs);
    }

    if (config.isUseDescriptionQuery()) {
      String solrQuery = descriptionQueryBuilder.sIRENQueryBuilder(needObject);
      SolrDocumentList docs = sIREnQueryExecutor.execute(solrQuery, solrServer);
      solrHintDocumentList.add(docs);
    }

    if (config.isUseTitleDescriptionQuery()) {
      String solrQuery = titleDescriptionQueryBuilder.sIRENQueryBuilder(needObject);
      SolrDocumentList docs = sIREnQueryExecutor.execute(solrQuery, solrServer);
      solrHintDocumentList.add(docs);
    }

    if (config.isUseTitleDescriptionTagQuery()) {
      String solrQuery = titleDescriptionTagQueryBuilder.sIRENQueryBuilder(needObject);
      SolrDocumentList docs = sIREnQueryExecutor.execute(solrQuery, solrServer);
      solrHintDocumentList.add(docs);
    }

    // create the hints
    BulkHintEvent bulkHintEvent = hintBuilder.produceFinalNormalizeHints(
      solrHintDocumentList, needEvent.getUri(), needEvent.getWonNodeUri());

    // publish the hints found
    log.info("Create {} hints for need {}", bulkHintEvent.getHintEvents().size(), needEvent);
    if (bulkHintEvent.getHintEvents().size() != 0) {
      log.debug("Publish bulk hint event: " + bulkHintEvent);
      getSender().tell(bulkHintEvent, getSelf());
    }
  }

  private void indexNeedEvent(NeedEvent needEvent, Dataset dataset) throws IOException, JsonLdError {

    log.info("Add need event content to solr index: " + needEvent);
    needIndexer.indexer_jsonld_format(dataset);
  }

}

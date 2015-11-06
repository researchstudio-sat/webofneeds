package won.matcher.siren.actor;

import com.github.jsonldjava.core.JsonLdError;
import com.hp.hpl.jena.query.Dataset;
import won.matcher.service.common.event.BulkHintEvent;
import won.matcher.service.common.event.NeedEvent;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocumentList;
import org.javasimon.SimonManager;
import org.javasimon.Split;
import org.javasimon.Stopwatch;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import won.matcher.siren.matcher.NeedObject;

import java.io.IOException;

/**
 * Created by hfriedrich on 23.10.2015.
 *
 * Extends SirenMatcherActor to provide a monitored version of it
 */
@Component
@Scope("prototype")
public class SirenMonitoringMatcherActor extends SirenMatcherActor
{

  @Override
  protected void processNeedEvent(NeedEvent needEvent)
    throws IOException, QueryNodeException, SolrServerException, JsonLdError {

    Stopwatch stopwatch = SimonManager.getStopwatch("processNeedEvent");
    Split split = stopwatch.start();

    super.processNeedEvent(needEvent);

    split.stop();
  }

  protected Dataset deserializeNeed(NeedEvent needEvent) throws IOException {

    Stopwatch stopwatch = SimonManager.getStopwatch("deserializeNeed");
    Split split = stopwatch.start();

    Dataset dataset = super.deserializeNeed(needEvent);

    split.stop();
    return dataset;
  }

  protected NeedObject buildNeedObject(Dataset dataset) {

    Stopwatch stopwatch = SimonManager.getStopwatch("buildNeedObject");
    Split split = stopwatch.start();

    NeedObject needObject = super.buildNeedObject(dataset);

    split.stop();
    return needObject;
  }

  protected void extractTerms(NeedObject needObject) {

    Stopwatch stopwatch = SimonManager.getStopwatch("extractTerms");
    Split split = stopwatch.start();

    super.extractTerms(needObject);

    split.stop();
  }

  protected String buildSirenQuery(NeedObject needObject, String[] titleTerms,
                                   String[] descriptionTerms, String[] tagTerms) throws QueryNodeException, IOException {

    Stopwatch stopwatch = SimonManager.getStopwatch("buildSirenQuery");
    Split split = stopwatch.start();

    String query = super.buildSirenQuery(needObject, titleTerms, descriptionTerms, tagTerms);

    split.stop();
    return query;
  }

  protected SolrDocumentList executeSirenQuery(String query) throws SolrServerException {

    Stopwatch stopwatch = SimonManager.getStopwatch("executeSirenQuery");
    Split split = stopwatch.start();

    SolrDocumentList docs = super.executeSirenQuery(query);

    split.stop();
    return docs;
  }

  protected BulkHintEvent produceHints(SolrDocumentList docs, NeedEvent needEvent) {

    Stopwatch stopwatch = SimonManager.getStopwatch("produceHints");
    Split split = stopwatch.start();

    BulkHintEvent hints = super.produceHints(docs, needEvent);

    split.stop();
    return hints;
  }

  protected void publishHints(BulkHintEvent bulkHintEvent, NeedEvent needEvent) {

    Stopwatch stopwatch = SimonManager.getStopwatch("publishHints");
    Split split = stopwatch.start();

    super.publishHints(bulkHintEvent, needEvent);

    split.stop();
  }

  protected void indexNeedEvent(NeedEvent needEvent, Dataset dataset) throws IOException, JsonLdError {

    Stopwatch stopwatch = SimonManager.getStopwatch("indexNeedEvent");
    Split split = stopwatch.start();

    super.indexNeedEvent(needEvent, dataset);

    split.stop();
  }
}

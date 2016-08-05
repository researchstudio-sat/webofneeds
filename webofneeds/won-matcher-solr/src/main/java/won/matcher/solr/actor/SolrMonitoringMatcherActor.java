package won.matcher.solr.actor;

import com.github.jsonldjava.core.JsonLdError;
import com.hp.hpl.jena.query.Dataset;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocumentList;
import org.javasimon.SimonManager;
import org.javasimon.Split;
import org.javasimon.Stopwatch;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import won.matcher.service.common.event.BulkHintEvent;
import won.matcher.service.common.event.NeedEvent;

import java.io.IOException;

/**
 * Created by hfriedrich on 23.10.2015.
 *
 * Extends SirenMatcherActor to provide a monitored version of it
 */
@Component
@Scope("prototype")
public class SolrMonitoringMatcherActor extends AbstractSolrMatcherActor
{

  @Override
  protected void processNeedEvent(NeedEvent needEvent)
    throws IOException, SolrServerException, JsonLdError {

    Stopwatch stopwatch = SimonManager.getStopwatch("processNeedEvent");
    Split split = stopwatch.start();

    super.processNeedEvent(needEvent);

    split.stop();
  }

  @Override
  protected Dataset deserializeNeed(NeedEvent needEvent) throws IOException {

    Stopwatch stopwatch = SimonManager.getStopwatch("deserializeNeed");
    Split split = stopwatch.start();

    Dataset dataset = super.deserializeNeed(needEvent);

    split.stop();
    return dataset;
  }

  @Override
  protected SolrDocumentList executeQuery(String query) throws SolrServerException, IOException {

    Stopwatch stopwatch = SimonManager.getStopwatch("executeQuery");
    Split split = stopwatch.start();

    SolrDocumentList docs = super.executeQuery(query);

    split.stop();
    return docs;
  }

  @Override
  protected BulkHintEvent produceHints(SolrDocumentList docs, NeedEvent needEvent) {

    Stopwatch stopwatch = SimonManager.getStopwatch("produceHints");
    Split split = stopwatch.start();

    BulkHintEvent hints = super.produceHints(docs, needEvent);

    split.stop();
    return hints;
  }

  @Override
  protected void publishHints(BulkHintEvent bulkHintEvent, NeedEvent needEvent) {

    Stopwatch stopwatch = SimonManager.getStopwatch("publishHints");
    Split split = stopwatch.start();

    super.publishHints(bulkHintEvent, needEvent);

    split.stop();
  }

  @Override
  protected void indexNeedEvent(NeedEvent needEvent, Dataset dataset) throws IOException, JsonLdError {

    Stopwatch stopwatch = SimonManager.getStopwatch("indexNeedEvent");
    Split split = stopwatch.start();

    super.indexNeedEvent(needEvent, dataset);

    split.stop();
  }
}

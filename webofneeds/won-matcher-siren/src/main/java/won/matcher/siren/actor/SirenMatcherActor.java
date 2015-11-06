package won.matcher.siren.actor;

import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.github.jsonldjava.core.JsonLdError;
import com.hp.hpl.jena.query.Dataset;
import won.matcher.service.common.event.BulkHintEvent;
import won.matcher.service.common.event.NeedEvent;
import won.matcher.siren.config.SirenMatcherConfig;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocumentList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import won.matcher.siren.indexer.NeedIndexer;
import won.matcher.siren.matcher.*;

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
  private NeedIndexer needIndexer;

  protected String[] titleTerms;
  protected String[] descriptionTerms;
  protected String[] tagTerms;

  @Autowired
  QueryNLPProcessor qNLPP;

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
    throws IOException, QueryNodeException, SolrServerException, JsonLdError {

    Dataset dataset = deserializeNeed(needEvent);
    NeedObject needObject = buildNeedObject(dataset);
    extractTerms(needObject);
    String query = buildSirenQuery(needObject, titleTerms, descriptionTerms, tagTerms);
    SolrDocumentList docs = executeSirenQuery(query);

    if (docs != null) {
      BulkHintEvent hints = produceHints(docs, needEvent);
      publishHints(hints, needEvent);
    }

    indexNeedEvent(needEvent, dataset);
  }

  protected Dataset deserializeNeed(NeedEvent needEvent) throws IOException {
    Dataset dataset = needEvent.deserializeNeedDataset();
    return dataset;
  }

  protected NeedObject buildNeedObject(Dataset dataset) {

    WoNNeedReader woNNeedReader = new WoNNeedReader();
    NeedObject needObject = woNNeedReader.readWoNNeedFromDeserializedNeedDataset(dataset, solrServer);
    return needObject;
  }

  protected void extractTerms(NeedObject needObject) {

    titleTerms = new String[0];
    descriptionTerms = new String[0];
    tagTerms = new String[0];
    titleTerms = qNLPP.extractWordTokens(needObject.getNeedTitle());
    descriptionTerms = qNLPP.extractWordTokens(needObject.getNeedDescription());
    tagTerms = qNLPP.extractWordTokens(needObject.getNeedTag());
  }

  protected String buildSirenQuery(NeedObject needObject, String[] titleTerms,
                                   String[] descriptionTerms, String[] tagTerms)
    throws IOException, QueryNodeException {

    SirenQueryBuilder queryBuilder = new SirenQueryBuilder(needObject, config.getConsideredQueryTokens());
    String solrQuery = null;

    if (config.isUseTitleQuery()) {
      queryBuilder.addTitleTerms(titleTerms);
      solrQuery = queryBuilder.build();
    } else if (config.isUseDescriptionQuery()) {
      queryBuilder.addDescriptionTerms(descriptionTerms);
      solrQuery = queryBuilder.build();
    } else if (config.isUseTitleDescriptionQuery()) {
      queryBuilder.addTitleTerms(titleTerms);
      queryBuilder.addDescriptionTerms(descriptionTerms);
      solrQuery = queryBuilder.build();
    } else if (config.isUseTitleDescriptionTagQuery()) {
      queryBuilder.addTitleTerms(titleTerms);
      queryBuilder.addTagTerms(tagTerms);
      queryBuilder.addDescriptionTerms(descriptionTerms);
      solrQuery = queryBuilder.build();
    }

    return solrQuery;
  }

  protected SolrDocumentList executeSirenQuery(String query) throws SolrServerException {
    return sIREnQueryExecutor.execute(query, solrServer);
  }

  protected BulkHintEvent produceHints(SolrDocumentList docs, NeedEvent needEvent) {

    ArrayList<SolrDocumentList> solrHintDocumentList = new ArrayList<SolrDocumentList>();
    solrHintDocumentList.add(docs);
    BulkHintEvent bulkHintEvent = hintBuilder.produceFinalNormalizeHints(
      solrHintDocumentList, needEvent.getUri(), needEvent.getWonNodeUri());
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
    needIndexer.indexer_jsonld_format(dataset);
  }

}

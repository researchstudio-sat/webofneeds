package actor;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.cluster.pubsub.DistributedPubSub;
import akka.cluster.pubsub.DistributedPubSubMediator;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.github.jsonldjava.core.JsonLdError;
import com.hp.hpl.jena.query.Dataset;
import common.event.BulkHintEvent;
import common.event.HintEvent;
import common.event.NeedEvent;
import config.SirenMatcherConfig;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.common.SolrDocumentList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import siren.indexer.NeedIndexer;
import siren.matcher.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Soheilk on 24.08.2015.
 */
@Component
@Scope("prototype")
public class SirenMatcherActor extends UntypedActor
{
  private LoggingAdapter log = Logging.getLogger(getContext().system(), this);

   /*
      HttpSolrServer is thread-safe and if you are using the following constructor,
      you *MUST* re-use the same instance for all requests.  If instances are created on
      the fly, it can cause a connection leak. The recommended practice is to keep a
      static instance of HttpSolrServer per solr server url and share it for all requests.
      See https://issues.apache.org/jira/browse/SOLR-861 for more details
  */
  private static SolrServer solrServer = null;

  private ActorRef pubSubMediator;
  private List<NeedEvent> needs = new LinkedList<>();

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
  private NeedIndexer needIndexer;


  @Override
  public void preStart() {

    // subscribe to need events
    pubSubMediator = DistributedPubSub.get(getContext().system()).mediator();
    pubSubMediator.tell(new DistributedPubSubMediator.Subscribe(NeedEvent.class.getName(), getSelf()), getSelf());

    // set solr server
    if (solrServer == null) {
      solrServer = new HttpSolrServer(config.getSolrServerUri());
    } else {
      log.warning("solr server uri already set by another instance!");
    }
  }

  @Override
  public void onReceive(final Object o) throws Exception {

        if (o instanceof NeedEvent) {
            NeedEvent needEvent = (NeedEvent) o;
            log.info("NeedEvent received: " + needEvent.getUri());
            processNeedEvent(needEvent);
        } else {
            unhandled(o);
        }
    }

  private void processNeedEvent(NeedEvent needEvent)
    throws QueryNodeException, SolrServerException, IOException, JsonLdError {

    //Reading the need that has to be used for making queries
    Dataset dataset = needEvent.deserializeNeedDataset();
    WoNNeedReader woNNeedReader = new WoNNeedReader();
    NeedObject needObject = woNNeedReader.readWoNNeedFromDeserializedNeedDataset(dataset, solrServer);

    ArrayList<SolrDocumentList> solrHintDocumentList = new ArrayList<SolrDocumentList>();

    //Here we start to build and execute different SIREn Query Builders
    if (config.isUseTitleQuery()) {
      String stringTitleBasedQuery = titleQueryBuilder.sIRENQueryBuilder(needObject);
      SolrDocumentList docs = sIREnQueryExecutor.execute(stringTitleBasedQuery, solrServer);
      solrHintDocumentList.add(docs);
    }

    if (config.isUseDescriptionQuery()) {
      String stringDescriptionBasedQuery = descriptionQueryBuilder.sIRENQueryBuilder(needObject);
      SolrDocumentList docs = sIREnQueryExecutor.execute(stringDescriptionBasedQuery, solrServer);
      solrHintDocumentList.add(docs);
    }

    if (config.isUseTitleDescriptionQuery()) {
      String stringDescriptionBasedQuery = titleDescriptionQueryBuilder.sIRENQueryBuilder(needObject);
      SolrDocumentList docs = sIREnQueryExecutor.execute(stringDescriptionBasedQuery, solrServer);
      solrHintDocumentList.add(docs);
    }

    // create the hints
    ArrayList<HintEvent> hintArrayList = hintBuilder.produceFinalNormalizeHints(
      solrHintDocumentList, needEvent.getUri(), needEvent.getWonNodeUri());

    BulkHintEvent bulkHintEvent = new BulkHintEvent();
    for (int i = 0; i < hintArrayList.size(); i++) {
      bulkHintEvent.addHintEvent(hintArrayList.get(i));
    }

    // publish the hints found
    if (hintArrayList.size() != 0) {
      pubSubMediator.tell(new DistributedPubSubMediator.Publish(
                            bulkHintEvent.getClass().getName(), bulkHintEvent), getSelf());
    }

    // save the new need in the siren index
    needIndexer.indexer_jsonld_format(dataset);
  }
}

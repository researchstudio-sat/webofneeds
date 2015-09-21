package actor;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.cluster.pubsub.DistributedPubSub;
import akka.cluster.pubsub.DistributedPubSubMediator;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.github.jsonldjava.core.JsonLdError;
import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.JsonUtils;
import com.hp.hpl.jena.query.Dataset;
import common.event.BulkHintEvent;
import com.hp.hpl.jena.query.Dataset;
import common.event.HintEvent;
import common.event.NeedEvent;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.common.SolrDocumentList;
import siren_matcher.*;
import config.SirenMatcherConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by Soheilk on 24.08.2015.
 */
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

    public static SolrServer SOLR_SERVER = new HttpSolrServer(Configuration.sIREnUri);


    @Override
    public void onReceive(final Object o) throws Exception {
  @Autowired
  private SirenMatcherConfig config;


  @Override
  public void preStart() {

    // subscribe to need events
    pubSubMediator = DistributedPubSub.get(getContext().system()).mediator();
    pubSubMediator.tell(new DistributedPubSubMediator.Subscribe(NeedEvent.class.getName(), getSelf()), getSelf());
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

    private void processNeedEvent(NeedEvent needEvent) throws QueryNodeException, SolrServerException, IOException {
        /*
            ThreadLocal<QueryNLPProcessor> processorThreadLocal = new ThreadLocal<QueryNLPProcessor>(){
              @Override
              protected QueryNLPProcessor initialValue() {
                try {
                  return new QueryNLPProcessor();
                } catch (IOException e) {
                  throw new RuntimeException("could not instantiate QueryNLPProcessor", e);
                }
              }
            };
        */

        //making a new instance of the SIREn Query Excutor
        SIREnQueryExecutor sIREnQueryExecutor = new SIREnQueryExecutor();

        String targetNeedUri = needEvent.getUri();

        //Reading the need that has to be used for making queries
        Dataset dataset = needEvent.deserializeNeedDataset();


        WoNNeedReader woNNeedReader = new WoNNeedReader();
        NeedObject needObject = woNNeedReader.readWoNNeedFromDeserializedNeedDataset(dataset, SOLR_SERVER);

        ArrayList<SolrDocumentList> solrHintDocumentList = new ArrayList<SolrDocumentList>();

        //Here we start to build and execute different SIREn Query Builders
        if (Configuration.ACTIVATE_TITEL_BASED_QUERY_BUILDER == true) {
            SIREnTitleBasedQueryBuilder sTitleBQueryBuilder = new SIREnTitleBasedQueryBuilder();
            String stringTitleBasedQuery = sTitleBQueryBuilder.sIRENQueryBuilder(needObject);
            //System.out.print("TITEL QUERY LENGHT: " + stringTitleBasedQuery.length()); // JUST FOR TEST
            SolrDocumentList docs = sIREnQueryExecutor.execute(stringTitleBasedQuery, SOLR_SERVER);
            solrHintDocumentList.add(docs);
            //System.out.println(docs); // JUST FOR TEST
        }

        if (Configuration.ACTIVATE_DESCRIPTION_BASED_QUERY_BUILDER == true) {
            SIREnDescriptionBasedQueryBuilder sDescriptionQueryBuilder = new SIREnDescriptionBasedQueryBuilder();
            String stringDescriptionBasedQuery = sDescriptionQueryBuilder.sIRENQueryBuilder(needObject);
            //System.out.print("DESCRIPTION QUERY LENGHT: " + stringDescriptionBasedQuery.length()); // JUST FOR TEST
            SolrDocumentList docs = sIREnQueryExecutor.execute(stringDescriptionBasedQuery, SOLR_SERVER);
            solrHintDocumentList.add(docs);
            //System.out.println(docs); // JUST FOR TEST
        }

        if (Configuration.ACTIVATE_TITEL_AND_DESCRIPTION_BASED_QUERY_BUILDER == true) {
            SIREnTitleAndDescriptionBasedQueryBuilder sDescriptionQueryBuilder = new SIREnTitleAndDescriptionBasedQueryBuilder();
            String stringDescriptionBasedQuery = sDescriptionQueryBuilder.sIRENQueryBuilder(needObject);
            //System.out.print("Title and DESCRIPTION QUERY LENGHT: " + stringDescriptionBasedQuery.length()); // JUST FOR TEST
            SolrDocumentList docs = sIREnQueryExecutor.execute(stringDescriptionBasedQuery, SOLR_SERVER);
            solrHintDocumentList.add(docs);
            //System.out.println(docs); // JUST FOR TEST
        }

        HintsBuilder hintBuilder = new HintsBuilder();
        ArrayList<HintEvent> hintArrayList = hintBuilder.produceFinalNormalizeHints(solrHintDocumentList, targetNeedUri);

        //If you need you can also use this =>   HintEvent hintEvent1 = new HintEvent("uri1", "uri2", 0.0);
        //If you need you can also use this => HintEvent hintEvent2 = new HintEvent("uri3", "uri4", 0.0);
        //If you need you can also use this =>  getSender().tell(hintEvent1, getSelf());

        BulkHintEvent bulkHintEvent = new BulkHintEvent();

        for (int i = 0; i < hintArrayList.size(); i++) {
            bulkHintEvent.addHintEvent(hintArrayList.get(i));
        }

        //If you need you can also use this => bulkHintEvent.addHintEvent(hintEvent1);
        //If you need you can also use this => bulkHintEvent.addHintEvent(hintEvent2);
        if (hintArrayList.size() != 0) {
            getSender().tell(bulkHintEvent, getSelf());
        }
    }
  }


}

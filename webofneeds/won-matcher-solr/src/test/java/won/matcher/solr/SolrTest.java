package won.matcher.solr;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import won.matcher.service.common.event.NeedEvent;
import won.matcher.service.common.spring.SpringExtension;
import won.matcher.solr.actor.SolrMatcherActor;
import won.matcher.solr.spring.MatcherSolrAppConfiguration;
import won.protocol.util.WonRdfUtils;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by hfriedrich on 11.09.2015.
 */
public class SolrTest {
    public static void main(String[] args) throws IOException, InterruptedException {

        // init basic Akka
        AnnotationConfigApplicationContext ctx =
                new AnnotationConfigApplicationContext(MatcherSolrAppConfiguration.class);
        ActorSystem system = ctx.getBean(ActorSystem.class);
        ActorRef solrMatcherActor = system.actorOf(
                SpringExtension.SpringExtProvider.get(system).props(SolrMatcherActor.class), "SolrMatcherActor");

        InputStream is = SolrTest.class.getResourceAsStream("/needmodel/need1.trig");
        Dataset dataset = DatasetFactory.create();
        RDFDataMgr.read(dataset, is, RDFFormat.TRIG.getLang());
        is.close();
        String needUri = WonRdfUtils.NeedUtils.getNeedURI(dataset).toString();

        NeedEvent needEvent = new NeedEvent(needUri, "no_uri", NeedEvent.TYPE.CREATED, System.currentTimeMillis(), dataset);

        // send event to matcher implementation
        solrMatcherActor.tell(needEvent, null);
    }
}

package won.matcher.solr;

import java.io.IOException;
import java.io.InputStream;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import won.matcher.service.common.event.NeedEvent;
import won.matcher.service.common.spring.SpringExtension;
import won.matcher.solr.actor.SolrMatcherActor;
import won.matcher.solr.spring.MatcherSolrAppConfiguration;
import won.protocol.util.WonRdfUtils;

/**
 * Created by hfriedrich on 11.09.2015.
 */
public class SolrTest {
    public static void main(String[] args) throws IOException, InterruptedException {
        // init basic Akka
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(
                        MatcherSolrAppConfiguration.class);
        ActorSystem system = ctx.getBean(ActorSystem.class);
        ActorRef solrMatcherActor = system.actorOf(
                        SpringExtension.SpringExtProvider.get(system).props(SolrMatcherActor.class),
                        "SolrMatcherActor");
        NeedEvent ne1 = createNeedEvent("/needmodel/need1.trig");
        NeedEvent ne2 = createNeedEvent("/needmodel/need2.trig");
        solrMatcherActor.tell(ne1, null);
        Thread.sleep(5000);
        solrMatcherActor.tell(ne2, null);
    }

    private static NeedEvent createNeedEvent(String path) throws IOException {
        InputStream is = null;
        Dataset dataset = null;
        try {
            try {
                is = SolrTest.class.getResourceAsStream(path);
                dataset = DatasetFactory.create();
                RDFDataMgr.read(dataset, is, RDFFormat.TRIG.getLang());
            } finally {
                if (is != null) {
                    is.close();
                }
            }
        } catch (IOException e) {
            System.err.println(e);
            return null;
        }
        String needUri = WonRdfUtils.NeedUtils.getNeedURI(dataset).toString();
        return new NeedEvent(needUri, "no_uri", NeedEvent.TYPE.ACTIVE, System.currentTimeMillis(), dataset);
    }
}

package won.matcher.sparql;

import java.io.IOException;
import java.io.InputStream;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import won.matcher.service.common.event.AtomEvent;
import won.matcher.service.common.event.Cause;
import won.matcher.service.common.spring.SpringExtension;
import won.matcher.sparql.actor.SparqlMatcherActor;
import won.matcher.sparql.spring.MatcherSparqlAppConfiguration;
import won.protocol.util.WonRdfUtils;

/**
 * Created by hfriedrich on 11.09.2015.
 */
public class SparqlMatcherActorExperiment {
    public static void main(String[] args) throws IOException, InterruptedException {
        // init basic Akka
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(
                        MatcherSparqlAppConfiguration.class);
        ActorSystem system = ctx.getBean(ActorSystem.class);
        ActorRef solrMatcherActor = system.actorOf(
                        SpringExtension.SpringExtProvider.get(system).props(SparqlMatcherActor.class),
                        "SolrMatcherActor");
        AtomEvent ne1 = createAtomEvent("/atommodel/atom1.trig");
        AtomEvent ne2 = createAtomEvent("/atommodel/atom2.trig");
        solrMatcherActor.tell(ne1, null);
        Thread.sleep(5000);
        solrMatcherActor.tell(ne2, null);
    }

    private static AtomEvent createAtomEvent(String path) throws IOException {
        InputStream is = null;
        Dataset dataset = null;
        try {
            try {
                is = SparqlMatcherActorExperiment.class.getResourceAsStream(path);
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
        String atomUri = WonRdfUtils.AtomUtils.getAtomURI(dataset).toString();
        return new AtomEvent(atomUri, "no_uri", AtomEvent.TYPE.ACTIVE, System.currentTimeMillis(), dataset,
                        Cause.PUSHED);
    }
}

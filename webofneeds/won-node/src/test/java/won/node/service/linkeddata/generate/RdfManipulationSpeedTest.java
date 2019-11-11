package won.node.service.linkeddata.generate;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDFS;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.util.StopWatch;

import won.protocol.util.CheapInsecureRandomString;

@Ignore
public class RdfManipulationSpeedTest {
    CheapInsecureRandomString rnd = new CheapInsecureRandomString();

    /**
     * Shows: adding to model is relatively stable, performance degrades somewhat.
     */
    @Test
    public void testAddToModel() {
        StopWatch sw = new StopWatch();
        for (int i = 0; i < 1000; i += 10) {
            Model m = ModelFactory.createDefaultModel();
            Resource main = m.getResource("uri:/main");
            int count = i * 1000;
            List<URI> uris = generateURIs(count);
            sw.start();
            addToModel(m, main, uris);
            sw.stop();
            long millis = sw.getLastTaskTimeMillis();
            System.out.println("Adding " + count + " uris takes " + millis + " millis, that's "
                            + (double) millis / ((double) count / 1000) + " per 1000 uris");
        }
    }

    @Test
    public void testUriCreate() {
        StopWatch sw = new StopWatch();
        sw.start();
        int count = 1000000;
        for (int i = 0; i < count; i++) {
            generateURI();
        }
        sw.stop();
        long millis = sw.getLastTaskTimeMillis();
        System.out.println("Creating " + count + " uris takes " + millis + " millis, that's "
                        + (double) millis / ((double) count / 1000) + " per 1000 uris");
        sw.start();
        for (int i = 0; i < count; i++) {
            generateURIString();
        }
        sw.stop();
        millis = sw.getLastTaskTimeMillis();
        System.out.println("Creating " + count + " uri strings takes " + millis + " millis, that's "
                        + (double) millis / ((double) count / 1000) + " per 1000 uris");
    }

    private Model addToModel(Model m, Resource main, List<URI> uris) {
        uris.forEach(uri -> m.add(main, RDFS.member, m.getResource(uri.toString())));
        return m;
    }

    private List<URI> generateURIs(int num) {
        return Stream.generate(() -> generateURI()).limit(num).collect(Collectors.toList());
    }

    private URI generateURI() {
        return URI.create("uri:/new-uri/" + rnd.nextString(30));
    }

    private String generateURIString() {
        return "uri:/new-uri/" + rnd.nextString(30);
    }
}

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.compose.Union;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.shacl.Shapes;
import org.apache.jena.shacl.ValidationReport;
import org.apache.jena.shacl.lib.ShLib;
import org.apache.jena.shacl.sys.ShaclSystem;
import org.apache.jena.sparql.graph.GraphFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.util.Assert;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.util.stream.Stream;

public class ShaclCheckTest {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    @Autowired
    private static ResourceLoader loader;
    private static Shapes authShapes;

    @BeforeAll
    public static void loadAuthShapes() {
        authShapes = loadShapes(new ClassPathResource("ontology/won-auth.ttl"));
    }

    @ParameterizedTest
    @MethodSource
    public void testExtWithAuthShapes(Resource ontology) {
        try {
            Graph toCheck = loadGraph(ontology);
            validateAgainstShapes(authShapes, toCheck, ontology);
        } catch (Exception e) {
            throw new RuntimeException(String.format("error executing test with input file %s", ontology.getFilename()),
                            e);
        }
    }

    public static Stream<Resource> testExtWithAuthShapes() throws IOException {
        Resource[] files = ResourcePatternUtils.getResourcePatternResolver(loader)
                        .getResources("classpath:/ontology/ext/won-ext-*.ttl");
        return Stream.of(files);
    }

    @ParameterizedTest
    @MethodSource
    public void testCoreWithAuthShapes(Resource ontology) {
        try {
            Graph toCheck = loadGraph(ontology);
            validateAgainstShapes(authShapes, toCheck, ontology);
        } catch (Exception e) {
            throw new RuntimeException(String.format("error executing test with input file %s", ontology.getFilename()),
                            e);
        }
    }

    public static Stream<Resource> testCoreWithAuthShapes() throws IOException {
        Resource[] files = ResourcePatternUtils.getResourcePatternResolver(loader)
                        .getResources("classpath:/ontology/*.ttl");
        return Stream.of(files);
    }

    public void validateAgainstShapes(Shapes shapes, Graph graph, Resource resourceOfGraph) {
        ValidationReport shaclReport = ShaclSystem.get().validate(shapes, new Union(shapes.getGraph(), graph));
        if (!shaclReport.conforms()) {
            ShLib.printReport(shaclReport);
            System.out.println();
            RDFDataMgr.write(System.out, shaclReport.getModel(), Lang.TTL);
        }
        Assert.isTrue(shaclReport.conforms(),
                        String.format("%s: graph does not conform to shapes", resourceOfGraph.getFilename()));
    }

    public static Shapes loadShapes(Resource shapesResource) {
        logger.debug("parsing shapes in {}", shapesResource.getFilename());
        long start = System.currentTimeMillis();
        Graph shapesGraph = loadGraph(shapesResource);
        logger.debug("loaded {} triples", shapesGraph.size());
        return Shapes.parse(shapesGraph);
    }

    private static Graph loadGraph(String uri) {
        Graph graph = loadGraph(MethodHandles.lookup().lookupClass().getClassLoader().getResourceAsStream(uri));
        return graph;
    }

    private static Graph loadGraph(InputStream stream) {
        Graph graph = GraphFactory.createGraphMem();
        RDFDataMgr.read(graph, stream, Lang.TTL);
        return graph;
    }

    private static Graph loadGraph(Resource resource) {
        Graph graph = null;
        try {
            graph = loadGraph(resource.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return graph;
    }
}

package won.auth;

import org.apache.jena.datatypes.xsd.XSDDateTime;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.compose.Union;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.shacl.Shapes;
import org.apache.jena.shacl.ValidationReport;
import org.apache.jena.shacl.lib.ShLib;
import org.apache.jena.shacl.sys.ShaclSystem;
import org.apache.jena.sparql.graph.GraphFactory;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import won.auth.model.*;
import won.shacl2java.Shacl2JavaInstanceFactory;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class)
public class InstantiationTests {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Configuration
    static class InstantiationTestsConfig {
    }

    @Autowired
    private ResourceLoader loader;
    @Value("classpath:/shacl/won-auth-shapes.ttl")
    private Resource shapesDef;
    @Value("classpath:/shacl/won-test-atom-shapes.ttl")
    private Resource atomDataShapesDef;

    public static Shapes loadShapes(Resource shapesResource) {
        logger.debug("parsing shapes in {}", shapesResource.getFilename());
        long start = System.currentTimeMillis();
        Graph shapesGraph = loadGraph(shapesResource);
        logDuration("loading shapes", start);
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

    @Test
    public void test_basic_005() throws IOException {
        Shapes shapes = loadShapes(shapesDef);
        Shacl2JavaInstanceFactory factory = new Shacl2JavaInstanceFactory(shapes, "won.auth.model");
        Graph data = loadGraph(loader.getResource("classpath:/won/basic/basic-005.ttl"));
        factory.load(new Union(shapes.getGraph(), data));
        logger.debug("done instantiating");
        Assert.assertEquals(3, factory.size());
    }

    @Test
    public void test_basic_006() throws IOException {
        Shapes shapes = loadShapes(shapesDef);
        Shacl2JavaInstanceFactory factory = new Shacl2JavaInstanceFactory(shapes, "won.auth.model");
        Graph data = loadGraph(loader.getResource("classpath:/won/basic/basic-006.ttl"));
        factory.load(new Union(shapes.getGraph(), data));
        logger.debug("done instantiating");
        Assert.assertEquals(3, factory.size());
    }

    private static void logDuration(String message, long start) {
        logger.debug(message + " took {} ms", System.currentTimeMillis() - start);
    }
}

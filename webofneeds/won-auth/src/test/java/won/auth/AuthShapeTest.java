package won.auth;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.compose.Union;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.shacl.ShaclValidator;
import org.apache.jena.shacl.Shapes;
import org.apache.jena.shacl.ValidationReport;
import org.apache.jena.shacl.engine.ValidationContext;
import org.apache.jena.shacl.lib.ShLib;
import org.apache.jena.shacl.parser.Shape;
import org.apache.jena.shacl.validation.VLib;
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
import won.shacl2java.validation.ResettableErrorHandler;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.stream.Stream;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class)
public class AuthShapeTest {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    @Autowired
    private ResourceLoader loader;
    @Value("classpath:/shacl/won-auth-shapes.ttl")
    private Resource shapesDef;
    private String AUTH = "https://w3id.org/won/auth#";

    @Configuration
    static class AuthShapeTestContextConfiguration {
    }

    private static Shapes loadShapes(Resource shapesResource) throws IOException {
        Graph graph = loadData(shapesResource);
        logger.debug("parsing shapes in {}", shapesResource.getFilename());
        return Shapes.parse(graph);
    }

    private static Graph loadData(Resource dataResource) throws IOException {
        logger.debug("loading data from {}", dataResource.getFilename());
        Model shapesGraph = ModelFactory.createDefaultModel();
        RDFDataMgr.read(shapesGraph, dataResource.getInputStream(), Lang.TTL);
        return shapesGraph.getGraph();
    }

    @Test
    public void testBasicShape_001() throws IOException {
        Shapes shapes = loadShapes(shapesDef);
        Graph data = loadData(loader.getResource("classpath:/won/basic/basic-001.ttl"));
        assertConformsTo(NodeFactory.createURI(AUTH + "opRead"),
                        NodeFactory.createURI(AUTH + "simpleOperationExpressionShape"), shapes, data, true);
    }

    @Test
    public void testBasicShape_002() throws IOException {
        Shapes shapes = loadShapes(shapesDef);
        Graph data = loadData(loader.getResource("classpath:/won/basic/basic-002.ttl"));
        assertConformsTo(NodeFactory.createURI(AUTH + "opHint"),
                        NodeFactory.createURI(AUTH + "messageOperationExpressionShape"), shapes, data, true);
        assertConformsTo(NodeFactory.createURI(AUTH + "opHint"),
                        NodeFactory.createURI(AUTH + "simpleOperationExpressionShape"), shapes, data, false);
        assertConformsTo(NodeFactory.createURI(AUTH + "msgTypesHint"),
                        NodeFactory.createURI(AUTH + "messageTypesExpressionShape"), shapes, data, true);
    }

    @Test
    public void testBasicShape_003() throws IOException {
        Shapes shapes = loadShapes(shapesDef);
        Graph data = loadData(loader.getResource("classpath:/won/basic/basic-003.ttl"));
        assertConformsTo(NodeFactory.createURI(AUTH + "opModifyOnBehalf"),
                        NodeFactory.createURI(AUTH + "messageOperationExpressionShape"), shapes, data, true);
        assertConformsTo(NodeFactory.createURI(AUTH + "opModifyOnBehalf"),
                        NodeFactory.createURI(AUTH + "simpleOperationExpressionShape"), shapes, data, false);
        assertConformsTo(NodeFactory.createURI(AUTH + "msgTypesModify"),
                        NodeFactory.createURI(AUTH + "messageTypesExpressionShape"), shapes, data, true);
    }

    @Test
    public void testBasicShape_004() throws IOException {
        Shapes shapes = loadShapes(shapesDef);
        Graph data = loadData(loader.getResource("classpath:/won/basic/basic-004.ttl"));
        assertConformsTo(NodeFactory.createURI(AUTH + "customMessageOperation"),
                        NodeFactory.createURI(AUTH + "messageOperationExpressionShape"), shapes, data, true);
        assertConformsTo(NodeFactory.createURI(AUTH + "customMessageOperation"),
                        NodeFactory.createURI(AUTH + "simpleOperationExpressionShape"), shapes, data, false);
    }

    @Test
    public void testBasicShape_005() throws IOException {
        Shapes shapes = loadShapes(shapesDef);
        Graph data = loadData(loader.getResource("classpath:/won/basic/basic-005.ttl"));
        assertConformsTo(NodeFactory.createURI("https://example.com/test/request"),
                        NodeFactory.createURI(AUTH + "operationRequestShape"), shapes, data, true);
    }

    @Test
    public void testBasicShape_006() throws IOException {
        Shapes shapes = loadShapes(shapesDef);
        Graph data = loadData(loader.getResource("classpath:/won/basic/basic-006.ttl"));
        assertConformsTo(NodeFactory.createURI("https://example.com/test/auth"),
                        NodeFactory.createURI(AUTH + "authorizationShape"), shapes, data, true);
    }

    private void assertConformsTo(Node focusNode, Node shapeNode, Shapes shapes, Graph data, boolean expected) {
        data = new Union(data, shapes.getGraph());
        ResettableErrorHandler handler = new ResettableErrorHandler();
        Shape shape = shapes.getShape(shapeNode);
        if (shape == null) {
            throw new IllegalArgumentException("no such shape: " + shapeNode);
        }
        boolean isFocusNode = VLib.isFocusNode(shape, focusNode, data);
        if (expected && !isFocusNode) {
            Assert.fail(String.format("%s should be focus node of %s", focusNode, shape));
        }
        if (!expected && isFocusNode) {
            Assert.fail(String.format("%s should not be focus node of %s", focusNode, shape));
        }
        if (isFocusNode) {
            ValidationContext vCtx = ValidationContext.create(shapes, shapes.getGraph(), handler);
            VLib.validateShape(vCtx, data, shapes.getShape(shapeNode), focusNode);
            if (vCtx.hasViolation()) {
                ValidationReport report = vCtx.generateReport();
                printNonconformingReport(report);
                Assert.fail(String.format("Data does not conform to shapes"));
            }
            if (handler.isError() || handler.isFatal()) {
                Assert.fail(String.format("Node %s %s to shape %s", focusNode,
                                expected ? "does not conform" : "unexpectedly conforms", shapeNode));
            }
        }
    }

    @Test
    public void testAuthShape() throws IOException {
        Shapes shapes = loadShapes(shapesDef);
        assertConformityOfAuthorizations(shapes, shapesDef);
        getAuthorizations().forEach(authGraph -> {
            assertConformityOfAuthorizations(shapes, authGraph);
        });
        getOperationRequests().forEach(opReq -> {
            assertConformityOfAuthorizations(shapes, opReq);
        });
    }

    @Test
    public void testCycles() throws IOException {
        Shapes shapes = loadShapes(loader.getResource("classpath:/won/exp/cycletest.ttl"));
    }

    private void assertConformityOfAuthorizations(Shapes shapes, Resource authGraph) {
        logger.debug("checking {}", authGraph.getFilename());
        Model data = ModelFactory.createDefaultModel();
        try {
            RDFDataMgr.read(data, authGraph.getInputStream(), Lang.TTL);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Graph g = data.getGraph();
        long start = CPUUtils.getCpuTime();
        ValidationReport report = ShaclValidator.get().validate(shapes, new Union(g, shapes.getGraph()));
        long duration = CPUUtils.getCpuTime() - start;
        logger.debug("validation took {} millis ", (double) duration / 1000000d);
        if (!report.conforms()) {
            printNonconformingReport(report);
        }
        Assert.assertTrue(report.conforms());
    }

    private void printNonconformingReport(ValidationReport report) {
        ShLib.printReport(report);
        System.out.println();
        RDFDataMgr.write(System.out, report.getModel(), Lang.TTL);
    }

    private void assertConformityOfOperationRequests(Shapes shapes, Resource authGraph) {
        logger.debug("checking {}", authGraph.getFilename());
        Model data = ModelFactory.createDefaultModel();
        try {
            RDFDataMgr.read(data, authGraph.getInputStream(), Lang.TTL);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Graph g = data.getGraph();
        long start = CPUUtils.getCpuTime();
        ValidationReport report = ShaclValidator.get().validate(shapes, g);
        long duration = CPUUtils.getCpuTime() - start;
        logger.debug("validation took {} millis ", (double) duration / 1000000d);
        if (!report.conforms()) {
            printNonconformingReport(report);
        }
        Assert.assertTrue(report.conforms());
    }

    public Stream<Resource> getAuthorizations() throws IOException {
        Resource[] files = ResourcePatternUtils.getResourcePatternResolver(loader)
                        .getResources("classpath:/won/auth/*.ttl");
        return Stream.of(files).sorted((left, right) -> left.getFilename().compareTo(right.getFilename()));
    }

    public Stream<Resource> getOperationRequests() throws IOException {
        Resource[] okFiles = ResourcePatternUtils.getResourcePatternResolver(loader)
                        .getResources("classpath:/won/opreq/ok/*.ttl");
        Resource[] failFiles = ResourcePatternUtils.getResourcePatternResolver(loader)
                        .getResources("classpath:/won/opreq/fail/*.ttl");
        return Stream.concat(Stream.of(okFiles), Stream.of(failFiles))
                        .sorted((left, right) -> left.getFilename().compareTo(right.getFilename()));
    }
}

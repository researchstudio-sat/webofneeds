package won.auth;

import org.apache.jena.graph.Graph;
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
import won.auth.model.AclEvalResult;
import won.auth.model.Authorization;
import won.auth.model.DecisionValue;
import won.auth.model.OperationRequest;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.util.function.Supplier;
import java.util.stream.Stream;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class)
public class WonAclEvaluatorTests {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Configuration
    static class AuthShapeTestContextConfiguration {
    }

    @Autowired
    private static ResourceLoader loader;
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
    public void testWonAcl_fail() throws IOException {
        Shapes shapes = loadShapes(shapesDef);
        Shapes atomDataShapes = loadShapes(atomDataShapesDef);
        ModelBasedTargetAtomCheckEvaluator targetAtomChecker = withDurationLog("initializing TargetAtomChecker",
                        () -> new ModelBasedTargetAtomCheckEvaluator(atomDataShapes,
                                        "won.auth.test.model"));
        WonAclEvaluator evaluator = withDurationLog("initializing WonAclEvaluator",
                        () -> new WonAclEvaluator(shapes, targetAtomChecker));
        getFailOperationRequests().forEach(
                        testCaseResource -> {
                            Graph graph = null;
                            try {
                                evaluateTest(shapes, atomDataShapes, evaluator, targetAtomChecker, testCaseResource,
                                                DecisionValue.ACCESS_DENIED);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        });
    }

    @Test
    public void testWonAcl_ok() throws IOException {
        Shapes shapes = loadShapes(shapesDef);
        Shapes atomDataShapes = loadShapes(atomDataShapesDef);
        ModelBasedTargetAtomCheckEvaluator targetAtomChecker = new ModelBasedTargetAtomCheckEvaluator(atomDataShapes,
                        "won.auth.test.model");
        WonAclEvaluator evaluator = withDurationLog("initializing WonAclEvaluator",
                        () -> new WonAclEvaluator(shapes, targetAtomChecker));
        getOkOperationRequests().forEach(
                        testCaseResource -> {
                            Graph graph = null;
                            try {
                                evaluateTest(shapes, atomDataShapes, evaluator, targetAtomChecker, testCaseResource,
                                                DecisionValue.ACCESS_GRANTED);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        });
    }

    private void evaluateTest(Shapes shapes, Shapes atomDataShapes,
                    WonAclEvaluator evaluator, ModelBasedTargetAtomCheckEvaluator targetAtomChecker, Resource resource,
                    DecisionValue accessDenied)
                    throws IOException {
        logger.debug("test case: {}", resource.getFilename());
        Graph graph = withDurationLog("loading data graph ",
                        () -> loadGraph(resource));
        withDurationLog("validating with auth shapes",
                        () -> validateTestData(shapes, graph, true, shapesDef.getFilename()));
        withDurationLog("validating with atom test data shapes",
                        () -> validateTestData(atomDataShapes, graph, false, atomDataShapesDef.getFilename()));
        withDurationLog("instantiating test atom entities",
                        () -> targetAtomChecker.loadData(graph));
        withDurationLog("instantiating auth/req entities",
                        () ->   evaluator.loadData(graph));
        withDurationLog("making auth decision",
                        () -> checkExpectedAuthDecision(evaluator, accessDenied, resource.getFilename()));
    }

    private static void withDurationLog(String message, Runnable runnable) {
        long start = System.currentTimeMillis();
        runnable.run();
        logDuration(message, start);
    }

    private <T> T withDurationLog(String message, Supplier<T> supplier) {
        long start = System.currentTimeMillis();
        T val = supplier.get();
        logDuration(message, start);
        return val;
    }

    private static void logDuration(String message, long start) {
        logger.debug(message + " took {} ms", System.currentTimeMillis() - start);
    }

    public void validateTestData(Shapes shapes, Graph graph, boolean failTest, String shapesName) {
        ValidationReport shaclReport = ShaclSystem.get().validate(shapes, graph);
        if (failTest) {
            if (!shaclReport.conforms()) {
                ShLib.printReport(shaclReport);
                System.out.println();
                RDFDataMgr.write(System.out, shaclReport.getModel(), Lang.TTL);
            }
            Assert.assertTrue(shaclReport.conforms());
        } else {
            if (!shaclReport.conforms()) {
                logger.info("graph failed SHACL validation with {}", shapesName);
            }
        }
    }

    private void checkExpectedAuthDecision(WonAclEvaluator loadedEvaluator, DecisionValue expected,
                    String testIdentifier) {
        for (Authorization auth : loadedEvaluator.getAutorizations()) {
            for (OperationRequest opReq : loadedEvaluator.getOperationRequests()) {
                logger.debug("checking OpRequest {} against Authorization {} ", opReq.get_node(), auth.get_node());
                String message = getWrongAuthDecisionMessage(testIdentifier, expected, auth, opReq);
                AclEvalResult decision = null;
                long start = System.currentTimeMillis();
                for (int i = 0; i < 1; i++) {
                    decision = loadedEvaluator.decide(auth, opReq);
                }
                logDuration("making authorization decision", start);
                Assert.assertEquals(message, expected, decision.getDecision());
            }
        }
    }

    private String getWrongAuthDecisionMessage(String testIdentifier, DecisionValue expected, Authorization auth,
                    OperationRequest opReq) {
        testIdentifier = testIdentifier != null ? testIdentifier + ": " : "";
        testIdentifier = testIdentifier.replaceAll("(/[^/])[^/]+(?=/)", "$1"); // abbreviate path
        String message = String.format("%sAuthorization %s should %s" + "authorize operation %s but it does%s",
                        testIdentifier, auth.get_node(), expected.equals(DecisionValue.ACCESS_GRANTED) ? "" : "not ",
                        opReq.get_node(), expected.equals(DecisionValue.ACCESS_GRANTED) ? " not" : "");
        return message;
    }

    public static Stream<Resource> getOkOperationRequests() throws IOException {
        Resource[] files = ResourcePatternUtils.getResourcePatternResolver(loader)
                        .getResources("classpath:/won/opreq/ok/opreq*.ttl");
        return Stream.of(files);
    }

    public static Stream<Resource> getFailOperationRequests() throws IOException {
        Resource[] files = ResourcePatternUtils.getResourcePatternResolver(loader)
                        .getResources("classpath:/won/opreq/fail/opreq*.ttl");
        return Stream.of(files);
    }
}

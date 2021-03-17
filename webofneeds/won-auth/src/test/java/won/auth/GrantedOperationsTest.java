package won.auth;

import org.apache.jena.datatypes.xsd.XSDDateTime;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.compose.Difference;
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
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import won.auth.model.AseRoot;
import won.auth.model.OperationRequest;
import won.auth.model.RdfOutput;
import won.protocol.util.RdfUtils;
import won.shacl2java.Shacl2JavaInstanceFactory;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class)
public class GrantedOperationsTest {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    @Autowired
    private static ResourceLoader loader;
    @Value("classpath:/shacl/won-auth.ttl")
    private Resource shapesDef;
    @Value("classpath:/shacl/won-test-atom-shapes.ttl")
    private Resource atomDataShapesDef;

    @Configuration
    static class Config {
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

    static Pattern testInputPattern = Pattern.compile("(.+)-input.ttl");

    static String getTestBaseName(String testInputFilename) {
        Matcher m = testInputPattern.matcher(testInputFilename);
        if (m.matches()) {
            return m.group(1);
        }
        return null;
    }

    @Test
    public void testSet1() throws IOException {
        Shacl2JavaInstanceFactory factory = AuthUtils.instanceFactory();
        Shapes atomDataShapes = loadShapes(atomDataShapesDef);
        ModelBasedConnectionTargetCheckEvaluator targetAtomChecker = new ModelBasedConnectionTargetCheckEvaluator(
                        atomDataShapes,
                        "won.auth.test.model");
        ModelBasedAtomNodeChecker atomNodeChecker = new ModelBasedAtomNodeChecker(atomDataShapes,
                        "won.auth.test.model");
        String folder = "set1";
        for (Resource socketGraphResource : getInputs(folder)) {
            String baseName = getTestBaseName(socketGraphResource.getFilename());
            Graph inputGraph = loadGraph(socketGraphResource);
            String testIdentifier = folder + "/" + baseName;
            logger.debug("running test {}", testIdentifier);
            validateTestData(atomDataShapes, inputGraph, true, testIdentifier);
            validateTestData(AuthUtils.shapes(), inputGraph, true, testIdentifier);
            Shacl2JavaInstanceFactory.Accessor ac = factory.accessor(inputGraph);
            targetAtomChecker.loadData(inputGraph);
            atomNodeChecker.loadData(inputGraph);
            Set<OperationRequest> operationRequestSet = ac.getInstancesOfType(OperationRequest.class);
            if (operationRequestSet.size() > 1) {
                Assert.fail("only one operationRequest allowed per file");
            }
            OperationRequest operationRequest = operationRequestSet.stream().findFirst().get();
            setValidTokenExpiryDate(operationRequest);
            WonAclEvaluatorTestFactory evaluatorFactory = new WonAclEvaluatorTestFactory(targetAtomChecker,
                            atomNodeChecker, null);
            evaluatorFactory.load(inputGraph);
            WonAclEvaluator evaluator = evaluatorFactory.create(false);
            Optional<AseRoot> grantedOperations = evaluator
                            .getGrants(operationRequest);
            Graph actualResult = RdfOutput.toGraph(grantedOperations.get(), false);
            actualResult = new Difference(actualResult, AuthUtils.shapes().getGraph());
            Graph expectedResult = loadGraph(new ClassPathResource(
                            "/won/grantedOps/" + folder + "/" + baseName + "-expected.ttl"));
            assertIsomorphicGraphs(expectedResult, actualResult, testIdentifier);
        }
    }

    private static void assertIsomorphicGraphs(Graph expectedResult, Graph actualResult, String testIdentifier) {
        if (!expectedResult.isIsomorphicWith(actualResult)) {
            System.err.println("Test failed: " + testIdentifier);
            RdfUtils.Pair<Graph> diff = RdfUtils.diff(expectedResult, actualResult);
            Graph onlyInExpected = diff.getFirst();
            Graph onlyInActual = diff.getSecond();
            if (!onlyInExpected.isEmpty()) {
                System.err.println("\nThese expected triples are missing from the actual data:\n");
                RDFDataMgr.write(System.err, onlyInExpected, Lang.TTL);
            }
            if (!onlyInActual.isEmpty()) {
                System.err.println("\nThese unexpected triples should not be in actual data:\n");
                RDFDataMgr.write(System.err, onlyInActual, Lang.TTL);
            }
            System.err.println("\nExpected data:\n");
            RDFDataMgr.write(System.err, expectedResult, Lang.TTL);
            System.err.println("\nActual data:\n");
            RDFDataMgr.write(System.err, actualResult, Lang.TTL);
            Assert.fail(testIdentifier + ": resulting acl graph differs from expected");
        }
    }

    public static List<Resource> getInputs(String basePath) throws IOException {
        Resource[] files = ResourcePatternUtils.getResourcePatternResolver(loader)
                        .getResources("classpath:/won/grantedOps/" + basePath + "/test*-input.ttl");
        return Arrays.asList(files);
    }

    public void validateTestData(Shapes shapes, Graph graph, boolean failTest, String testCaseIdentifier) {
        ValidationReport shaclReport = ShaclSystem.get().validate(shapes, new Union(shapes.getGraph(), graph));
        if (failTest) {
            if (!shaclReport.conforms()) {
                ShLib.printReport(shaclReport);
                System.out.println();
                RDFDataMgr.write(System.out, shaclReport.getModel(), Lang.TTL);
            }
            Assert.assertTrue(String.format("%s: test data invalid", testCaseIdentifier), shaclReport.conforms());
        } else {
            if (!shaclReport.conforms()) {
                logger.info("failed SHACL validation: {}", testCaseIdentifier);
            }
        }
    }

    private void setValidTokenExpiryDate(OperationRequest opReq) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.add(Calendar.MINUTE, 30);
        opReq.getBearsTokens().forEach(token -> token.setTokenExp(new XSDDateTime(cal)));
    }
}

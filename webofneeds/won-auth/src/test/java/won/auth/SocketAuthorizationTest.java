package won.auth;

import org.apache.jena.graph.Graph;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.shacl.Shapes;
import org.apache.jena.sparql.graph.GraphFactory;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternUtils;
import won.auth.model.SocketDefinition;
import won.auth.socket.impl.SocketAuthorizationAclModifierAlgorithms;
import won.protocol.util.RdfUtils;
import won.shacl2java.Shacl2JavaInstanceFactory;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SocketAuthorizationTest {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final URI OWN_ATOM = URI.create("https://example.com/won/resource/atom/own-atom");
    private static final URI OWN_SOCKET1 = URI.create("https://example.com/won/resource/atom/own-atom#socket1");
    private static final URI OWN_SOCKET2 = URI.create("https://example.com/won/resource/atom/own-atom#socket2");
    private static final URI TARGET_ATOM = URI.create("https://example.com/won/resource/atom/target-atom");
    private static final URI TARGET_SOCKET = URI.create("https://example.com/won/resource/atom/target-atom#socket");
    @Autowired
    private static ResourceLoader loader;

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

    static Pattern testInputPattern = Pattern.compile("(.+)-socket.ttl");

    static String getTestBaseName(String testInputFilename) {
        Matcher m = testInputPattern.matcher(testInputFilename);
        if (m.matches()) {
            return m.group(1);
        }
        return null;
    }

    @Test
    public void testAddLocalAuth() throws IOException {
        Shacl2JavaInstanceFactory factory = AuthUtils.newInstanceFactory();
        SocketAuthorizationAclModifierAlgorithms socketAclAlgos = new SocketAuthorizationAclModifierAlgorithms();
        String folder = "addLocalAuth";
        for (Resource socketGraphResource : getInputs(folder)) {
            String baseName = getTestBaseName(socketGraphResource.getFilename());
            String testIdentifier = folder + "/" + baseName;
            logger.debug("running test {}", testIdentifier);
            Graph expectedResult = loadGraph(new ClassPathResource(
                            "/won/socket/" + folder + "/" + baseName + "-acl-after.ttl"));
            Graph aclBefore = loadGraph(new ClassPathResource(
                            "/won/socket/" + folder + "/" + baseName + "-acl-before.ttl"));
            factory.load(loadGraph(socketGraphResource));
            Set<SocketDefinition> sockets = factory.getInstancesOfType(SocketDefinition.class);
            for (SocketDefinition socket : sockets) {
                Graph actualResult = socketAclAlgos
                                .addAuthorizationsForSocket(aclBefore, socket.getLocalAuths(), OWN_SOCKET1, OWN_ATOM);
                assertIsomorphicGraphs(expectedResult, actualResult, testIdentifier);
            }
        }
    }

    @Test
    public void testAddTargetAuth() throws IOException {
        Shacl2JavaInstanceFactory factory = AuthUtils.newInstanceFactory();
        SocketAuthorizationAclModifierAlgorithms socketAclAlgos = new SocketAuthorizationAclModifierAlgorithms();
        String folder = "addTargetAuth";
        for (Resource socketGraphResource : getInputs(folder)) {
            String baseName = getTestBaseName(socketGraphResource.getFilename());
            String testIdentifier = folder + "/" + baseName;
            logger.debug("running test {}", testIdentifier);
            Graph expectedResult = loadGraph(new ClassPathResource(
                            "/won/socket/" + folder + "/" + baseName + "-acl-after.ttl"));
            Graph aclBefore = loadGraph(new ClassPathResource(
                            "/won/socket/" + folder + "/" + baseName + "-acl-before.ttl"));
            factory.load(loadGraph(socketGraphResource));
            Set<SocketDefinition> sockets = factory.getInstancesOfType(SocketDefinition.class);
            for (SocketDefinition socket : sockets) {
                Graph actualResult = socketAclAlgos
                                .addAuthorizationsForSocket(aclBefore, socket.getTargetAuths(), OWN_SOCKET1,
                                                TARGET_ATOM);
                assertIsomorphicGraphs(expectedResult, actualResult, testIdentifier);
            }
        }
    }

    @Test
    public void testRemoveTargetAuthRemoveSocket() throws IOException {
        Shacl2JavaInstanceFactory factory = AuthUtils.newInstanceFactory();
        SocketAuthorizationAclModifierAlgorithms socketAclAlgos = new SocketAuthorizationAclModifierAlgorithms();
        String folder = "removeTargetAuthRemoveSocket";
        for (Resource socketGraphResource : getInputs(folder)) {
            String baseName = getTestBaseName(socketGraphResource.getFilename());
            String testIdentifier = folder + "/" + baseName;
            logger.debug("running test {}", testIdentifier);
            Graph expectedResult = loadGraph(new ClassPathResource(
                            "/won/socket/" + folder + "/" + baseName + "-acl-after.ttl"));
            Graph aclBefore = loadGraph(new ClassPathResource(
                            "/won/socket/" + folder + "/" + baseName + "-acl-before.ttl"));
            factory.load(loadGraph(socketGraphResource));
            Set<SocketDefinition> sockets = factory.getInstancesOfType(SocketDefinition.class);
            for (SocketDefinition socket : sockets) {
                Graph actualResult = socketAclAlgos
                                .removeAuthorizationsForSocket(aclBefore, OWN_SOCKET1, TARGET_ATOM, true);
                assertIsomorphicGraphs(expectedResult, actualResult, testIdentifier);
            }
        }
    }

    @Test
    public void testRemoveTargetAuth() throws IOException {
        Shacl2JavaInstanceFactory factory = AuthUtils.newInstanceFactory();
        SocketAuthorizationAclModifierAlgorithms socketAclAlgos = new SocketAuthorizationAclModifierAlgorithms();
        String folder = "removeTargetAuth";
        for (Resource socketGraphResource : getInputs(folder)) {
            String baseName = getTestBaseName(socketGraphResource.getFilename());
            String testIdentifier = folder + "/" + baseName;
            logger.debug("running test {}", testIdentifier);
            Graph expectedResult = loadGraph(new ClassPathResource(
                            "/won/socket/" + folder + "/" + baseName + "-acl-after.ttl"));
            Graph aclBefore = loadGraph(new ClassPathResource(
                            "/won/socket/" + folder + "/" + baseName + "-acl-before.ttl"));
            factory.load(loadGraph(socketGraphResource));
            Set<SocketDefinition> sockets = factory.getInstancesOfType(SocketDefinition.class);
            for (SocketDefinition socket : sockets) {
                Graph actualResult = socketAclAlgos
                                .removeAuthorizationsForSocket(aclBefore, OWN_SOCKET1, TARGET_ATOM, false);
                assertIsomorphicGraphs(expectedResult, actualResult, testIdentifier);
            }
        }
    }

    @Test
    public void testRemoveLocalAuthRemoveSocket() throws IOException {
        Shacl2JavaInstanceFactory factory = AuthUtils.newInstanceFactory();
        SocketAuthorizationAclModifierAlgorithms socketAclAlgos = new SocketAuthorizationAclModifierAlgorithms();
        String folder = "removeLocalAuthRemoveSocket";
        for (Resource socketGraphResource : getInputs(folder)) {
            String baseName = getTestBaseName(socketGraphResource.getFilename());
            String testIdentifier = folder + "/" + baseName;
            logger.debug("running test {}", testIdentifier);
            Graph expectedResult = loadGraph(new ClassPathResource(
                            "/won/socket/" + folder + "/" + baseName + "-acl-after.ttl"));
            Graph aclBefore = loadGraph(new ClassPathResource(
                            "/won/socket/" + folder + "/" + baseName + "-acl-before.ttl"));
            factory.load(loadGraph(socketGraphResource));
            Set<SocketDefinition> sockets = factory.getInstancesOfType(SocketDefinition.class);
            for (SocketDefinition socket : sockets) {
                Graph actualResult = socketAclAlgos
                                .removeAuthorizationsForSocket(aclBefore, OWN_SOCKET1, TARGET_ATOM, true);
                assertIsomorphicGraphs(expectedResult, actualResult, testIdentifier);
            }
        }
    }

    @Test
    public void testRemoveLocalAuth() throws IOException {
        Shacl2JavaInstanceFactory factory = AuthUtils.newInstanceFactory();
        SocketAuthorizationAclModifierAlgorithms socketAclAlgos = new SocketAuthorizationAclModifierAlgorithms();
        String folder = "removeLocalAuth";
        for (Resource socketGraphResource : getInputs(folder)) {
            String baseName = getTestBaseName(socketGraphResource.getFilename());
            String testIdentifier = folder + "/" + baseName;
            logger.debug("running test {}", testIdentifier);
            Graph expectedResult = loadGraph(new ClassPathResource(
                            "/won/socket/" + folder + "/" + baseName + "-acl-after.ttl"));
            Graph aclBefore = loadGraph(new ClassPathResource(
                            "/won/socket/" + folder + "/" + baseName + "-acl-before.ttl"));
            factory.load(loadGraph(socketGraphResource));
            Set<SocketDefinition> sockets = factory.getInstancesOfType(SocketDefinition.class);
            for (SocketDefinition socket : sockets) {
                Graph actualResult = socketAclAlgos
                                .removeAuthorizationsForSocket(aclBefore, OWN_SOCKET1, TARGET_ATOM, false);
                assertIsomorphicGraphs(expectedResult, actualResult, testIdentifier);
            }
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
                        .getResources("classpath:/won/socket/" + basePath + "/test*-socket.ttl");
        return Arrays.asList(files);
    }
}

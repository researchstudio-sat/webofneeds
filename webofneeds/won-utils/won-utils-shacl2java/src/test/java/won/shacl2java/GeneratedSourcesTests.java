package won.shacl2java;

import org.apache.jena.ext.com.google.common.io.Files;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.shacl.Shapes;
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
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import test1.Address;
import test1.Person;
import test2.*;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class)
public class GeneratedSourcesTests {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    @Autowired
    private static ResourceLoader loader;
    @Value("classpath:/won/shacl2java/")
    private Resource testBaseFolder;

    public static Shapes loadShapes(Resource shapesResource) throws IOException {
        logger.debug("parsing shapes in {}", shapesResource.getFilename());
        Model shapesGraph = ModelFactory.createDefaultModel();
        RDFDataMgr.read(shapesGraph, shapesResource.getInputStream(), Lang.TTL);
        return Shapes.parse(shapesGraph.getGraph());
    }

    public static Model loadData(Resource shapesResource) throws IOException {
        logger.debug("parsing shapes in {}", shapesResource.getFilename());
        Model dataGraph = ModelFactory.createDefaultModel();
        RDFDataMgr.read(dataGraph, shapesResource.getInputStream(), Lang.TTL);
        return dataGraph;
    }

    private File getOutputDir() {
        File tempDir = Files.createTempDir();
        tempDir.deleteOnExit();
        return tempDir;
    }

    @Test
    public void test1Generated_data001() throws IOException {
        Shapes shapes = loadShapes(testBaseFolder.createRelative("test1/shapes.ttl"));
        Shacl2JavaConfig config = Shacl2JavaConfig.builder()
                        .packageName("test1").build();
        Model data = loadData(testBaseFolder.createRelative("test1/data-001.ttl"));
        Shacl2JavaInstanceFactory factory = new Shacl2JavaInstanceFactory(shapes, config.getPackageName());
        Shacl2JavaInstanceFactory.Accessor accessor = factory.accessor(data.getGraph());
        Map<String, Set<Object>> entities = accessor.getInstanceMap();
        Assert.assertEquals(4, entities.size());
        Optional<Person> bob = accessor.getInstanceOfType("https://example.com/ns#Bob", Person.class);
        Assert.assertTrue(bob.isPresent());
        Optional<Address> address = accessor.getInstanceOfType("https://example.com/ns#BobsAddress", Address.class);
        Assert.assertTrue(address.isPresent());
        Assert.assertSame(address.get(), bob.get().getAddresses().stream().findFirst().get());
        Assert.assertEquals("1234", bob.get().getAddresses().stream().findFirst().get().getPostalCode());
    }

    @Test
    public void test1Generated_rdfgen002() throws IOException {
        Shapes shapes = loadShapes(testBaseFolder.createRelative("test1/shapes.ttl"));
        Person bob = new Person("http://example.com/ns#Bob");
        bob.setNode(NodeFactory.createURI("http://example.com/ns#Bob"));
        bob.addFirstName("Bob");
        bob.setLastName("The Builder");
        Address address = new Address();
        address.setPostalCode("1234");
        bob.addAddress(address);
        Graph graph = test1.RdfOutput.toGraph(bob);
        Model expected = loadData(new ClassPathResource("won/shacl2java/test1/expected-rdfgen002.ttl"));
        Assert.assertTrue("generated graph differs from expected",
                        graph.isIsomorphicWith(expected.getGraph()));
    }

    @Test
    public void test2Generated_auth001() throws IOException {
        Shapes shapes = loadShapes(testBaseFolder.createRelative("test2/shapes.ttl"));
        Shacl2JavaConfig config = Shacl2JavaConfig.builder()
                        .packageName("test2").build();
        Model data = loadData(testBaseFolder.createRelative("test2/auth-001.ttl"));
        Shacl2JavaInstanceFactory factory = new Shacl2JavaInstanceFactory(shapes, config.getPackageName());
        Shacl2JavaInstanceFactory.Accessor accessor = factory.accessor(data.getGraph());
        Map<String, Set<Object>> entities = accessor.getInstanceMap();
        Assert.assertEquals(4, entities.size());
        Object o = entities.get("https://example.com/test/atom1#authorization1").stream().findFirst().get();
        Assert.assertEquals(Authorization.class, o.getClass());
        Authorization authShape = (Authorization) o;
        Assert.assertEquals(1, authShape.getGranteesAtomExpression().size());
        Assert.assertTrue(authShape.getGranteesAtomExpression().stream().findFirst().get().getAtomsURI()
                        .contains(URI.create("https://example.com/test/atom2")));
        Assert.assertNotNull(authShape.getGrants());
        AseRoot grant = (AseRoot) authShape.getGrants().stream().findAny().get();
        Assert.assertNotNull(grant.getAtomStates());
        Assert.assertNotNull(grant.getOperationsUnion());
        Assert.assertEquals(AtomState.class, grant.getAtomStates().stream().findAny().get().getClass());
        Assert.assertEquals(SimpleOperationExpression.class,
                        grant.getOperationsUnion().stream().findAny().get().getClass());
        // just make sure obtaining rdf throws no exceptions
        RDFDataMgr.write(new StringWriter(), RdfOutput.toGraph(authShape), Lang.TTL);
    }

    @Test
    public void test2Generated_auth002() throws IOException {
        Shapes shapes = loadShapes(testBaseFolder.createRelative("test2/shapes.ttl"));
        Shacl2JavaConfig config = Shacl2JavaConfig.builder()
                        .packageName("test2").build();
        Model data = loadData(testBaseFolder.createRelative("test2/auth-002.ttl"));
        Shacl2JavaInstanceFactory factory = new Shacl2JavaInstanceFactory(shapes, config.getPackageName());
        Shacl2JavaInstanceFactory.Accessor accessor = factory.accessor(data.getGraph());
        Map<String, Set<Object>> entities = accessor.getInstanceMap();
        Assert.assertEquals(10, entities.size());
        Object o = entities.get("https://example.com/test/atom1#authorization5").stream().findFirst().get();
        Assert.assertEquals(Authorization.class, o.getClass());
        Authorization authShape = (Authorization) o;
        Assert.assertEquals(1, authShape.getGranteesAtomExpression().size());
        // just make sure obtaining rdf throws no exceptions
        RDFDataMgr.write(new StringWriter(), RdfOutput.toGraph(authShape), Lang.TTL);
    }

    @Test
    public void test2Generated_auth003() throws IOException {
        Shapes shapes = loadShapes(testBaseFolder.createRelative("test2/shapes.ttl"));
        Shacl2JavaConfig config = Shacl2JavaConfig.builder()
                        .packageName("test2").build();
        Model data = loadData(testBaseFolder.createRelative("test2/auth-003.ttl"));
        Shacl2JavaInstanceFactory factory = new Shacl2JavaInstanceFactory(shapes, config.getPackageName());
        Shacl2JavaInstanceFactory.Accessor accessor = factory.accessor(data.getGraph());
        Map<String, Set<Object>> entities = accessor.getInstanceMap();
        Assert.assertEquals(8, entities.size());
        Object o = entities.get("https://example.com/test/atom1#authorization2").stream().findFirst().get();
        Assert.assertEquals(Authorization.class, o.getClass());
        Authorization authShape = (Authorization) o;
        Assert.assertEquals(1, authShape.getGranteesAtomExpression().size());
        Set grantees = authShape.getGranteesAseRoot();
        grantees.forEach(g -> logger.info("grantee: {} ", g));
        // just make sure obtaining rdf throws no exceptions
        RDFDataMgr.write(new StringWriter(), RdfOutput.toGraph(authShape), Lang.TTL);
    }

    @Configuration
    static class AuthShapeTestContextConfiguration {
    }
}

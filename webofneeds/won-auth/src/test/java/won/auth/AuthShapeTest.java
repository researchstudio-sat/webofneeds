package won.auth;

import org.apache.jena.graph.Graph;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.shacl.ShaclValidator;
import org.apache.jena.shacl.Shapes;
import org.apache.jena.shacl.ValidationReport;
import org.apache.jena.shacl.lib.ShLib;
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

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.stream.Stream;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class)
public class AuthShapeTest {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    @Autowired
    private static ResourceLoader loader;
    @Value("classpath:/shacl/won-auth-shapes.ttl")
    private Resource shapesDef;

    @Configuration
    static class AuthShapeTestContextConfiguration {
    }

    public static Shapes loadShapes(Resource shapesResource) throws IOException {
        logger.debug("parsing shapes in {}", shapesResource.getFilename());
        Model shapesGraph = ModelFactory.createDefaultModel();
        RDFDataMgr.read(shapesGraph, shapesResource.getInputStream(), Lang.TTL);
        return Shapes.parse(shapesGraph);
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
        ValidationReport report = ShaclValidator.get().validate(shapes, g);
        long duration = CPUUtils.getCpuTime() - start;
        logger.debug("validation took {} millis ", (double) duration / 1000000d);
        if (!report.conforms()) {
            ShLib.printReport(report);
            System.out.println();
            RDFDataMgr.write(System.out, report.getModel(), Lang.TTL);
        }
        Assert.assertTrue(report.conforms());
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
            ShLib.printReport(report);
            System.out.println();
            RDFDataMgr.write(System.out, report.getModel(), Lang.TTL);
        }
        Assert.assertTrue(report.conforms());
    }

    public static Stream<Resource> getAuthorizations() throws IOException {
        Resource[] files = ResourcePatternUtils.getResourcePatternResolver(loader)
                        .getResources("classpath:/won/auth/*.ttl");
        return Stream.of(files);
    }

    public static Stream<Resource> getOperationRequests() throws IOException {
        Resource[] okFiles = ResourcePatternUtils.getResourcePatternResolver(loader)
                        .getResources("classpath:/won/opreq/ok/*.ttl");
        Resource[] failFiles = ResourcePatternUtils.getResourcePatternResolver(loader)
                        .getResources("classpath:/won/opreq/ok/*.ttl");
        return Stream.concat(Stream.of(okFiles), Stream.of(failFiles));
    }
}

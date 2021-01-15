package won.shacl2java;

import com.squareup.javapoet.TypeSpec;
import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.Set;
import org.apache.jena.ext.com.google.common.io.Files;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.shacl.Shapes;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import won.shacl2java.sourcegen.SourceGenerator;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class)
public class SourceGeneratorTests {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    @Autowired
    private static ResourceLoader loader;
    @Value("classpath:/won/shacl2java/")
    private Resource testBaseFolder;

    @Configuration
    static class AuthShapeTestContextConfiguration {
    }

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
    public void test1() throws IOException {
        Shapes shapes = loadShapes(testBaseFolder.createRelative("test1/shapes.ttl"));
        String outputDir = getOutputDir().getAbsolutePath();
        Shacl2JavaConfig config = Shacl2JavaConfig.builder()
                        .packageName("test1")
                        .outputDir(outputDir).build();
        SourceGenerator gen = new SourceGenerator();
        Set<TypeSpec> typeSpecs = gen.generateTypes(shapes, config);
        SourceGenerator.writeClasses(typeSpecs, config);
        logger.debug("wrote classes to {} ", outputDir);
    }

    @Test
    public void test2() throws IOException {
        Shapes shapes = loadShapes(testBaseFolder.createRelative("test2/shapes.ttl"));
        String outputDir = getOutputDir().getAbsolutePath();
        Shacl2JavaConfig config = Shacl2JavaConfig.builder()
                        .packageName("test2")
                        .classNameRegexReplace("(Property)?Shape$", "")
                        .addVisitorClass(URI.create("https://w3id.org/won/auth#TreeExpressionShape"))
                        .outputDir(outputDir).build();
        SourceGenerator gen = new SourceGenerator();
        Set<TypeSpec> typeSpecs = gen.generateTypes(shapes, config);
        SourceGenerator.writeClasses(typeSpecs, config);
        logger.debug("wrote classes to {} ", outputDir);
    }

    @Test
    public void test3() throws IOException {
        Shapes shapes = loadShapes(testBaseFolder.createRelative("test3/shapes.ttl"));
        String outputDir = getOutputDir().getAbsolutePath();
        Shacl2JavaConfig config = Shacl2JavaConfig.builder()
                        .packageName("test3")
                        .classNameRegexReplace("(Property)?Shape$", "")
                        .outputDir(outputDir).build();
        SourceGenerator gen = new SourceGenerator();
        Set<TypeSpec> typeSpecs = gen.generateTypes(shapes, config);
        SourceGenerator.writeClasses(typeSpecs, config);
        logger.debug("wrote classes to {} ", outputDir);
    }
}

package won.shacl2java.testutil;

import ch.qos.logback.classic.Level;
import com.squareup.javapoet.TypeSpec;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.net.URL;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.shacl.Shapes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.shacl2java.Shacl2JavaConfig;
import won.shacl2java.sourcegen.SourceGenerator;

public class Shacl2JavaTestSourcesGenerator {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static Shapes loadShapes(URL shapesResource) throws IOException {
        logger.debug("parsing shapes in {}", shapesResource.toString());
        Model shapesGraph = ModelFactory.createDefaultModel();
        RDFDataMgr.read(shapesGraph, new FileInputStream(new File(shapesResource.getPath())), Lang.TTL);
        return Shapes.parse(shapesGraph.getGraph());
    }

    public static void main(String[] args) throws IOException {
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory
                        .getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.WARN);
        String inputDir = args[0];
        String outputDir = args[1];
        logger.debug("Processing test data in {}", inputDir);
        logger.debug("Generating classes in {}", outputDir);
        File outputDirFile = new File(outputDir);
        File inputDirFile = new File(inputDir);
        generateInDir(inputDirFile, outputDirFile);
    }

    private static void generateInDir(File inputDirFile, File outputDirFile) throws IOException {
        outputDirFile.mkdirs();
        File[] testfolders = inputDirFile.listFiles((FileFilter) DirectoryFileFilter.INSTANCE);
        for (int i = 0; i < Objects.requireNonNull(testfolders).length; i++) {
            logger.debug("Processing test folder {}", testfolders[i]);
            String packageName = testfolders[i].getName();
            Shapes shapes = loadShapes(new File(testfolders[i], "shapes.ttl").toURI().toURL());
            Shacl2JavaConfig config = Shacl2JavaConfig.builder()
                            .packageName(packageName)
                            .classNameRegexReplace("(Property)?Shape$", "")
                            .abbreviateTypeIndicators(false)
                            .alwaysAddTypeIndicator(false)
                            .addVisitorClass(URI.create("https://w3id.org/won/auth#TreeExpressionShape"))
                            .outputDir(outputDirFile.getAbsolutePath()).build();
            SourceGenerator gen = new SourceGenerator();
            Set<TypeSpec> typeSpecs = gen.generateTypes(shapes, config);
            SourceGenerator.writeClasses(typeSpecs, config);
            logger.debug("wrote classes to {} ", outputDirFile);
        }
    }
}

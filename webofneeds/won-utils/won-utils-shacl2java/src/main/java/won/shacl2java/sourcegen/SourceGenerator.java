package won.shacl2java.sourcegen;

import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.shacl.Shapes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.shacl2java.Shacl2JavaConfig;
import won.shacl2java.sourcegen.typegen.TypegenContext;
import won.shacl2java.sourcegen.typegen.logic.IndividualsGenerator;
import won.shacl2java.sourcegen.typegen.logic.MainTypesGenerator;
import won.shacl2java.sourcegen.typegen.logic.MainTypesPostprocessor;
import won.shacl2java.sourcegen.typegen.logic.ShapeTypeInterfaceGenerator;
import won.shacl2java.sourcegen.typegen.logic.ShapeTypeInterfaceImplementer;
import won.shacl2java.sourcegen.typegen.logic.ShapeTypeInterfacePopulator;
import won.shacl2java.sourcegen.typegen.logic.UnionEmulationPostprocessor;
import won.shacl2java.sourcegen.typegen.logic.VisitorAcceptMethodAdder;
import won.shacl2java.sourcegen.typegen.logic.VisitorImplGenerator;
import won.shacl2java.sourcegen.typegen.logic.VisitorInterfaceGenerator;
import won.shacl2java.sourcegen.typegen.mapping.IndividualClassNames;
import won.shacl2java.sourcegen.typegen.mapping.ShapeTargetClasses;
import won.shacl2java.sourcegen.typegen.mapping.ShapeTypeImplTypes;
import won.shacl2java.sourcegen.typegen.mapping.ShapeTypeInterfaceTypes;
import won.shacl2java.sourcegen.typegen.mapping.ShapeTypeSpecs;
import won.shacl2java.sourcegen.typegen.mapping.TypeSpecNames;
import won.shacl2java.sourcegen.typegen.mapping.VisitorClassTypeSpecs;
import won.shacl2java.sourcegen.typegen.mapping.VisitorInterfaceTypes;
import won.shacl2java.sourcegen.typegen.support.NameClashDetector;

public class SourceGenerator {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public SourceGenerator() {
    }

    public SourceGeneratorStats generate(File shapesFile, Shacl2JavaConfig config) throws IOException {
        long start = System.currentTimeMillis();
        Shapes shapes = readShapes(shapesFile);
        Duration readShapesDuration = Duration.of(System.currentTimeMillis() - start, ChronoUnit.MILLIS);
        start = System.currentTimeMillis();
        Set<TypeSpec> typeSpecs = generateTypes(shapes, config);
        Duration generationDuration = Duration.of(System.currentTimeMillis() - start, ChronoUnit.MILLIS);
        start = System.currentTimeMillis();
        SourceGenerator.writeClasses(typeSpecs, config);
        Duration writeDuration = Duration.of(System.currentTimeMillis() - start, ChronoUnit.MILLIS);
        SourceGeneratorStats stats = new SourceGeneratorStats();
        stats.setShapesFile(shapesFile.toString());
        stats.setNumClasses(typeSpecs.size());
        stats.setOutputDir(config.getOutputDir());
        stats.setReadShapesDuration(readShapesDuration);
        stats.setGenerationDuration(generationDuration);
        stats.setWriteDuration(writeDuration);
        return stats;
    }

    public static Shapes readShapes(File shapesFile) throws FileNotFoundException {
        Model shapesGraph = ModelFactory.createDefaultModel();
        RDFDataMgr.read(shapesGraph, new FileInputStream(shapesFile), Lang.TTL);
        return Shapes.parse(shapesGraph.getGraph());
    }

    public Set<TypeSpec> generateTypes(Shapes shapes, Shacl2JavaConfig config) {
        TypegenContext ctx = new TypegenContext();
        NameClashDetector nameClashDetector = new NameClashDetector();
        IndividualClassNames individualClassNames = new IndividualClassNames();
        TypeSpecNames typeSpecNames = new TypeSpecNames();
        ctx.manageMapping(typeSpecNames);
        ShapeTargetClasses shapeTargetClasses = new ShapeTargetClasses();
        ShapeTypeInterfaceTypes shapeTypeInterfaceTypes = new ShapeTypeInterfaceTypes();
        ctx.manageMapping(shapeTypeInterfaceTypes);
        ShapeTypeImplTypes shapeTypeImplTypes = new ShapeTypeImplTypes();
        ctx.manageMapping(shapeTypeImplTypes);
        ShapeTypeSpecs shapeTypeSpecs = new ShapeTypeSpecs();
        ctx.manageMapping(shapeTypeSpecs);
        VisitorClassTypeSpecs visitorClassTypeSpecs = new VisitorClassTypeSpecs();
        ctx.manageMapping(visitorClassTypeSpecs);
        VisitorInterfaceTypes visitorInterfaceTypes = new VisitorInterfaceTypes();
        ctx.manageMapping(visitorInterfaceTypes);
        IndividualsGenerator individualsGenerator = new IndividualsGenerator(shapes, config,
                        individualClassNames.producer());
        MainTypesGenerator mainTypesGenerator = new MainTypesGenerator(
                        shapes, config, nameClashDetector,
                        individualClassNames.consumer(),
                        shapeTypeSpecs.producer(),
                        visitorClassTypeSpecs.producer(),
                        typeSpecNames.producer(),
                        shapeTargetClasses.producer());
        MainTypesPostprocessor mainTypesPostprocessor = new MainTypesPostprocessor(
                        shapes,
                        config,
                        shapeTypeSpecs.consumer(),
                        individualClassNames.consumer(),
                        typeSpecNames.consumer(),
                        shapeTargetClasses.consumer());
        ShapeTypeInterfaceGenerator interfaceGenerator = new ShapeTypeInterfaceGenerator(
                        shapes,
                        config, shapeTypeInterfaceTypes.producer(),
                        nameClashDetector, typeSpecNames.producer());
        ShapeTypeInterfaceImplementer interfaceImplementer = new ShapeTypeInterfaceImplementer(
                        shapes,
                        config,
                        shapeTypeSpecs.consumer(),
                        shapeTypeInterfaceTypes.consumer(),
                        shapeTypeImplTypes.producer());
        ShapeTypeInterfacePopulator interfacePopulator = new ShapeTypeInterfacePopulator(
                        shapeTypeInterfaceTypes.consumer(),
                        shapeTypeImplTypes.consumer(), config);
        VisitorInterfaceGenerator visitorInterfaceGenerator = new VisitorInterfaceGenerator(
                        config,
                        visitorClassTypeSpecs.consumer(),
                        shapeTypeSpecs.consumer(),
                        visitorInterfaceTypes.producer());
        VisitorImplGenerator visitorImplGenerator = new VisitorImplGenerator(
                        config,
                        visitorClassTypeSpecs.consumer(),
                        shapeTypeSpecs.consumer());
        VisitorAcceptMethodAdder visitorAcceptMethodAdder = new VisitorAcceptMethodAdder(
                        config,
                        visitorClassTypeSpecs.consumer(),
                        visitorInterfaceTypes.consumer(),
                        shapeTypeSpecs.consumer());
        UnionEmulationPostprocessor unionEmulationPostprocessor = new UnionEmulationPostprocessor(
                        config,
                        shapeTypeInterfaceTypes.consumer(),
                        shapeTypeImplTypes.consumer());
        ctx.applyGenerator(individualsGenerator);
        ctx.applyGenerator(mainTypesGenerator);
        ctx.applyGenerator(interfaceGenerator);
        ctx.applyPostpropcessor(interfaceImplementer);
        ctx.applyPostpropcessor(mainTypesPostprocessor);
        ctx.applyPostpropcessor(interfacePopulator);
        ctx.applyGenerator(visitorInterfaceGenerator);
        ctx.applyPostpropcessor(visitorAcceptMethodAdder);
        ctx.applyGenerator(visitorImplGenerator);
        ctx.applyPostpropcessor(unionEmulationPostprocessor);
        return ctx.getTypeSpecs();
    }

    public static void writeClasses(Set<TypeSpec> types, Shacl2JavaConfig config) throws IOException {
        File outputDir = new File(config.getOutputDir());
        outputDir.mkdirs();
        for (TypeSpec typeSpec : types) {
            JavaFile file = JavaFile.builder(config.getPackageName(), typeSpec).build();
            file.writeTo(outputDir);
        }
    }
}

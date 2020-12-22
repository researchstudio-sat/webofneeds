package won.shacl2java.sourcegen.typegen.logic;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.lang.model.element.Modifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.shacl2java.Shacl2JavaConfig;
import won.shacl2java.sourcegen.typegen.TypesGenerator;
import won.shacl2java.sourcegen.typegen.mapping.ShapeTypeSpecs;
import won.shacl2java.sourcegen.typegen.mapping.VisitorClassTypeSpecs;
import won.shacl2java.sourcegen.typegen.mapping.VisitorInterfaceTypes;
import won.shacl2java.util.NameUtils;

import static javax.lang.model.element.Modifier.DEFAULT;
import static javax.lang.model.element.Modifier.PUBLIC;

public class VisitorInterfaceGenerator implements TypesGenerator {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private VisitorClassTypeSpecs.Consumer visitorClassTypeSpecs;
    private ShapeTypeSpecs.Consumer shapeTypeSpecs;
    private VisitorInterfaceTypes.Producer visitorInterfaceTypes;
    private Shacl2JavaConfig config;

    public VisitorInterfaceGenerator(Shacl2JavaConfig config,
                    VisitorClassTypeSpecs.Consumer visitorClassTypeSpecs,
                    ShapeTypeSpecs.Consumer shapeTypeSpecs,
                    VisitorInterfaceTypes.Producer visitorInterfaceTypes) {
        this.visitorClassTypeSpecs = visitorClassTypeSpecs;
        this.shapeTypeSpecs = shapeTypeSpecs;
        this.visitorInterfaceTypes = visitorInterfaceTypes;
        this.config = config;
    }

    @Override
    public Set<TypeSpec> generate() {
        logger.debug("generating visitors for visitor classes {}",
                        visitorClassTypeSpecs.keySet().stream()
                                        .map(URI::toString)
                                        .collect(Collectors.joining(",", "[", "]")));
        Set<TypeSpec> newTypeSpecs = new HashSet<>();
        for (URI visitorClass : config.getVisitorClasses()) {
            // generate a visitor interface
            String visitorName = NameUtils.classNameForShapeURI(visitorClass, config) + config.getVisitorSuffix();
            String hostInterfaceName = NameUtils.classNameForShapeURI(visitorClass, config);
            Optional<Set<TypeSpec>> hostClassesOpt = visitorClassTypeSpecs.get(visitorClass);
            if (hostClassesOpt.isPresent()) {
                Set<TypeSpec> hostClasses = hostClassesOpt.get();
                TypeSpec visitorInterface = generateVisitorInterface(
                                ClassName.get(config.getPackageName(), visitorName), hostClasses);
                newTypeSpecs.add(visitorInterface);
                visitorInterfaceTypes.put(visitorClass, visitorInterface);
            }
        }
        Collection<TypeSpec> hostClasses = shapeTypeSpecs.values();
        ClassName visitorInterfaceName = ClassName.get(config.getPackageName(),
                        config.DEFAULT_VISITOR_FOR_ALL_CLASSES_NAME);
        ClassName visitorHostInterfaceName = ClassName.get(config.getPackageName(),
                        config.DEFAULT_VISITOR_HOST_FOR_ALL_CLASSES_NAME);
        TypeSpec visitorInterface = generateVisitorInterface(visitorInterfaceName,
                        hostClasses);
        newTypeSpecs.add(visitorInterface);
        TypeSpec visitorHostInterface = generateVisitorHostInterface(visitorHostInterfaceName, visitorInterfaceName);
        newTypeSpecs.add(visitorHostInterface);
        return newTypeSpecs;
    }

    public TypeSpec generateVisitorInterface(ClassName visitorName, Collection<TypeSpec> hostClasses) {
        TypeSpec.Builder ifBuilder = TypeSpec
                        .interfaceBuilder(visitorName)
                        .addModifiers(PUBLIC);
        for (TypeSpec typeSpec : hostClasses) {
            logger.debug("proessing type {} as part of visitor class {} for generating interface",
                            typeSpec.name, visitorName);
            ifBuilder.addMethod(
                            MethodSpec
                                            .methodBuilder("visit")
                                            .addModifiers(DEFAULT, PUBLIC)
                                            .addParameter(ClassName.get(config.getPackageName(), typeSpec.name),
                                                            "host")
                                            .build());
        }
        TypeSpec visitorInterface = ifBuilder.build();
        return visitorInterface;
    }

    public TypeSpec generateVisitorHostInterface(ClassName interfaceName, ClassName visitorName) {
        TypeSpec.Builder ifBuilder = TypeSpec
                        .interfaceBuilder(interfaceName)
                        .addModifiers(PUBLIC)
                        .addMethod(MethodSpec.methodBuilder("accept")
                                        .addParameter(visitorName, "visitor")
                                        .addModifiers(PUBLIC, Modifier.ABSTRACT)
                                        .build());
        TypeSpec visitorInterface = ifBuilder.build();
        return visitorInterface;
    }
}

package won.shacl2java.sourcegen.typegen.logic;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.shacl2java.Shacl2JavaConfig;
import won.shacl2java.sourcegen.typegen.TypesGenerator;
import won.shacl2java.sourcegen.typegen.mapping.VisitorClassTypeSpecs;
import won.shacl2java.sourcegen.typegen.mapping.VisitorInterfaceTypes;
import won.shacl2java.util.NameUtils;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static javax.lang.model.element.Modifier.DEFAULT;
import static javax.lang.model.element.Modifier.PUBLIC;

public class VisitorInterfaceGenerator implements TypesGenerator {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private VisitorClassTypeSpecs.Consumer visitorClassTypeSpecs;
    private VisitorInterfaceTypes.Producer visitorInterfaceTypes;
    private Shacl2JavaConfig config;

    public VisitorInterfaceGenerator(Shacl2JavaConfig config, VisitorClassTypeSpecs.Consumer visitorClassTypeSpecs,
                    VisitorInterfaceTypes.Producer visitorInterfaceTypes) {
        this.visitorClassTypeSpecs = visitorClassTypeSpecs;
        this.visitorInterfaceTypes = visitorInterfaceTypes;
        this.config = config;
    }

    @Override
    public Set<TypeSpec> generate() {
        logger.debug("generating visitors for visitor classes {}", visitorClassTypeSpecs.keySet().stream()
                        .map(URI::toString)
                        .collect(Collectors.joining(",", "[", "]")));
        Set<TypeSpec> newTypeSpecs = new HashSet<>();
        for (URI visitorClass : config.getVisitorClasses()) {
            // generate a visitor interface
            String visitorName = NameUtils.classNameForShapeURI(visitorClass, config) + config.getVisitorSuffix();
            String hostInterfaceName = NameUtils.classNameForShapeURI(visitorClass, config);
            ClassName hostInterfaceClassName = ClassName.get(config.getPackageName(), hostInterfaceName);
            TypeSpec.Builder ifBuilder = TypeSpec
                            .interfaceBuilder(visitorName)
                            .addModifiers(PUBLIC);
            if (visitorClassTypeSpecs.containsKey(visitorClass)) {
                for (TypeSpec typeSpec : visitorClassTypeSpecs.get(visitorClass).get()) {
                    logger.debug("proessing type {} as part of visitor class {} for generating interface",
                                    typeSpec.name, visitorClass);
                    ifBuilder.addMethod(
                                    MethodSpec
                                                    .methodBuilder("visit")
                                                    .addModifiers(DEFAULT, PUBLIC)
                                                    .addParameter(ClassName.get(config.getPackageName(), typeSpec.name),
                                                                    "host")
                                                    .build());
                }
                TypeSpec visitorInterface = ifBuilder.build();
                newTypeSpecs.add(visitorInterface);
                visitorInterfaceTypes.put(visitorClass, visitorInterface);
            }
        }
        return newTypeSpecs;
    }
}

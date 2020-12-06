package won.shacl2java.sourcegen.typegen.logic;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.shacl2java.Shacl2JavaConfig;
import won.shacl2java.sourcegen.typegen.TypesPostprocessor;
import won.shacl2java.sourcegen.typegen.mapping.VisitorClassTypeSpecs;
import won.shacl2java.sourcegen.typegen.mapping.VisitorInterfaceTypes;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static javax.lang.model.element.Modifier.PUBLIC;
import static won.shacl2java.util.CollectionUtils.addToMultivalueMap;

public class VisitorAcceptMethodAdder implements TypesPostprocessor {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private Shacl2JavaConfig config;
    private VisitorClassTypeSpecs.Consumer visitorClassTypeSpecs;
    private VisitorInterfaceTypes.Consumer visitorInterfaceTypes;

    public VisitorAcceptMethodAdder(Shacl2JavaConfig config,
                    VisitorClassTypeSpecs.Consumer visitorClassTypeSpecs,
                    VisitorInterfaceTypes.Consumer visitorInterfaceTypes) {
        this.config = config;
        this.visitorClassTypeSpecs = visitorClassTypeSpecs;
        this.visitorInterfaceTypes = visitorInterfaceTypes;
    }

    @Override
    public Map<TypeSpec, TypeSpec> postprocess(Set<TypeSpec> typeSpecs) {
        logger.debug("generating visitors for visitor classes {}", visitorClassTypeSpecs.keySet().stream()
                        .map(URI::toString)
                        .collect(Collectors.joining(",", "[", "]")));
        Map<TypeSpec, Set<TypeSpec>> visitorIftoImpls = new HashMap();
        for (URI visitorClassUri : visitorInterfaceTypes.keySet()) {
            visitorIftoImpls.put(visitorInterfaceTypes.get(visitorClassUri).get(),
                            visitorClassTypeSpecs.get(visitorClassUri).get());
        }
        return visitorIftoImpls
                        .entrySet()
                        .stream()
                        .flatMap(ifToImpl -> ifToImpl.getValue()
                                        .stream()
                                        .map(impl -> {
                                            TypeSpec visitorInterface = ifToImpl.getKey();
                                            TypeSpec.Builder modifyingBuilder = impl.toBuilder();
                                            addAcceptMethodsForVisitorInterface(impl, modifyingBuilder,
                                                            visitorInterface, config);
                                            return new ImmutablePair<TypeSpec, TypeSpec>(impl,
                                                            modifyingBuilder.build());
                                        }))
                        .collect(Collectors.toMap(p -> p.getLeft(), p -> p.getRight()));
    }

    private void addAcceptMethodsForVisitorInterface(TypeSpec typeSpec, TypeSpec.Builder modifyingBuilder,
                    TypeSpec visitorInterface, Shacl2JavaConfig config) {
        ClassName visitorClassName = ClassName.get(config.getPackageName(), visitorInterface.name);
        // generate an accept/acceptRecursively method in our class
        logger.debug("adding accept(visitor) methods to {}", typeSpec.name);
        MethodSpec accept = MethodSpec.methodBuilder("accept")
                        .addModifiers(PUBLIC)
                        .addParameter(visitorClassName, "visitor")
                        .addStatement("$N.visit(this)", "visitor")
                        .build();
        modifyingBuilder.addMethod(accept);
    }
}

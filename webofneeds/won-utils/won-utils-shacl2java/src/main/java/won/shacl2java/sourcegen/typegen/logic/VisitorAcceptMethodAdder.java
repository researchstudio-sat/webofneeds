package won.shacl2java.sourcegen.typegen.logic;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.shacl2java.Shacl2JavaConfig;
import won.shacl2java.sourcegen.typegen.TypesPostprocessor;
import won.shacl2java.sourcegen.typegen.mapping.ShapeTypeSpecs;
import won.shacl2java.sourcegen.typegen.mapping.VisitorClassTypeSpecs;
import won.shacl2java.sourcegen.typegen.mapping.VisitorInterfaceTypes;

import static javax.lang.model.element.Modifier.PUBLIC;

public class VisitorAcceptMethodAdder implements TypesPostprocessor {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private Shacl2JavaConfig config;
    private VisitorClassTypeSpecs.Consumer visitorClassTypeSpecs;
    private VisitorInterfaceTypes.Consumer visitorInterfaceTypes;
    private ShapeTypeSpecs.Consumer shapeTypeSpecs;

    public VisitorAcceptMethodAdder(Shacl2JavaConfig config,
                    VisitorClassTypeSpecs.Consumer visitorClassTypeSpecs,
                    VisitorInterfaceTypes.Consumer visitorInterfaceTypes,
                    ShapeTypeSpecs.Consumer shapeTypeSpecs) {
        this.config = config;
        this.visitorClassTypeSpecs = visitorClassTypeSpecs;
        this.visitorInterfaceTypes = visitorInterfaceTypes;
        this.shapeTypeSpecs = shapeTypeSpecs;
    }

    @Override
    public Map<TypeSpec, TypeSpec> postprocess(Set<TypeSpec> typeSpecs) {
        logger.debug("generating visitors for visitor classes {}", visitorClassTypeSpecs.keySet().stream()
                        .map(URI::toString)
                        .collect(Collectors.joining(",", "[", "]")));
        Map<TypeSpec, TypeSpec> changes = new HashMap();
        for (URI visitorClassUri : visitorInterfaceTypes.keySet()) {
            TypeSpec visitorInterface = visitorInterfaceTypes.get(visitorClassUri).get();
            ClassName visitorInterfaceName = ClassName.get(config.getPackageName(), visitorInterface.name);
            Set<TypeSpec> hosts = visitorClassTypeSpecs.get(visitorClassUri).get();
            for (TypeSpec host : hosts) {
                addAcceptMethod(host, visitorInterfaceName, changes);
            }
        }
        for (TypeSpec host : shapeTypeSpecs.values()) {
            addAcceptMethod(host,
                            ClassName.get(config.getPackageName(), config.DEFAULT_VISITOR_FOR_ALL_CLASSES_NAME),
                            changes);
            addImplementsHostInterface(host,
                            ClassName.get(config.getPackageName(), config.DEFAULT_VISITOR_HOST_FOR_ALL_CLASSES_NAME),
                            changes);
        }
        return changes;
    }

    public void addAcceptMethod(TypeSpec host, ClassName visitorInterfaceName,
                    Map<TypeSpec, TypeSpec> changes) {
        TypeSpec currentHost = changes.get(host);
        if (currentHost == null) {
            currentHost = host;
        }
        TypeSpec.Builder modifyingBuilder = currentHost.toBuilder();
        logger.debug("adding accept(visitor) methods to {}", host.name);
        MethodSpec accept = MethodSpec.methodBuilder("accept")
                        .addModifiers(PUBLIC)
                        .addParameter(visitorInterfaceName, "visitor")
                        .addStatement("$N.visit(this)", "visitor")
                        .build();
        modifyingBuilder.addMethod(accept);
        changes.put(host, modifyingBuilder.build());
    }

    public void addImplementsHostInterface(TypeSpec host, ClassName hostInterfaceName,
                    Map<TypeSpec, TypeSpec> changes) {
        TypeSpec currentHost = changes.get(host);
        if (currentHost == null) {
            currentHost = host;
        }
        TypeSpec.Builder modifyingBuilder = currentHost.toBuilder();
        logger.debug("adding accept(visitor) methods to {}", host.name);
        modifyingBuilder.addSuperinterface(hostInterfaceName);
        changes.put(host, modifyingBuilder.build());
    }
}

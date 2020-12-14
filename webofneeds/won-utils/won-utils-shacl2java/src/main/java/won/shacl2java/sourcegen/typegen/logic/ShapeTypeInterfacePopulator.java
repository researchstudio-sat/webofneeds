package won.shacl2java.sourcegen.typegen.logic;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.shacl2java.Shacl2JavaConfig;
import won.shacl2java.sourcegen.typegen.mapping.ShapeTypeImplTypes;
import won.shacl2java.sourcegen.typegen.mapping.ShapeTypeInterfaceTypes;
import won.shacl2java.sourcegen.typegen.TypesPostprocessor;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

import static javax.lang.model.element.Modifier.DEFAULT;
import static javax.lang.model.element.Modifier.PUBLIC;

public class ShapeTypeInterfacePopulator implements TypesPostprocessor {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private ShapeTypeInterfaceTypes.Consumer shapeTypeInterfaceTypes;
    private ShapeTypeImplTypes.Consumer shapeTypeImplTypes;
    private Shacl2JavaConfig config;

    public ShapeTypeInterfacePopulator(ShapeTypeInterfaceTypes.Consumer shapeTypeInterfaceTypes,
                    ShapeTypeImplTypes.Consumer shapeTypeImplTypes, Shacl2JavaConfig config) {
        this.shapeTypeInterfaceTypes = shapeTypeInterfaceTypes;
        this.shapeTypeImplTypes = shapeTypeImplTypes;
        this.config = config;
    }

    @Override
    public Map<TypeSpec, TypeSpec> postprocess(Set<TypeSpec> allTypes) {
        if (!config.isInterfacesForRdfTypes()) {
            return Collections.emptyMap();
        }
        Map<TypeSpec, TypeSpec> modifiedInterfaces = new HashMap<>();
        for (URI interfaceClassUri : shapeTypeInterfaceTypes.keySet()) {
            TypeSpec interfaceType = shapeTypeInterfaceTypes.get(interfaceClassUri).get();
            TypeSpec.Builder modifyingBuilder = interfaceType.toBuilder();
            Map<String, Set<MethodSpec>> candidateInterfaceMethodsByType = new HashMap<>();
            if (shapeTypeImplTypes.containsKey(interfaceClassUri)) {
                Optional<Set<TypeSpec>> implementingTypes = shapeTypeImplTypes.get(interfaceClassUri);
                if (implementingTypes.isPresent()) {
                    for (TypeSpec type : implementingTypes.get()) {
                        // generate public abstract method with same signature and remember with type
                        // name
                        Set<MethodSpec> candidateInterfaceMethods = type.methodSpecs.stream()
                                        .filter(m -> m.hasModifier(PUBLIC))
                                        .filter(m -> !m.isConstructor())
                                        .filter(m -> !m.name.equals("get_node"))
                                        .filter(m -> !m.name.equals("set_node"))
                                        .filter(m -> !Arrays.stream(Object.class.getMethods())
                                                        .anyMatch(om -> om.getName().equals(m.name)))
                                        .map(methodSpec -> {
                                            MethodSpec.Builder b = MethodSpec.methodBuilder(methodSpec.name);
                                            b.returns(methodSpec.returnType);
                                            b.addTypeVariables(methodSpec.typeVariables);
                                            methodSpec.parameters.forEach(p -> b.addParameter(p));
                                            if (!methodSpec.returnType.equals(ClassName.VOID)) {
                                                if (methodSpec.returnType.isPrimitive()) {
                                                    b.addStatement("return ($T) null", methodSpec.returnType.box());
                                                } else {
                                                    b.addStatement("return null");
                                                }
                                            }
                                            b.addModifiers(PUBLIC, DEFAULT);
                                            return b.build();
                                        }).collect(Collectors.toSet());
                        candidateInterfaceMethodsByType.put(type.name, candidateInterfaceMethods);
                    }
                }
                Set<MethodSpec> commonMethodsForInterface = candidateInterfaceMethodsByType.values()
                                .stream()
                                .reduce(
                                                null,
                                                (left, right) -> {
                                                    if (left == null) {
                                                        return right;
                                                    } else if (right == null) {
                                                        return left;
                                                    }
                                                    HashSet<MethodSpec> intersection = new HashSet(left);
                                                    intersection.retainAll(right);
                                                    return intersection;
                                                });
                commonMethodsForInterface.forEach(m -> {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Adding interface method {} to interface {}", m.name,
                                        shapeTypeInterfaceTypes.get(interfaceClassUri).get().name);
                    }
                    modifyingBuilder.addMethod(m);
                });
            }
            modifiedInterfaces.put(interfaceType, modifyingBuilder.build());
        }
        return modifiedInterfaces;
    }
}

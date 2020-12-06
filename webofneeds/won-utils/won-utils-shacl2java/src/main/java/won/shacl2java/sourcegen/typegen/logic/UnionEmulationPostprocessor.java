package won.shacl2java.sourcegen.typegen.logic;

import com.squareup.javapoet.*;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.shacl2java.Shacl2JavaConfig;
import won.shacl2java.sourcegen.typegen.mapping.ShapeTypeImplTypes;
import won.shacl2java.sourcegen.typegen.mapping.ShapeTypeInterfaceTypes;
import won.shacl2java.sourcegen.typegen.TypesPostprocessor;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static javax.lang.model.element.Modifier.*;
import static won.shacl2java.util.CollectionUtils.addToMultivalueMap;

public class UnionEmulationPostprocessor implements TypesPostprocessor {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private ShapeTypeInterfaceTypes.Consumer shapeTypeInterfaceTypes;
    private ShapeTypeImplTypes.Consumer shapeTypeImplTypes;
    private Shacl2JavaConfig config;

    public UnionEmulationPostprocessor(Shacl2JavaConfig config,
                    ShapeTypeInterfaceTypes.Consumer shapeTypeInterfaceTypes,
                    ShapeTypeImplTypes.Consumer shapeTypeImplTypes) {
        this.shapeTypeInterfaceTypes = shapeTypeInterfaceTypes;
        this.shapeTypeImplTypes = shapeTypeImplTypes;
        this.config = config;
    }

    @Override
    public Map<TypeSpec, TypeSpec> postprocess(Set<TypeSpec> typeSpecs) {
        // flip uri-interface mapping so we can fetch efficiently when iterating over
        // all type specs
        Map<TypeSpec, Set<TypeSpec>> interfacesToImpls = new HashMap<>();
        for (URI classURI : shapeTypeInterfaceTypes.keySet()) {
            interfacesToImpls.put(shapeTypeInterfaceTypes.get(classURI).get(), shapeTypeImplTypes.get(classURI).get());
        }
        // Now, interfacesToImpls contains the interfaces and implementations we want to
        // modify.
        // 1. add the the when-method to the interface
        // 2. add the sub-interface with the is-methods to the interface
        // 3. add an implementation of the when-method to each implementing class
        Map<TypeSpec, TypeSpec> oldTypeToModifiedType = new HashMap<>();
        for (Map.Entry<TypeSpec, Set<TypeSpec>> ifAndImpls : interfacesToImpls.entrySet()) {
            TypeSpec interfaceType = ifAndImpls.getKey();
            Set<TypeSpec> implementingClasses = ifAndImpls.getValue();
            // if we've already modified these types, replace them by the modified versions
            TypeSpec currentInterfaceType = oldTypeToModifiedType.getOrDefault(interfaceType, interfaceType);
            Set<ImmutablePair<TypeSpec, TypeSpec>> currentImplementingClasses = implementingClasses.stream()
                            .map(c -> new ImmutablePair<TypeSpec, TypeSpec>(c,
                                            oldTypeToModifiedType.getOrDefault(c, c)))
                            .collect(Collectors.toSet());
            // modify interface
            TypeSpec modifiedInterfaceType = addUnionEmulationPatternMethodsToInterface(currentInterfaceType,
                            implementingClasses, config);
            oldTypeToModifiedType.put(interfaceType, modifiedInterfaceType);
            // modify each implementation
            for (ImmutablePair<TypeSpec, TypeSpec> impl : currentImplementingClasses) {
                TypeSpec modifiedImpl = addUnionEmulationPatternMethodsToImplementations(currentInterfaceType,
                                impl.getRight(),
                                config);
                oldTypeToModifiedType.put(impl.getLeft(), modifiedImpl);
            }
        }
        return oldTypeToModifiedType;
    }

    private TypeSpec addUnionEmulationPatternMethodsToImplementations(TypeSpec modifiedInterfaceType, TypeSpec impl,
                    Shacl2JavaConfig config) {
        TypeSpec.Builder implBuilder = impl.toBuilder();
        TypeVariableName R = TypeVariableName.get("R");
        ClassName subInterfaceName = ClassName.get(config.getPackageName(), modifiedInterfaceType.name, "Cases");
        TypeName casesType = ParameterizedTypeName.get(subInterfaceName, R);
        implBuilder.addMethod(
                        MethodSpec.methodBuilder("when")
                                        .addModifiers(PUBLIC)
                                        .addTypeVariable(R)
                                        .addParameter(casesType, "option")
                                        .returns(R)
                                        .addStatement("return option.is(this)")
                                        .build());
        return implBuilder.build();
    }

    private TypeSpec addUnionEmulationPatternMethodsToInterface(TypeSpec interfaceType,
                    Set<TypeSpec> implementingClasses, Shacl2JavaConfig config) {
        // 1. add the the when-method to the interface
        // 2. add the sub-interface with the is-methods to the interface
        TypeSpec.Builder interfaceBuilder = interfaceType.toBuilder();
        TypeVariableName R = TypeVariableName.get("R");
        ClassName subInterfaceName = ClassName.get(config.getPackageName(), interfaceType.name, "Cases");
        TypeName casesType = ParameterizedTypeName.get(subInterfaceName, R);
        interfaceBuilder.addMethod(
                        MethodSpec.methodBuilder("when")
                                        .addModifiers(PUBLIC, ABSTRACT)
                                        .addTypeVariable(R)
                                        .addParameter(casesType, "option")
                                        .returns(R)
                                        .build());
        TypeSpec.Builder subInterfaceBuilder = TypeSpec.interfaceBuilder("Cases")
                        .addModifiers(PUBLIC, STATIC)
                        .addTypeVariable(R);
        for (TypeSpec impl : implementingClasses) {
            subInterfaceBuilder.addMethod(
                            MethodSpec.methodBuilder("is")
                                            .addModifiers(DEFAULT, PUBLIC)
                                            .addParameter(ClassName.get(config.getPackageName(), impl.name), "option")
                                            .addStatement("return null")
                                            .returns(R)
                                            .build());
        }
        interfaceBuilder.addType(subInterfaceBuilder.build());
        return interfaceBuilder.build();
    }
}

package won.shacl2java.sourcegen.typegen.logic;

import com.squareup.javapoet.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.shacl2java.Shacl2JavaConfig;
import won.shacl2java.sourcegen.typegen.TypesGenerator;
import won.shacl2java.sourcegen.typegen.mapping.VisitorClassTypeSpecs;
import won.shacl2java.util.NameUtils;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static javax.lang.model.element.Modifier.*;
import static won.shacl2java.util.NameUtils.getterNameForField;

public class VisitorImplGenerator implements TypesGenerator {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private Shacl2JavaConfig config;
    VisitorClassTypeSpecs.Consumer visitorClassTypeSpecs;

    public VisitorImplGenerator(Shacl2JavaConfig config, VisitorClassTypeSpecs.Consumer visitorClassTypeSpecs) {
        this.config = config;
        this.visitorClassTypeSpecs = visitorClassTypeSpecs;
    }

    @Override
    public Set<TypeSpec> generate() {
        logger.debug("generating default implementations for visitor classes {}",
                        visitorClassTypeSpecs.keySet().stream()
                                        .map(URI::toString)
                                        .collect(Collectors.joining(",", "[", "]")));
        Set<TypeSpec> newTypeSpecs = new HashSet<>();
        for (URI visitorClass : config.getVisitorClasses()) {
            String visitorName = NameUtils.classNameForShapeURI(visitorClass, config) + config.getVisitorSuffix();
            String hostInterfaceName = NameUtils.classNameForShapeURI(visitorClass, config);
            ClassName hostInterfaceClassName = ClassName.get(config.getPackageName(), hostInterfaceName);
            if (visitorClassTypeSpecs.containsKey(visitorClass)) {
                // generate default visitor implementation
                String visitorImplName = "Default" + visitorName;
                TypeSpec.Builder vImplBuilder = TypeSpec
                                .classBuilder(visitorImplName)
                                .addModifiers(PUBLIC);
                vImplBuilder.addSuperinterface(ClassName.get(config.getPackageName(), visitorName));
                vImplBuilder.addMethod(MethodSpec.methodBuilder("onBeforeRecursion")
                                .addModifiers(PROTECTED)
                                .addParameter(hostInterfaceClassName, "host")
                                .addParameter(hostInterfaceClassName, "child")
                                .build());
                vImplBuilder.addMethod(MethodSpec.methodBuilder("onAfterRecursion")
                                .addModifiers(PROTECTED)
                                .addParameter(hostInterfaceClassName, "host")
                                .addParameter(hostInterfaceClassName, "child")
                                .build());
                Set<String> visitedTypeNames = visitorClassTypeSpecs.get(visitorClass).get().stream()
                                .map(spec -> spec.name)
                                .collect(Collectors.toSet());
                for (TypeSpec typeSpec : visitorClassTypeSpecs.get(visitorClass).get()) {
                    logger.debug("proessing type {} as part of visitor class {} for generating default implementation",
                                    typeSpec.name, visitorClass);
                    ClassName childClassName = ClassName.get(config.getPackageName(), typeSpec.name);
                    vImplBuilder.addMethod(MethodSpec.methodBuilder("onBeginVisit")
                                    .addModifiers(PROTECTED)
                                    .addParameter(childClassName, "host")
                                    .build());
                    vImplBuilder.addMethod(MethodSpec.methodBuilder("onEndVisit")
                                    .addModifiers(PROTECTED)
                                    .addParameter(childClassName, "host")
                                    .build());
                    MethodSpec.Builder visitRecursivelyBuilder = MethodSpec.methodBuilder("visit")
                                    .addModifiers(PUBLIC, FINAL)
                                    .addParameter(childClassName, "host");
                    visitRecursivelyBuilder.addStatement("onBeginVisit(host)");
                    for (FieldSpec subElement : typeSpec.fieldSpecs) {
                        // let's allow the visitor to visit the subelement, too.
                        TypeName _subElTyp = subElement.type;
                        TypeName rawSubElementType = null;
                        if (subElement.type instanceof ParameterizedTypeName) {
                            _subElTyp = ((ParameterizedTypeName) subElement.type).typeArguments.get(0);
                            rawSubElementType = ((ParameterizedTypeName) subElement.type).rawType;
                        }
                        final TypeName subElementType = _subElTyp;
                        if (visitedTypeNames.contains(((ClassName) subElementType).simpleName())) {
                            if (subElement.type instanceof ParameterizedTypeName) {
                                logger.debug("adding recursion for field {}", subElement.name);
                                String getter = getterNameForField(subElement.name);
                                if (rawSubElementType != null && rawSubElementType.equals(TypeName.get(Set.class))) {
                                    visitRecursivelyBuilder
                                                    .beginControlFlow("host.$N().forEach(child -> ", getter)
                                                    .addStatement("onBeforeRecursion(host, child)")
                                                    .addStatement("child.accept(this)")
                                                    .addStatement("onAfterRecursion(host, child)")
                                                    .endControlFlow(")");
                                } else {
                                    visitRecursivelyBuilder
                                                    .addStatement("$N child = host.$N()", subElementType, getter)
                                                    .beginControlFlow("if (child != null)")
                                                    .addStatement("onBeforeRecursion(host, child)")
                                                    .addStatement("child.accept(visitor, depthFirst)")
                                                    .addStatement("onAfterRecursion(host, child)")
                                                    .endControlFlow();
                                }
                            }
                        }
                    }
                    visitRecursivelyBuilder.addStatement("onEndVisit(host)");
                    vImplBuilder.addMethod(visitRecursivelyBuilder.build());
                }
                newTypeSpecs.add(vImplBuilder.build());
            }
        }
        return newTypeSpecs;
    }
}

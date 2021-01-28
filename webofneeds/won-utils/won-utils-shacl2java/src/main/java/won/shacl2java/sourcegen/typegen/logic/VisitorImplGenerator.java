package won.shacl2java.sourcegen.typegen.logic;

import com.squareup.javapoet.*;
import org.apache.jena.graph.Graph;
import org.apache.jena.sparql.graph.GraphFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.shacl2java.Shacl2JavaConfig;
import won.shacl2java.runtime.model.GraphEntity;
import won.shacl2java.runtime.model.GraphEntityVisitor;
import won.shacl2java.sourcegen.typegen.TypesGenerator;
import won.shacl2java.sourcegen.typegen.mapping.ShapeTypeSpecs;
import won.shacl2java.sourcegen.typegen.mapping.VisitorClassTypeSpecs;
import won.shacl2java.util.NameUtils;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static javax.lang.model.element.Modifier.*;
import static won.shacl2java.util.NameUtils.getterNameForField;

public class VisitorImplGenerator implements TypesGenerator {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    VisitorClassTypeSpecs.Consumer visitorClassTypeSpecs;
    ShapeTypeSpecs.Consumer shapeTypeSpecs;
    private Shacl2JavaConfig config;

    public VisitorImplGenerator(Shacl2JavaConfig config, VisitorClassTypeSpecs.Consumer visitorClassTypeSpecs,
                    ShapeTypeSpecs.Consumer shapeTypeSpecs) {
        this.config = config;
        this.visitorClassTypeSpecs = visitorClassTypeSpecs;
        this.shapeTypeSpecs = shapeTypeSpecs;
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
            Optional<Set<TypeSpec>> hostClassesOpt = visitorClassTypeSpecs.get(visitorClass);
            if (hostClassesOpt.isPresent()) {
                Set<TypeSpec> hostTypeSpecs = hostClassesOpt.get();
                TypeSpec visitorImpl = generateVisitorDefaultImplementation(
                                ClassName.get(config.getPackageName(), visitorName), hostInterfaceClassName,
                                hostTypeSpecs);
                newTypeSpecs.add(visitorImpl);
            }
        }
        generateVisitorsForAllClasses(newTypeSpecs);
        return newTypeSpecs;
    }

    public void generateVisitorsForAllClasses(Set<TypeSpec> newTypeSpecs) {
        TypeSpec visitorImpl = generateVisitorDefaultImplementation(
                        ClassName.get(config.getPackageName(), config.DEFAULT_VISITOR_FOR_ALL_CLASSES_NAME),
                        ClassName.OBJECT,
                        shapeTypeSpecs.values());
        newTypeSpecs.add(visitorImpl);
        visitorImpl = generateGraphEntityVisitorEngine(
                        ClassName.get(config.getPackageName(), config.DEFAULT_VISITOR_FOR_ALL_CLASSES_NAME),
                        shapeTypeSpecs.values());
        newTypeSpecs.add(visitorImpl);
        generateRdfOutput(newTypeSpecs);
    }

    public void generateRdfOutput(Set<TypeSpec> newTypeSpecs) {
        ClassName engineClassName = ClassName.get(config.getPackageName(), "GraphEntityVisitorEngine");
        TypeSpec rdfOutput = TypeSpec.classBuilder(ClassName.get(config.getPackageName(), "RdfOutput"))
                        .addModifiers(PUBLIC)
                        .addMethod(MethodSpec.methodBuilder("toGraph")
                                        .addModifiers(PUBLIC, STATIC)
                                        .returns(ClassName.get(Graph.class))
                                        .addParameter(ClassName.get(config.getPackageName(),
                                                        config.DEFAULT_VISITOR_HOST_FOR_ALL_CLASSES_NAME),
                                                        "startingNode")
                                        .addStatement("Graph graph = $T.createGraphMem()", GraphFactory.class)
                                        .addStatement("populateGraph(startingNode, graph)")
                                        .addStatement("return graph")
                                        .build())
                        .addMethod(MethodSpec.methodBuilder("populateGraph")
                                        .addModifiers(PUBLIC, STATIC)
                                        .addParameter(ClassName.get(config.getPackageName(),
                                                        config.DEFAULT_VISITOR_HOST_FOR_ALL_CLASSES_NAME),
                                                        "startingNode")
                                        .addParameter(ClassName.get(Graph.class), "graph")
                                        .addStatement("$T graphEntityVisitor = $L",
                                                        GraphEntityVisitor.class,
                                                        TypeSpec.anonymousClassBuilder("")
                                                                        .addSuperinterface(GraphEntityVisitor.class)
                                                                        .addMethod(MethodSpec.methodBuilder("visit")
                                                                                        .addModifiers(PUBLIC)
                                                                                        .addParameter(GraphEntity.class,
                                                                                                        "graphEntity")
                                                                                        .addStatement("graphEntity.toRdf ( triple -> graph.add(triple) )")
                                                                                        .build())
                                                                        .build())
                                        .addStatement("startingNode.accept(new $T(graphEntityVisitor))",
                                                        engineClassName)
                                        .build())
                        .build();
        newTypeSpecs.add(rdfOutput);
    }

    public TypeSpec generateVisitorDefaultImplementation(ClassName visitorInterface,
                    ClassName hostInterfaceClassName, Collection<TypeSpec> hostTypeSpecs) {
        // generate default visitor implementation
        String visitorImplName = "Default" + visitorInterface.simpleName();
        TypeSpec.Builder vImplBuilder = TypeSpec
                        .classBuilder(visitorImplName)
                        .addModifiers(PUBLIC);
        vImplBuilder.addSuperinterface(visitorInterface);
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
        Set<String> visitedTypeNames = hostTypeSpecs.stream()
                        .map(spec -> spec.name)
                        .collect(Collectors.toSet());
        for (TypeSpec typeSpec : hostTypeSpecs) {
            logger.debug("proessing type {} as part of visitor class {} for generating default implementation",
                            typeSpec.name, visitorImplName);
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
            for (FieldSpec fieldSpec : typeSpec.fieldSpecs) {
                // let's allow the visitor to visit the subelement, too.
                TypeName fieldType = fieldSpec.type;
                TypeName rawFieldType = null;
                if (fieldSpec.type instanceof ParameterizedTypeName) {
                    fieldType = ((ParameterizedTypeName) fieldSpec.type).typeArguments.get(0);
                    rawFieldType = ((ParameterizedTypeName) fieldSpec.type).rawType;
                }
                if (visitedTypeNames.contains(((ClassName) fieldType).simpleName())) {
                    String getter = getterNameForField(fieldSpec.name);
                    if (fieldSpec.type instanceof ParameterizedTypeName
                                    && rawFieldType != null
                                    && rawFieldType.equals(TypeName.get(Set.class))) {
                        logger.debug("adding recursion for field {}", fieldSpec.name);
                        visitRecursivelyBuilder
                                        .beginControlFlow("host.$N().forEach(child -> ", getter)
                                        .addStatement("onBeforeRecursion(host, child)")
                                        .addStatement("child.accept(this)")
                                        .addStatement("onAfterRecursion(host, child)")
                                        .endControlFlow(")");
                    } else {
                        visitRecursivelyBuilder
                                        .beginControlFlow("")
                                        .addStatement("$T child = host.$N()", fieldType, getter)
                                        .beginControlFlow("if (child != null)")
                                        .addStatement("onBeforeRecursion(host, child)")
                                        .addStatement("child.accept(this)")
                                        .addStatement("onAfterRecursion(host, child)")
                                        .endControlFlow()
                                        .endControlFlow();
                    }
                }
            }
            visitRecursivelyBuilder.addStatement("onEndVisit(host)");
            vImplBuilder.addMethod(visitRecursivelyBuilder.build());
        }
        TypeSpec visitorImpl = vImplBuilder.build();
        return visitorImpl;
    }

    public TypeSpec generateGraphEntityVisitorEngine(
                    ClassName visitorInterface,
                    Collection<TypeSpec> hostTypeSpecs) {
        // generate default visitor implementation
        String visitorImplName = "GraphEntityVisitorEngine";
        TypeSpec.Builder vImplBuilder = TypeSpec
                        .classBuilder(visitorImplName)
                        .addModifiers(PUBLIC);
        vImplBuilder.addSuperinterface(visitorInterface);
        vImplBuilder.addField(
                        FieldSpec.builder(ClassName.get(GraphEntityVisitor.class), "graphEntityVisitor", PRIVATE)
                                        .build());
        vImplBuilder.addMethod(MethodSpec.constructorBuilder()
                        .addModifiers(PUBLIC)
                        .addParameter(ClassName.get(GraphEntityVisitor.class), "graphEntityVisitor")
                        .addStatement("this.graphEntityVisitor = graphEntityVisitor")
                        .build());
        Set<String> visitedTypeNames = hostTypeSpecs.stream()
                        .map(spec -> spec.name)
                        .collect(Collectors.toSet());
        for (TypeSpec typeSpec : hostTypeSpecs) {
            logger.debug("proessing type {} as part of visitor class {} for generating default implementation",
                            typeSpec.name, visitorImplName);
            ClassName childClassName = ClassName.get(config.getPackageName(), typeSpec.name);
            MethodSpec.Builder visitRecursivelyBuilder = MethodSpec.methodBuilder("visit")
                            .addModifiers(PUBLIC, FINAL)
                            .addParameter(childClassName, "host");
            if (!typeSpec.superclass.equals(ClassName.get(GraphEntity.class))) {
                // add an empty implementation for host classes that are
                // not subclasses of GraphEntity
                vImplBuilder.addMethod(visitRecursivelyBuilder.build());
                continue;
            }
            visitRecursivelyBuilder.addStatement("host.accept(this.graphEntityVisitor)");
            for (FieldSpec fieldSpec : typeSpec.fieldSpecs) {
                // let's allow the visitor to visit the subelement, too.
                TypeName fieldType = fieldSpec.type;
                TypeName rawFieldType = null;
                if (fieldType instanceof ParameterizedTypeName) {
                    fieldType = ((ParameterizedTypeName) fieldType).typeArguments.get(0);
                    rawFieldType = ((ParameterizedTypeName) fieldSpec.type).rawType;
                }
                if (visitedTypeNames.contains(((ClassName) fieldType).simpleName())) {
                    logger.debug("adding recursion for field {}", fieldSpec.name);
                    String getter = getterNameForField(fieldSpec.name);
                    if (fieldSpec.type instanceof ParameterizedTypeName
                                    && rawFieldType != null
                                    && rawFieldType.equals(TypeName.get(Set.class))) {
                        visitRecursivelyBuilder
                                        .beginControlFlow("host.$N().forEach(child -> ", getter)
                                        .addStatement("child.accept(this)")
                                        .endControlFlow(")");
                    } else {
                        visitRecursivelyBuilder
                                        .beginControlFlow("")
                                        .addStatement("$T child = host.$N()", fieldType, getter)
                                        .beginControlFlow("if (child != null)")
                                        .addStatement("child.accept(this)")
                                        .endControlFlow()
                                        .endControlFlow();
                    }
                }
            }
            vImplBuilder.addMethod(visitRecursivelyBuilder.build());
        }
        TypeSpec visitorImpl = vImplBuilder.build();
        return visitorImpl;
    }
}

package won.shacl2java.sourcegen.typegen.logic;

import com.squareup.javapoet.*;
import org.apache.jena.datatypes.TypeMapper;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.graph.impl.LiteralLabelFactory;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.riot.other.G;
import org.apache.jena.shacl.Shapes;
import org.apache.jena.shacl.engine.constraint.*;
import org.apache.jena.shacl.parser.Shape;
import org.apache.jena.shacl.vocabulary.SHACL;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.shacl2java.Shacl2JavaConfig;
import won.shacl2java.annotation.ShapeNode;
import won.shacl2java.constraints.EnumShapeChecker;
import won.shacl2java.runtime.model.GraphEntity;
import won.shacl2java.sourcegen.typegen.TypesGenerator;
import won.shacl2java.sourcegen.typegen.mapping.*;
import won.shacl2java.sourcegen.typegen.support.NameClashDetector;
import won.shacl2java.util.NameUtils;
import won.shacl2java.util.ShapeUtils;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static javax.lang.model.element.Modifier.*;

public class MainTypesGenerator implements TypesGenerator {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private Shapes shapes;
    private Shacl2JavaConfig config;
    private NameClashDetector nameClashDetector;
    private IndividualClassNames.Consumer individualClassNames;
    private ShapeTypeSpecs.Producer shapeTypeSpecs;
    private VisitorClassTypeSpecs.Producer visitorClassTypeSpecs;
    private TypeSpecNames.Producer typeSpecNames;
    private ShapeTargetClasses.Producer shapeTargetClasses;
    private BuildableClassNames.Producer buildableClassNames;

    public MainTypesGenerator(Shapes shapes, Shacl2JavaConfig config,
                    NameClashDetector nameClashDetector,
                    IndividualClassNames.Consumer individualClassNames,
                    ShapeTypeSpecs.Producer shapeTypeSpecs,
                    VisitorClassTypeSpecs.Producer visitorClassTypeSpecs,
                    TypeSpecNames.Producer typeSpecNames, ShapeTargetClasses.Producer producer,
                    BuildableClassNames.Producer buildableClassNames) {
        this.shapes = shapes;
        this.config = config;
        this.nameClashDetector = nameClashDetector;
        this.individualClassNames = individualClassNames;
        this.shapeTypeSpecs = shapeTypeSpecs;
        this.visitorClassTypeSpecs = visitorClassTypeSpecs;
        this.typeSpecNames = typeSpecNames;
        this.shapeTargetClasses = producer;
        this.buildableClassNames = buildableClassNames;
    }

    private static boolean isAnonymousShape(Shape shape) {
        return shape.getShapeNode().isBlank();
    }

    private static String getJavadocGeneratedForShape(Shape shape) {
        Node shapeNode = shape.getShapeNode();
        if (shapeNode.isBlank()) {
            return "Generated for an anonymous shape";
        }
        String uri = shapeNode.getURI();
        return String.format("Generated for shape <a href=\"%s\">%s</a>", uri, uri);
    }

    @Override
    public Set<TypeSpec> generate() {
        return shapes.getShapeMap().values().stream()
                        .filter(s -> s.isNodeShape() || looksLikeANodeShape(s))
                        .distinct()
                        .map(shape -> {
                            if (isAnonymousShape(shape)) {
                                return null;
                            }
                            logger.debug("Generating class for shape {}", shape.getShapeNode().getURI());
                            EnumShapeChecker enumShapeChecker = ShapeUtils.checkForEnumShape(shape);
                            TypeSpec spec = null;
                            if (enumShapeChecker.isEnumShape()) {
                                spec = generateEnum(config, shape, enumShapeChecker);
                            } else {
                                spec = generateClass(shape, shapes, config);
                                buildableClassNames.put(shape.getShapeNode(),
                                                ClassName.get(config.getPackageName(), spec.name));
                            }
                            typeSpecNames.put(ClassName.get(config.getPackageName(), spec.name), spec);
                            shapeTypeSpecs.put(shape, spec);
                            return spec;
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
    }

    private boolean looksLikeANodeShape(Shape s) {
        if (s.getConstraints().isEmpty()) {
            return false;
        }
        return s.getConstraints().stream().anyMatch(
                        c -> c instanceof ShXone
                                        || c instanceof ShOr
                                        || c instanceof ShAnd
                                        || c instanceof ShNot
                                        || c instanceof ClosedConstraint);
    }

    public TypeSpec generateClass(Shape shape, Shapes shapes, Shacl2JavaConfig config) {
        String name = NameUtils.classNameForShape(shape, config);
        nameClashDetector.detectNameClash(shape, name);
        TypeSpec.Builder typeBuilder = TypeSpec.classBuilder(name)
                        .superclass(ClassName.get(GraphEntity.class))
                        .addModifiers(PUBLIC);
        typeBuilder.addJavadoc(getJavadocGeneratedForShape(shape));
        typeBuilder.addAnnotation(
                        AnnotationSpec.builder(ShapeNode.class)
                                        .addMember("value",
                                                        "{ $S }",
                                                        shape.getShapeNode().getURI())
                                        .build());
        typeBuilder.addMethod(
                        MethodSpec.constructorBuilder()
                                        .addParameter(ClassName.get(Node.class), "node")
                                        .addParameter(ClassName.get(Graph.class), "graph")
                                        .addModifiers(PUBLIC)
                                        .addStatement("super(node,graph)")
                                        .build())
                        .addMethod(
                                        MethodSpec.constructorBuilder()
                                                        .addParameter(ClassName.get(String.class), "uri")
                                                        .addParameter(ClassName.get(Graph.class), "graph")
                                                        .addModifiers(PUBLIC)
                                                        .addStatement("super(uri,graph)")
                                                        .build())
                        .addMethod(
                                        MethodSpec.constructorBuilder()
                                                        .addParameter(ClassName.get(URI.class), "uri")
                                                        .addParameter(ClassName.get(Graph.class), "graph")
                                                        .addModifiers(PUBLIC)
                                                        .addStatement("super(uri,graph)")
                                                        .build())
                        .addMethod(
                                        MethodSpec.constructorBuilder()
                                                        .addParameter(ClassName.get(Node.class), "node")
                                                        .addModifiers(PUBLIC)
                                                        .addStatement("super(node)")
                                                        .build())
                        .addMethod(
                                        MethodSpec.constructorBuilder()
                                                        .addParameter(ClassName.get(String.class), "uri")
                                                        .addModifiers(PUBLIC)
                                                        .addStatement("super(uri)")
                                                        .build())
                        .addMethod(
                                        MethodSpec.constructorBuilder()
                                                        .addParameter(ClassName.get(URI.class), "uri")
                                                        .addModifiers(PUBLIC)
                                                        .addStatement("super(uri)")
                                                        .build())
                        .addMethod(
                                        MethodSpec.constructorBuilder()
                                                        .addModifiers(PUBLIC)
                                                        .build());
        TypeSpec typeSpec = typeBuilder.build();
        // remember type for visitor pattern generation
        shape.getShapeGraph()
                        .find(shape.getShapeNode(), RDF.type.asNode(), null)
                        .toSet().stream()
                        .map(Triple::getObject)
                        .map(Node::getURI)
                        .map(URI::create)
                        .filter(u -> config.getVisitorClasses().contains(u))
                        .forEach(visitorClassUri -> visitorClassTypeSpecs.add(visitorClassUri, typeSpec));
        // remember if this shape uses sh:targetClass so we can map property shapes with
        // sh:class to its java type
        G.allSP(shapes.getGraph(), shape.getShapeNode(), SHACL.targetClass).forEach(targetClass -> {
            shapeTargetClasses.put(targetClass, shape);
        });
        return typeSpec;
    }

    public TypeSpec generateEnum(Shacl2JavaConfig config, Shape shape,
                    EnumShapeChecker enumShapeChecker) {
        ClassName name = NameUtils.javaPoetClassNameForShape(shape, config);
        nameClashDetector.detectNameClash(shape, name.simpleName());
        TypeSpec.Builder typeBuilder = TypeSpec.enumBuilder(name).addModifiers(PUBLIC);
        typeBuilder.addJavadoc(getJavadocGeneratedForShape(shape));
        typeBuilder.addAnnotation(
                        AnnotationSpec.builder(ShapeNode.class)
                                        .addMember("value",
                                                        "{ $S }",
                                                        shape.getShapeNode().getURI())
                                        .build());
        ClassName valueClassName = ClassName.OBJECT;
        List<Object> convertedValues = new ArrayList<>();
        String argumentsFormat = null;
        Function<Object, Object[]> argMapper = null;
        if (SHACL.IRI.equals(enumShapeChecker.getNodeKind())) {
            valueClassName = ClassName.get(URI.class);
            convertedValues = enumShapeChecker.getValues().stream().map(n -> URI.create(n.getURI()))
                            .collect(Collectors.toList());
            argumentsFormat = "URI.create($S)";
            argMapper = val -> new Object[] { val.toString() };
        } else if (SHACL.Literal.equals(enumShapeChecker.getNodeKind())) {
            valueClassName = ClassName.get(Literal.class);
            convertedValues = enumShapeChecker.getValues().stream().map(n -> n.getLiteral())
                            .collect(Collectors.toList());
            Literal lit = null;
            LiteralLabelFactory.create(lit.getLexicalForm(),
                            TypeMapper.getInstance().getTypeByName(lit.getDatatypeURI()));
            argumentsFormat = "$T.create($S, $T.getInstance().getTypeByName($S))";
            argMapper = val -> new Object[] {
                            LiteralLabelFactory.class,
                            ((Literal) val).getLexicalForm(),
                            TypeMapper.class,
                            ((Literal) val).getDatatypeURI()
            };
        } else if (enumShapeChecker.getValues().stream().allMatch(n -> n.isURI())) {
            valueClassName = ClassName.get(URI.class);
            convertedValues = enumShapeChecker.getValues().stream().map(n -> URI.create(n.getURI()))
                            .collect(Collectors.toList());
            argumentsFormat = "URI.create($S)";
            argMapper = val -> new Object[] { val.toString() };
        } else if (enumShapeChecker.getValues().stream().allMatch(n -> n.isLiteral())) {
            valueClassName = ClassName.get(Literal.class);
            convertedValues = enumShapeChecker.getValues().stream().map(n -> n.getLiteral())
                            .collect(Collectors.toList());
            argumentsFormat = "$T.create($S, $T.getInstance().getTypeByName($S))";
            argMapper = val -> new Object[] {
                            LiteralLabelFactory.class,
                            ((Literal) val).getLexicalForm(),
                            TypeMapper.class,
                            ((Literal) val).getDatatypeURI()
            };
        } else {
            return null;
        }
        typeBuilder.addField(
                        FieldSpec.builder(valueClassName, "value", PRIVATE).build());
        typeBuilder.addMethod(
                        MethodSpec.constructorBuilder()
                                        .addParameter(valueClassName, "value")
                                        .addStatement("this.$N = $N", "value", "value")
                                        .build());
        typeBuilder.addMethod(
                        MethodSpec.methodBuilder("getValue")
                                        .returns(valueClassName)
                                        .addStatement("return $N", "value")
                                        .addModifiers(PUBLIC).build());
        typeBuilder.addMethod(
                        MethodSpec.methodBuilder("forValue")
                                        .addModifiers(PUBLIC, STATIC)
                                        .addParameter(valueClassName, "value")
                                        .returns(name)
                                        .addStatement("return $T.stream(values()).filter(s -> s.getValue().equals(value)).findFirst().orElseThrow(() -> new $T($S + value))",
                                                        Arrays.class, IllegalArgumentException.class,
                                                        "Unsupported value: ")
                                        .build());
        for (Object value : convertedValues) {
            typeBuilder.addEnumConstant(NameUtils.enumConstantName(value),
                            TypeSpec.anonymousClassBuilder(argumentsFormat, argMapper.apply(value)).build());
        }
        return typeBuilder.build();
    }
}
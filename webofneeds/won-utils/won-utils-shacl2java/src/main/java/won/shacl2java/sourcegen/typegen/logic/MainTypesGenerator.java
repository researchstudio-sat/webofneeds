package won.shacl2java.sourcegen.typegen.logic;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.riot.other.G;
import org.apache.jena.shacl.Shapes;
import org.apache.jena.shacl.parser.Shape;
import org.apache.jena.shacl.vocabulary.SHACL;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.shacl2java.Shacl2JavaConfig;
import won.shacl2java.annotation.ShapeNode;
import won.shacl2java.constraints.EnumShapeChecker;
import won.shacl2java.model.GraphEntity;
import won.shacl2java.sourcegen.typegen.TypesGenerator;
import won.shacl2java.sourcegen.typegen.mapping.IndividualClassNames;
import won.shacl2java.sourcegen.typegen.mapping.ShapeTargetClasses;
import won.shacl2java.sourcegen.typegen.mapping.ShapeTypeSpecs;
import won.shacl2java.sourcegen.typegen.mapping.TypeSpecNames;
import won.shacl2java.sourcegen.typegen.mapping.VisitorClassTypeSpecs;
import won.shacl2java.sourcegen.typegen.support.NameClashDetector;
import won.shacl2java.util.NameUtils;
import won.shacl2java.util.ShapeUtils;

import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;

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

    public MainTypesGenerator(Shapes shapes, Shacl2JavaConfig config,
                    NameClashDetector nameClashDetector,
                    IndividualClassNames.Consumer individualClassNames,
                    ShapeTypeSpecs.Producer shapeTypeSpecs,
                    VisitorClassTypeSpecs.Producer visitorClassTypeSpecs,
                    TypeSpecNames.Producer typeSpecNames, ShapeTargetClasses.Producer producer) {
        this.shapes = shapes;
        this.config = config;
        this.nameClashDetector = nameClashDetector;
        this.individualClassNames = individualClassNames;
        this.shapeTypeSpecs = shapeTypeSpecs;
        this.visitorClassTypeSpecs = visitorClassTypeSpecs;
        this.typeSpecNames = typeSpecNames;
        this.shapeTargetClasses = producer;
    }

    @Override
    public Set<TypeSpec> generate() {
        return StreamSupport
                        .stream(Spliterators.spliteratorUnknownSize(shapes.iteratorAll(), Spliterator.ORDERED),
                                        false)
                        .distinct()
                        .filter(s -> s.isNodeShape())
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
                            }
                            typeSpecNames.put(ClassName.get(config.getPackageName(), spec.name), spec);
                            shapeTypeSpecs.put(shape, spec);
                            return spec;
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
    }

    private static boolean isAnonymousShape(Shape shape) {
        return shape.getShapeNode().isBlank();
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
        if (SHACL.IRI.equals(enumShapeChecker.getNodeKind())) {
            valueClassName = ClassName.get(String.class);
            convertedValues = enumShapeChecker.getValues().stream().map(n -> n.getURI())
                            .collect(Collectors.toList());
        } else if (SHACL.Literal.equals(enumShapeChecker.getNodeKind())) {
            valueClassName = ClassName.get(Literal.class);
            convertedValues = enumShapeChecker.getValues().stream().map(n -> n.getLiteral())
                            .collect(Collectors.toList());
        } else if (enumShapeChecker.getValues().stream().allMatch(n -> n.isURI())) {
            valueClassName = ClassName.get(String.class);
            convertedValues = enumShapeChecker.getValues().stream().map(n -> n.getURI())
                            .collect(Collectors.toList());
        } else if (enumShapeChecker.getValues().stream().allMatch(n -> n.isLiteral())) {
            valueClassName = ClassName.get(Literal.class);
            convertedValues = enumShapeChecker.getValues().stream().map(n -> n.getLiteral())
                            .collect(Collectors.toList());
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
        for (Object value : convertedValues) {
            typeBuilder.addEnumConstant(NameUtils.enumConstantName(value),
                            TypeSpec.anonymousClassBuilder("$S", value).build());
        }
        return typeBuilder.build();
    }

    private static String getJavadocGeneratedForShape(Shape shape) {
        Node shapeNode = shape.getShapeNode();
        if (shapeNode.isBlank()) {
            return "Generated for an anonymous shape";
        }
        String uri = shapeNode.getURI();
        return String.format("Generated for shape <a href=\"%s\">%s</a>", uri, uri);
    }
}
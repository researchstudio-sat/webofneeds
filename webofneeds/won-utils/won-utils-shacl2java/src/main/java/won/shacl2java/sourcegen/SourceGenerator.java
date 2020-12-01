package won.shacl2java.sourcegen;

import com.squareup.javapoet.*;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Node_Blank;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.shacl.Shapes;
import org.apache.jena.shacl.parser.PropertyShape;
import org.apache.jena.shacl.parser.Shape;
import org.apache.jena.shacl.vocabulary.SHACL;
import org.apache.jena.sparql.path.Path;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.shacl2java.Shacl2JavaConfig;
import won.shacl2java.annotation.Individual;
import won.shacl2java.annotation.Individuals;
import won.shacl2java.annotation.PropertyPath;
import won.shacl2java.annotation.ShapeNode;
import won.shacl2java.constraints.EnumShapeChecker;
import won.shacl2java.util.CollectionUtils;
import won.shacl2java.util.NameUtils;
import won.shacl2java.util.ShapeUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static javax.lang.model.element.Modifier.*;
import static org.apache.jena.shacl.validation.VLib.focusNodes;
import static won.shacl2java.util.CollectionUtils.addToMultivalueMap;
import static won.shacl2java.util.NameUtils.*;

public class SourceGenerator {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private Map<URI, Set<TypeSpec>> visitorClassToTypeSpec = new HashMap<>();
    private Map<URI, TypeSpec> interfacesForRdfTypes = new HashMap();
    private Map<URI, Set<String>> rdfTypeInterfaceImplNames = new HashMap();
    private Map<Node, ClassName> individualNodeToClassName = new HashMap<>();

    public SourceGenerator() {
    }

    public SourceGeneratorStats generate(File shapesFile, Shacl2JavaConfig config) throws IOException {
        long start = System.currentTimeMillis();
        Shapes shapes = readShapes(shapesFile);
        Duration readShapesDuration = Duration.of(System.currentTimeMillis() - start, ChronoUnit.MILLIS);
        start = System.currentTimeMillis();
        List<TypeSpec> typeSpecs = generateTypes(shapes, config);
        Duration generationDuration = Duration.of(System.currentTimeMillis() - start, ChronoUnit.MILLIS);
        start = System.currentTimeMillis();
        SourceGenerator.writeClasses(typeSpecs, config);
        Duration writeDuration = Duration.of(System.currentTimeMillis() - start, ChronoUnit.MILLIS);
        SourceGeneratorStats stats = new SourceGeneratorStats();
        stats.setShapesFile(shapesFile.toString());
        stats.setNumClasses(typeSpecs.size());
        stats.setOutputDir(config.getOutputDir());
        stats.setReadShapesDuration(readShapesDuration);
        stats.setGenerationDuration(generationDuration);
        stats.setWriteDuration(writeDuration);
        return stats;
    }

    public static Shapes readShapes(File shapesFile) throws FileNotFoundException {
        Model shapesGraph = ModelFactory.createDefaultModel();
        RDFDataMgr.read(shapesGraph, new FileInputStream(shapesFile), Lang.TTL);
        return Shapes.parse(shapesGraph.getGraph());
    }

    public List<TypeSpec> generateTypes(Shapes shapes, Shacl2JavaConfig config) {
        List<TypeSpec> typeSpecs = new ArrayList<>();
        typeSpecs.add(generateIndividuals(shapes, config));
        typeSpecs.addAll(StreamSupport
                        .stream(Spliterators.spliteratorUnknownSize(shapes.iteratorAll(), Spliterator.ORDERED), false)
                        .distinct()
                        .map(shape -> {
                            if (isAnonymousShape(shape)) {
                                return null;
                            }
                            logger.debug("Generating class for shape {}", shape.getShapeNode().getURI());
                            EnumShapeChecker enumShapeChecker = ShapeUtils.checkForEnumShape(shape);
                            if (enumShapeChecker.isEnumShape()) {
                                return generateEnum(config, shape, enumShapeChecker);
                            } else {
                                return generateClass(shape, shapes, config);
                            }
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList()));
        Map<URI, TypeSpec> visitorInterfaces = generateVisitors(config);
        typeSpecs.addAll(visitorInterfaces.values());
        typeSpecs = addVisitorAcceptMethods(typeSpecs, visitorInterfaces, config);
        Collection<TypeSpec> populatedInterfacesForRdfTypes = populateInterfacesForRdfTypes(interfacesForRdfTypes,
                        typeSpecs, config);
        typeSpecs.addAll(populatedInterfacesForRdfTypes);
        return typeSpecs;
    }

    private TypeSpec generateIndividuals(Shapes shapes, Shacl2JavaConfig config) {
        TypeSpec.Builder individualsTypeBuilder = TypeSpec.classBuilder("Individuals")
                        .addModifiers(PUBLIC)
                        .addAnnotation(AnnotationSpec.builder(Individuals.class).build());
        StreamSupport
                        .stream(Spliterators.spliteratorUnknownSize(shapes.iteratorAll(), Spliterator.ORDERED), false)
                        .distinct()
                        .forEach(shape -> {
                            // find focus nodes in the shapes graph itself
                            Collection<Node> focusNodes = focusNodes(shapes.getGraph(), shape);
                            for (Node focusNode : focusNodes) {
                                if (focusNode.isBlank() || focusNode.isLiteral()) {
                                    continue;
                                }
                                String name = NameUtils.enumConstantName(focusNode.getURI());
                                String typeName = NameUtils.classNameForShape(shape, config);
                                ClassName type = ClassName.get(config.getPackageName(),
                                                typeName);
                                // remember so we can inject individuals into shape-classes
                                individualNodeToClassName.put(focusNode, type);
                                FieldSpec fieldSpec = FieldSpec.builder(type, name)
                                                .addModifiers(PUBLIC, STATIC, FINAL)
                                                .addAnnotation(
                                                                AnnotationSpec.builder(ClassName.get(Individual.class))
                                                                                .addMember("value", "$S",
                                                                                                focusNode.getURI())
                                                                                .build())
                                                .addAnnotation(
                                                                AnnotationSpec.builder(ShapeNode.class)
                                                                                .addMember("value",
                                                                                                "{ $S }",
                                                                                                shape.getShapeNode()
                                                                                                                .getURI())
                                                                                .build())
                                                .initializer("new $T()", type)
                                                .build();
                                individualsTypeBuilder.addField(fieldSpec);
                            }
                        });
        return individualsTypeBuilder.build();
    }

    public TypeSpec generateClass(Shape shape, Shapes shapes, Shacl2JavaConfig config) {
        String name = NameUtils.classNameForShape(shape, config);
        TypeSpec.Builder typeBuilder = TypeSpec.classBuilder(name).addModifiers(PUBLIC);
        addInterfacesForRdfTypes(typeBuilder, shape, config);
        typeBuilder.addJavadoc(getJavadocGeneratedForShape(shape));
        typeBuilder.addAnnotation(
                        AnnotationSpec.builder(ShapeNode.class)
                                        .addMember("value",
                                                        "{ $S }",
                                                        shape.getShapeNode().getURI())
                                        .build());
        typeBuilder.addMethod(
                        MethodSpec.constructorBuilder()
                                        .addModifiers(PUBLIC)
                                        .build());
        addNodeFieldAndAccessors(typeBuilder);
        Set<Shape> relevantShapes = new HashSet<Shape>();
        relevantShapes.add(shape);
        relevantShapes.addAll(
                        ShapeUtils.getShNodeShapeNodes(shape)
                                        .stream()
                                        .map(shapes::getShape)
                                        .collect(Collectors.toSet()));
        configureTypeWithNodeShapes(typeBuilder, relevantShapes, shapes, config);
        addEqualsHashCodeToString(typeBuilder, config);
        TypeSpec typeSpec = typeBuilder.build();
        // remember type for visitor pattern generation
        shape.getShapeGraph()
                        .find(shape.getShapeNode(), RDF.type.asNode(), null)
                        .toSet().stream()
                        .map(Triple::getObject)
                        .map(Node::getURI)
                        .map(URI::create)
                        .filter(u -> config.getVisitorClasses().contains(u))
                        .forEach(visitorClassUri -> addToMultivalueMap(visitorClassToTypeSpec,
                                        visitorClassUri, typeSpec));
        return typeSpec;
    }

    public void addInterfacesForRdfTypes(TypeSpec.Builder typeBuilder, Shape shape, Shacl2JavaConfig config) {
        if (!config.isInterfacesForRdfTypes()) {
            return;
        }
        shape.getShapeGraph()
                        .find(shape.getShapeNode(), RDF.type.asNode(), null)
                        .toSet().stream()
                        .map(Triple::getObject)
                        .map(Node::getURI)
                        .filter(u -> !u.startsWith(SHACL.getURI()))
                        .map(u -> URI.create(u))
                        .forEach(u -> {
                            TypeSpec iFace = interfacesForRdfTypes.computeIfAbsent(u, typeUri -> TypeSpec
                                            .interfaceBuilder(classNameForShapeURI(typeUri, config))
                                            .addModifiers(PUBLIC)
                                            .build());
                            typeBuilder.addSuperinterface(ClassName.get(config.getPackageName(), iFace.name));
                            addToMultivalueMap(rdfTypeInterfaceImplNames, u, typeBuilder.build().name);
                        });
    }

    public List<TypeSpec> populateInterfacesForRdfTypes(Map<URI, TypeSpec> interfaceSpecs,
                    Collection<TypeSpec> allTypes, Shacl2JavaConfig config) {
        if (!config.isInterfacesForRdfTypes()) {
            return interfaceSpecs.values().stream().collect(Collectors.toList());
        }
        List<TypeSpec> modifiedInterfaces = new ArrayList<>();
        for (URI interfaceClassUri : interfaceSpecs.keySet()) {
            TypeSpec.Builder modifyingBuilder = interfaceSpecs.get(interfaceClassUri).toBuilder();
            Map<String, Set<MethodSpec>> candidateInterfaceMethodsByType = new HashMap<>();
            if (rdfTypeInterfaceImplNames.containsKey(interfaceClassUri)) {
                Set<String> implementingTypes = rdfTypeInterfaceImplNames.get(interfaceClassUri);
                for (TypeSpec type : allTypes) {
                    if (implementingTypes.contains(type.name)) {
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
                                            methodSpec.parameters.forEach(p -> b.addParameter(p));
                                            if (!methodSpec.returnType.equals(ClassName.VOID)) {
                                                b.addStatement("return null");
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
                                        interfaceSpecs.get(interfaceClassUri).name);
                    }
                    modifyingBuilder.addMethod(m);
                });
            }
            modifiedInterfaces.add(modifyingBuilder.build());
        }
        return modifiedInterfaces;
    }

    public TypeSpec generateEnum(Shacl2JavaConfig config, Shape shape, EnumShapeChecker enumShapeChecker) {
        ClassName name = NameUtils.javaPoetClassNameForShape(shape, config);
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

    private void configureTypeWithNodeShapes(TypeSpec.Builder typeBuilder, Set<Shape> relevantShapes,
                    Shapes shapes,
                    Shacl2JavaConfig config) {
        for (Shape relevantShape : relevantShapes) {
            Set<PropertyShape> propertyShapes = new HashSet();
            propertyShapes.addAll(relevantShape.getPropertyShapes());
            propertyShapes.addAll(ShapeUtils.getShPropertyShapes(relevantShape));
            // remember all fields we generate for the same path for this type, so we can
            // add a common interface
            // and a getter for the union of all these fields:
            Map<Path, Set<FieldSpec>> fieldsPerPath = new HashMap<>();
            Map<Path, Set<PropertySpec>> propSpecsPerPath = propertyShapes.stream().collect(Collectors.toMap(
                            s -> s.getPath(),
                            s -> (Set<PropertySpec>) ShapeUtils.getPropertySpecs(s),
                            (left, right) -> {
                                Set union = new HashSet(left);
                                union.addAll(right);
                                return union;
                            }));
            for (Path path : propSpecsPerPath.keySet()) {
                String propertyName = NameUtils.propertyNameForPath(path);
                logger.debug("generating property '{}' of {}", propertyName, NameUtils.nameForShape(relevantShape));
                Set<PropertySpec> propertySpecs = propSpecsPerPath.get(path);
                logger.debug("found property specs: \n{}",
                                propertySpecs.stream().map(Object::toString)
                                                .collect(Collectors.joining("\n", "\n", "\n")));
                boolean addTypeSuffix = propertySpecs.size() > 1;
                for (PropertySpec propertySpec : propertySpecs) {
                    addFieldWithPropertySpec(propertyName, path, propertySpec, typeBuilder, fieldsPerPath, config,
                                    addTypeSuffix);
                }
            }
            fieldsPerPath.entrySet().stream().forEach(pathToFields -> {
                Set<FieldSpec> fieldSpecs = pathToFields.getValue();
                if (fieldSpecs.size() < 2) {
                    return;
                }
                Path path = pathToFields.getKey();
                String fieldName = propertyNameForPath(path);
                addUnionGetter(fieldName, path, typeBuilder, fieldSpecs, config);
            });
        }
        relevantShapes
                        .stream()
                        .flatMap(s -> checkShapeForIndividuals(s).stream())
                        .collect(Collectors.toMap(
                                        s -> new Object[] { s.getPredicate(), s.getClassName() },
                                        s -> s,
                                        (left, right) -> IndividualPropertySpec.merge(left, right)))
                        .values()
                        .stream()
                        .forEach(spec -> addFieldWithIndividualPropertySpec(spec, typeBuilder, config));
    }

    private Set<IndividualPropertySpec> checkShapeForIndividuals(Shape shape) {
        Set<IndividualPropertySpec> individualPropertySpecs = new HashSet<>();
        shape.getShapeGraph()
                        .find(shape.getShapeNode(), null, null)
                        .forEach(triple -> {
                            Node pred = triple.getPredicate();
                            Node obj = triple.getObject();
                            if (!(pred.isURI() && obj.isURI())) {
                                return;
                            }
                            if (pred.getURI().startsWith(SHACL.getURI())) {
                                return;
                            }
                            if (obj.getURI().startsWith(SHACL.getURI())) {
                                return;
                            }
                            ClassName type = individualNodeToClassName.get(obj);
                            if (type == null) {
                                return;
                            }
                            // we found a static property, add to property spec:
                            IndividualPropertySpec spec = new IndividualPropertySpec(pred, type);
                            spec.addObject(obj);
                            individualPropertySpecs.add(spec);
                        });
        return individualPropertySpecs;
    }

    private static void addEqualsHashCodeToString(TypeSpec.Builder typeBuilder, Shacl2JavaConfig config) {
        ClassName typeClass = ClassName.get(config.getPackageName(), typeBuilder.build().name);
        MethodSpec toString = MethodSpec.methodBuilder("toString")
                        .returns(ClassName.get(String.class))
                        .addStatement("return getClass().getSimpleName()+\"{_node=\" + get_node() + \"}\"")
                        .addModifiers(PUBLIC)
                        .build();
        String hashparams = typeBuilder.fieldSpecs.stream().map(m -> m.name).collect(Collectors.joining(","));
        /**
         * MethodSpec hashCode = MethodSpec.methodBuilder("hashCode")
         * .addModifiers(PUBLIC) .returns(TypeName.INT)
         * .addStatement(String.format("return $T.hash(%s)", hashparams),
         * TypeName.get(Objects.class)) .build(); MethodSpec.Builder equalsBuilder =
         * MethodSpec.methodBuilder("equals"); equalsBuilder.addModifiers(PUBLIC)
         * .addParameter(TypeName.OBJECT, "o") .returns(TypeName.BOOLEAN)
         * .addStatement("if (this == o) return true") .addStatement("if (o == null ||
         * getClass() != o.getClass()) return false") .addStatement("$T obj = ($T) o",
         * typeClass, typeClass); String allEquals = typeBuilder.fieldSpecs .stream()
         * .map(fs -> String.format("Objects.equals(%s, obj.%s)", fs.name, fs.name))
         * .collect(Collectors.joining(" && ")); equalsBuilder.addStatement("return " +
         * allEquals); typeBuilder.addMethod(equalsBuilder.build());
         * typeBuilder.addMethod(hashCode);
         */
        MethodSpec hashCode = MethodSpec.methodBuilder("hashCode")
                        .addModifiers(PUBLIC)
                        .returns(TypeName.INT)
                        .addStatement("return $T.hash($N)", TypeName.get(Objects.class), "_node")
                        .build();
        MethodSpec equals = MethodSpec.methodBuilder("equals")
                        .addModifiers(PUBLIC)
                        .addParameter(TypeName.OBJECT, "o")
                        .returns(TypeName.BOOLEAN)
                        .addStatement("if (this == o) return true")
                        .addStatement("if (o == null || getClass() != o.getClass()) return false")
                        .addStatement("$T obj = ($T) o", typeClass, typeClass)
                        .addStatement("return _node.equals(obj._node)")
                        .build();
        typeBuilder.addMethod(equals);
        typeBuilder.addMethod(hashCode);
        typeBuilder.addMethod(toString);
    }

    private static void addUnionGetter(String fieldName, Path path, TypeSpec.Builder typeBuilder,
                    Set<FieldSpec> fieldSpecs, Shacl2JavaConfig config) {
        logger.debug("adding union getter for fields [{}]",
                        fieldSpecs.stream().map(f -> f.name).collect(Collectors.joining(",")));
        fieldName = plural(fieldName) + config.getUnionGetterSuffix();
        MethodSpec.Builder unionGetterbuilder = MethodSpec
                        .methodBuilder(getterNameForField(fieldName))
                        .returns(
                                        ParameterizedTypeName.get(
                                                        ClassName.get(Set.class),
                                                        ClassName.OBJECT))
                        .addModifiers(PUBLIC);
        String setName = "union";
        unionGetterbuilder.addStatement("Set $N = new $T()", setName, ClassName.get(HashSet.class));
        ClassName classNameSet = ClassName.get(Set.class);
        for (FieldSpec fieldSpec : fieldSpecs) {
            if (fieldSpec.type instanceof ParameterizedTypeName) {
                ClassName rawType = ((ParameterizedTypeName) fieldSpec.type).rawType;
                if (rawType.equals(classNameSet)) {
                    addStatementsSetAddAll(unionGetterbuilder, setName, fieldSpec.name);
                }
            } else if (fieldSpec.type.equals(classNameSet)) {
                addStatementsSetAddAll(unionGetterbuilder, setName, fieldSpec.name);
            } else {
                addStatementsSetAdd(unionGetterbuilder, setName, fieldSpec.name);
            }
        }
        unionGetterbuilder.addStatement("return $N", setName);
        unionGetterbuilder
                        .addJavadoc("Returns the union of all fields that represent the property path\n<p>'" +
                                        "{@code " + path.toString() + "}\n"
                                        + "'</p>\n<p> i.e. \n"
                                        + fieldSpecs.stream()
                                                        .map(f -> f.name)
                                                        .collect(Collectors.joining(
                                                                        "</code></li>\n\t<li><code>",
                                                                        "<ul>\n\t<li><code>",
                                                                        "</code></li>\n</ul>"))
                                        + "\n</p>");
        typeBuilder.addMethod(unionGetterbuilder.build());
    }

    private static void addStatementsSetAddAll(MethodSpec.Builder methodBuilder, String setName, String toAdd) {
        methodBuilder
                        .beginControlFlow("if ($N != null) ", toAdd)
                        .addStatement("$N.addAll($N)", setName, toAdd)
                        .endControlFlow();
    }

    private static void addStatementsSetAdd(MethodSpec.Builder methodBuilder, String setName, String toAdd) {
        methodBuilder
                        .beginControlFlow("if ($N != null)", toAdd)
                        .addStatement("$N.add($N)", setName, toAdd)
                        .endControlFlow();
    }

    private static void addNodeFieldAndAccessors(TypeSpec.Builder typeBuilder) {
        FieldSpec nodeField = FieldSpec.builder(TypeName.get(Node.class), "_node")
                        .addModifiers(PRIVATE).build();
        MethodSpec setter = generateSetter(nodeField);
        MethodSpec getter = generateGetter(nodeField);
        typeBuilder
                        .addField(nodeField)
                        .addMethod(setter)
                        .addMethod(getter);
    }

    private static void addFieldWithPropertySpec(String propertyName, Path path, PropertySpec propertySpec,
                    TypeSpec.Builder typeBuilder, Map<Path, Set<FieldSpec>> fieldsPerPath, Shacl2JavaConfig config,
                    boolean addTypeSuffix) {
        logger.debug("adding field {} for propertySpec: {}", propertyName, propertySpec);
        PropertyHelper helper = new PropertyHelper(propertyName, propertySpec, config, addTypeSuffix);
        TypeName fieldType = helper.getTypeName();
        String fieldName = helper.getFieldName();
        FieldSpec field = FieldSpec.builder(fieldType,
                        fieldName, PRIVATE)
                        .addAnnotation(
                                        AnnotationSpec.builder(PropertyPath.class)
                                                        .addMember(
                                                                        "value",
                                                                        "{ $S }",
                                                                        path.toString())
                                                        .build())
                        .build();
        CollectionUtils.addToMultivalueMap(fieldsPerPath, path, field);
        MethodSpec setter = generateSetter(field);
        if (propertySpec.hasNamedNodeShape() && propertySpec.getShNodeShape().getShapeNode().isURI()) {
            setter = setter.toBuilder().addJavadoc(
                            getAccessorJavadocGeneratedForNodeShape(propertySpec.getShNodeShape(), propertySpec))
                            .build();
        }
        MethodSpec getter = generateGetter(field);
        if (propertySpec.hasNamedNodeShape() && propertySpec.getShNodeShape().getShapeNode().isURI()) {
            getter = getter.toBuilder().addJavadoc(
                            getAccessorJavadocGeneratedForNodeShape(propertySpec.getShNodeShape(), propertySpec))
                            .build();
        }
        typeBuilder
                        .addField(field)
                        .addMethod(setter)
                        .addMethod(getter);
        if (!propertySpec.isSingletonProperty()) {
            typeBuilder.addMethod(generateAdder(field));
        }
    }

    private void addFieldWithIndividualPropertySpec(IndividualPropertySpec propertySpec,
                    TypeSpec.Builder typeBuilder, Shacl2JavaConfig config) {
        logger.debug("adding field {} for individual property spec: {}", propertySpec.getPredicate().getLocalName(),
                        propertySpec);
        IndividualPropertyHelper helper = new IndividualPropertyHelper(propertySpec, config, false);
        TypeName typeName = helper.getTypeName();
        String fieldName = helper.getFieldName();
        AnnotationSpec.Builder annBuilder = AnnotationSpec.builder(Individual.class);
        propertySpec.getObjects().forEach(o -> annBuilder.addMember("value", "{ $S }", o.getURI()));
        FieldSpec field = FieldSpec.builder(typeName,
                        fieldName, PRIVATE)
                        .addAnnotation(annBuilder.build())
                        .build();
        MethodSpec setter = generateSetter(field);
        MethodSpec getter = generateGetter(field);
        typeBuilder
                        .addField(field)
                        .addMethod(setter)
                        .addMethod(getter);
        if (!propertySpec.isSingletonProperty()) {
            typeBuilder.addMethod(generateAdder(field));
        }
    }

    private static String getJavadocGeneratedForShape(Shape shape) {
        Node shapeNode = shape.getShapeNode();
        if (shapeNode.isBlank()) {
            return "Generated for an anonymous shape";
        }
        String uri = shapeNode.getURI();
        return String.format("Generated for shape <a href=\"%s\">%s</a>", uri, uri);
    }

    private static String getAccessorJavadocGeneratedForNodeShape(Shape shape, PropertySpec propertySpec) {
        String uri = shape.getShapeNode().getURI();
        if (propertySpec.isSingletonProperty()) {
            return String.format("Value conforms to shape <a href=\"%s\">%s</a>", uri, uri);
        }
        return String.format("Values conform to shape <a href=\"%s\">%s</a>", uri, uri);
    }

    public static void writeClasses(List<TypeSpec> types, Shacl2JavaConfig config) throws IOException {
        File outputDir = new File(config.getOutputDir());
        outputDir.mkdirs();
        for (TypeSpec typeSpec : types) {
            JavaFile file = JavaFile.builder(config.getPackageName(), typeSpec).build();
            file.writeTo(outputDir);
        }
    }

    private static MethodSpec generateSetter(FieldSpec field) {
        return MethodSpec
                        .methodBuilder(NameUtils.setterNameForField(field))
                        .addParameter(field.type, field.name)
                        .addModifiers(PUBLIC)
                        .addStatement("this.$N = $N", field, field)
                        .build();
    }

    private static MethodSpec generateAdder(FieldSpec field) {
        TypeName baseType = ((ParameterizedTypeName) field.type).typeArguments.get(0);
        return MethodSpec
                        .methodBuilder(NameUtils.adderNameForFieldNameInPlural(field.name))
                        .addParameter(baseType, "toAdd")
                        .addModifiers(PUBLIC)
                        .beginControlFlow("if (this.$N == null)", field)
                        .addStatement("    this.$N = new $T()", field, ClassName.get(HashSet.class))
                        .endControlFlow()
                        .addStatement("this.$N.add($N)", field, "toAdd")
                        .build();
    }

    private static MethodSpec generateGetter(FieldSpec field) {
        MethodSpec.Builder mb = MethodSpec.methodBuilder(NameUtils.getterNameForField(field))
                        .returns(field.type)
                        .addModifiers(PUBLIC);
        if ((field.type instanceof ParameterizedTypeName)) {
            if (((ParameterizedTypeName) field.type).rawType.equals(ClassName.get(Set.class))) {
                mb.beginControlFlow("if (this.$N == null)", field.name)
                                .addStatement("return $T.emptySet()", ClassName.get(Collections.class))
                                .endControlFlow()
                                .addStatement("return $T.unmodifiableSet($N)", Collections.class, field.name);
                return mb.build();
            }
        }
        mb.addStatement("return this.$N", field.name);
        return mb.build();
    }

    private static boolean isAnonymousShape(Shape shape) {
        return shape.getShapeNode().isBlank();
    }

    private Map<URI, TypeSpec> generateVisitors(Shacl2JavaConfig config) {
        logger.debug("generating visitors for visitor classes {}", visitorClassToTypeSpec.keySet().stream()
                        .map(URI::toString)
                        .collect(Collectors.joining(",", "[", "]")));
        Map<URI, TypeSpec> newTypeSpecs = new HashMap<>();
        for (URI visitorClass : config.getVisitorClasses()) {
            // generate a visitor interface
            String visitorName = NameUtils.classNameForShapeURI(visitorClass, config) + config.getVisitorSuffix();
            TypeSpec.Builder ifBuilder = TypeSpec
                            .interfaceBuilder(visitorName)
                            .addModifiers(PUBLIC);
            if (visitorClassToTypeSpec.containsKey(visitorClass)) {
                for (TypeSpec typeSpec : visitorClassToTypeSpec.get(visitorClass)) {
                    logger.debug("proessing type {} as part of visitor class {}", typeSpec.name, visitorClass);
                    ifBuilder.addMethod(
                                    MethodSpec
                                                    .methodBuilder("visit")
                                                    .addParameter(ClassName.get(config.getPackageName(), typeSpec.name),
                                                                    "other")
                                                    .addModifiers(DEFAULT, PUBLIC)
                                                    .build());
                }
                ifBuilder.addMethod(MethodSpec.methodBuilder("onBeforeRecursion")
                                .addModifiers(PUBLIC, DEFAULT)
                                .addParameter(ParameterSpec.builder(ClassName.OBJECT, "parent").build())
                                .addParameter(ParameterSpec.builder(ClassName.OBJECT, "child").build())
                                .build());
                ifBuilder.addMethod(MethodSpec.methodBuilder("onAfterRecursion")
                                .addModifiers(PUBLIC, DEFAULT)
                                .addParameter(ParameterSpec.builder(ClassName.OBJECT, "parent").build())
                                .addParameter(ParameterSpec.builder(ClassName.OBJECT, "child").build())
                                .build());
                TypeSpec visitorInterface = ifBuilder.build();
                newTypeSpecs.put(visitorClass, visitorInterface);
            }
        }
        return newTypeSpecs;
    }

    private List<TypeSpec> addVisitorAcceptMethods(List<TypeSpec> typeSpecs, Map<URI, TypeSpec> visitorInterfaces,
                    Shacl2JavaConfig config) {
        logger.debug("generating visitors for visitor classes {}", visitorClassToTypeSpec.keySet().stream()
                        .map(URI::toString)
                        .collect(Collectors.joining(",", "[", "]")));
        Map<TypeSpec, Set<URI>> typeSpecToVisitorClasses = new HashMap();
        for (Map.Entry<URI, Set<TypeSpec>> entry : visitorClassToTypeSpec.entrySet()) {
            for (TypeSpec ts : entry.getValue()) {
                addToMultivalueMap(typeSpecToVisitorClasses, ts, entry.getKey());
            }
        }
        return typeSpecs.stream().map(typeSpec -> {
            if (!typeSpecToVisitorClasses.containsKey(typeSpec)) {
                return typeSpec;
            }
            TypeSpec.Builder modifyingBuilder = typeSpec.toBuilder();
            for (URI visitorClass : typeSpecToVisitorClasses.get(typeSpec)) {
                TypeSpec visitorInterface = visitorInterfaces.get(visitorClass);
                addAcceptMethodsForVisitorInterface(typeSpec, modifyingBuilder, visitorInterface, config);
            }
            return modifyingBuilder.build();
        }).collect(Collectors.toList());
    }

    private void addAcceptMethodsForVisitorInterface(TypeSpec typeSpec, TypeSpec.Builder modifyingBuilder,
                    TypeSpec visitorInterface, Shacl2JavaConfig config) {
        ClassName visitorClassName = ClassName.get(config.getPackageName(), visitorInterface.name);
        // generate an accept/acceptRecursively method in our class
        logger.debug("adding accept(visitor) and acceptRecursively(visitor) methods to {}", typeSpec.name);
        MethodSpec accept = MethodSpec.methodBuilder("accept")
                        .addParameter(visitorClassName, "visitor")
                        .addModifiers(PUBLIC)
                        .addStatement("$N.visit(this)", "visitor")
                        .build();
        MethodSpec.Builder acceptRecursivelyBuilder = MethodSpec.methodBuilder("acceptRecursively")
                        .addParameter(visitorClassName, "visitor")
                        .addParameter(ClassName.BOOLEAN, "depthFirst")
                        .addModifiers(PUBLIC)
                        .beginControlFlow("if (!$N)", "depthFirst")
                        .addStatement("$N.visit(this)", "visitor")
                        .endControlFlow();
        for (FieldSpec subElement : typeSpec.fieldSpecs) {
            // let's allow the visitor to visit the subelement, too.
            TypeName _subElTyp = subElement.type;
            TypeName rawSubElementType = null;
            if (subElement.type instanceof ParameterizedTypeName) {
                _subElTyp = ((ParameterizedTypeName) subElement.type).typeArguments.get(0);
                rawSubElementType = ((ParameterizedTypeName) subElement.type).rawType;
            }
            final TypeName subElementType = _subElTyp;
            if (visitorInterface.methodSpecs
                            .stream()
                            .filter(m -> m.name.equals("visit"))
                            .filter(m -> m.parameters.size() == 1)
                            .anyMatch(m -> m.parameters.get(0).type.equals(subElementType))) {
                if (subElement.type instanceof ParameterizedTypeName) {
                    logger.debug("adding recursion for field {}", subElement.name);
                    if (rawSubElementType != null && rawSubElementType.equals(TypeName.get(Set.class))) {
                        acceptRecursivelyBuilder
                                        .beginControlFlow("if ($N != null)", subElement.name)
                                        .beginControlFlow("$N.forEach(child -> ", subElement.name)
                                        .addStatement("visitor.onBeforeRecursion(this, child)")
                                        .addStatement("child.acceptRecursively(visitor, depthFirst)")
                                        .addStatement("visitor.onAfterRecursion(this, child)")
                                        .endControlFlow(")")
                                        .endControlFlow();
                    } else {
                        acceptRecursivelyBuilder
                                        .beginControlFlow("if ($N != null)", subElement.name)
                                        .addStatement("visitor.onBeforeRecursion(this, $N)", subElement.name)
                                        .addStatement("$N.acceptRecursively(visitor, depthFirst)", subElement.name)
                                        .addStatement("visitor.onAfterRecursion(this, $N)", subElement.name)
                                        .endControlFlow();
                    }
                }
            }
        }
        acceptRecursivelyBuilder.beginControlFlow("if ($N)", "depthFirst")
                        .addStatement("$N.visit(this)", "visitor")
                        .endControlFlow();
        modifyingBuilder
                        .addMethod(accept)
                        .addMethod(acceptRecursivelyBuilder.build());
    }

    private static class PropertyHelper {
        private TypeName typeName;
        private ClassName baseType;
        private String basePropertyName;
        private PropertySpec propertySpec;
        private Shacl2JavaConfig config;
        private String typeSuffix = "";
        private String propertyKindSuffix = "";
        private boolean addTypeIndicator = false;

        public PropertyHelper(String basePropertyName, PropertySpec propertySpec, Shacl2JavaConfig config,
                        boolean addTypeIndicator) {
            this.basePropertyName = basePropertyName;
            this.propertySpec = propertySpec;
            this.config = config;
            this.addTypeIndicator = addTypeIndicator;
            Class<?> candidateJavaType = determineCandidateJavaType();
            this.typeName = determineTypeName(candidateJavaType);
            this.typeSuffix = (config.isAlwaysAddTypeIndicator() || addTypeIndicator) ? getTypeIndicator() : "";
        }

        public TypeName getTypeName() {
            return typeName;
        }

        public String getFieldName() {
            String name = basePropertyName;
            if (!propertySpec.isSingletonProperty()) {
                name = plural(name);
            }
            return name + propertyKindSuffix + typeSuffix;
        }

        private String getTypeIndicator() {
            if (baseType == null) {
                return "";
            }
            if (!config.isAbbreviateTypeIndicators()) {
                return baseType.simpleName();
            }
            return baseType.simpleName().replaceAll("\\p{Lower}", "");
        }

        private TypeName determineTypeName(Class<?> candidateJavaType) {
            ClassName type = null;
            if (propertySpec.hasNamedNodeShape()) {
                // in this case, reset any propertyKindSuffix we may have set
                this.propertyKindSuffix = "";
                type = NameUtils.javaPoetClassNameForShape(propertySpec.getShNodeShape(), config);
            } else {
                type = ClassName.get(candidateJavaType);
            }
            if (type == null) {
                logger.debug("using default type 'Object' for propertySpec {}", propertySpec);
                type = TypeName.OBJECT;
            }
            this.baseType = type;
            if (propertySpec.isSingletonProperty()) {
                return type;
            } else {
                ClassName set = ClassName.get(Set.class);
                return ParameterizedTypeName.get(set, type);
            }
        }

        /**
         * If we can produce a java class for the field's type, we do that here
         *
         * @return
         */
        private Class<?> determineCandidateJavaType() {
            if (propertySpec.getShDatatype() != null) {
                return propertySpec.getShDatatype().getJavaClass();
            } else if (SHACL.IRI.equals(propertySpec.getShNodeKind())) {
                // if we're using this, we indicate that the String is an IRI
                return URI.class;
            } else if (propertySpec.getShInOrHasValue() != null) {
                return getTypeFromWhitelist(propertySpec.getShInOrHasValue());
            } else if (SHACL.Literal.equals(propertySpec.getShNodeKind())) {
                this.propertyKindSuffix = "Lit";
                return Literal.class;
            }
            return Object.class;
        }

        private Class<?> getTypeFromWhitelist(Set<Node> shInOrHasValue) {
            Iterator<Node> it = shInOrHasValue.iterator();
            Class<?> type = null; // cannot be in list
            while (it.hasNext()) {
                if (type == null) {
                    type = getJavaClassForNode(it.next());
                } else {
                    if (!type.equals(getJavaClassForNode(it.next()))) {
                        return Object.class;
                    }
                }
            }
            return type;
        }

        private Class<?> getJavaClassForNode(Node node) {
            if (node.isBlank()) {
                return Node_Blank.class;
            }
            if (node.isURI()) {
                return URI.class;
            }
            if (node.isLiteral()) {
                return node.getLiteralDatatype().getJavaClass();
            }
            throw new IllegalStateException("Node is neither blank, nor IRI, nor literal - cannot handle that");
        }
    }

    private static class IndividualPropertyHelper {
        private ClassName baseType;
        private TypeName typeName;
        private String basePropertyName;
        private IndividualPropertySpec propertySpec;
        private Shacl2JavaConfig config;
        private String typeSuffix = "";

        public IndividualPropertyHelper(IndividualPropertySpec propertySpec, Shacl2JavaConfig config,
                        boolean addTypeIndicator) {
            this.basePropertyName = propertySpec.getPredicate().getLocalName();
            this.propertySpec = propertySpec;
            this.config = config;
            this.baseType = propertySpec.getClassName();
            this.typeName = determineTypeName();
            this.typeSuffix = (config.isAlwaysAddTypeIndicator() || addTypeIndicator) ? getTypeIndicator() : "";
        }

        public TypeName getTypeName() {
            return typeName;
        }

        public String getFieldName() {
            String name = basePropertyName;
            if (!propertySpec.isSingletonProperty()) {
                name = plural(name);
            }
            return name + typeSuffix;
        }

        private String getTypeIndicator() {
            if (baseType == null) {
                return "";
            }
            if (!config.isAbbreviateTypeIndicators()) {
                return baseType.simpleName();
            }
            return baseType.simpleName().toString().replaceAll("\\p{Lower}", "");
        }

        private TypeName determineTypeName() {
            TypeName type = propertySpec.getClassName();
            if (propertySpec.isSingletonProperty()) {
                return type;
            } else {
                ClassName set = ClassName.get(Set.class);
                return ParameterizedTypeName.get(set, type);
            }
        }
    }
}

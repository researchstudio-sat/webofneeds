package won.shacl2java.sourcegen.typegen.logic;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Node_Blank;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.shacl.Shapes;
import org.apache.jena.shacl.parser.PropertyShape;
import org.apache.jena.shacl.parser.Shape;
import org.apache.jena.shacl.vocabulary.SHACL;
import org.apache.jena.sparql.path.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.shacl2java.Shacl2JavaConfig;
import won.shacl2java.annotation.Individual;
import won.shacl2java.annotation.PropertyPath;
import won.shacl2java.constraints.PropertySpec;
import won.shacl2java.sourcegen.typegen.TypesPostprocessor;
import won.shacl2java.sourcegen.typegen.mapping.ShapeTargetClasses;
import won.shacl2java.sourcegen.typegen.mapping.ShapeTypeSpecs;
import won.shacl2java.sourcegen.typegen.mapping.TypeSpecNames;
import won.shacl2java.sourcegen.typegen.support.IndividualPropertySpec;
import won.shacl2java.sourcegen.typegen.support.ProducerConsumerMap;
import won.shacl2java.util.CollectionUtils;
import won.shacl2java.util.NameUtils;
import won.shacl2java.util.ShapeUtils;

import static com.squareup.javapoet.TypeSpec.Kind.CLASS;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static won.shacl2java.sourcegen.typegen.support.TypegenUtils.findCommonSuperclassOrSuperinterface;
import static won.shacl2java.sourcegen.typegen.support.TypegenUtils.generateAdder;
import static won.shacl2java.sourcegen.typegen.support.TypegenUtils.generateGetter;
import static won.shacl2java.sourcegen.typegen.support.TypegenUtils.generateSetter;
import static won.shacl2java.util.NameUtils.getterNameForField;
import static won.shacl2java.util.NameUtils.plural;
import static won.shacl2java.util.NameUtils.propertyNameForPath;

public class MainTypesPostprocessor implements TypesPostprocessor {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private ShapeTypeSpecs.Consumer shapeTypeSpecs;
    private ProducerConsumerMap.Consumer individualClassNames;
    private TypeSpecNames.Consumer typeSpecNames;
    private ShapeTargetClasses.Consumer shapeTargetClasses;
    private Shapes shapes;
    private Shacl2JavaConfig config;

    public MainTypesPostprocessor(Shapes shapes,
                    Shacl2JavaConfig config,
                    ShapeTypeSpecs.Consumer shapeTypeSpecs,
                    ProducerConsumerMap.Consumer individualClassNames,
                    TypeSpecNames.Consumer typeSpecNames,
                    ShapeTargetClasses.Consumer shapeTargetClasses) {
        this.shapeTypeSpecs = shapeTypeSpecs;
        this.individualClassNames = individualClassNames;
        this.typeSpecNames = typeSpecNames;
        this.shapes = shapes;
        this.config = config;
        this.shapeTargetClasses = shapeTargetClasses;
    }

    @Override
    public Map<TypeSpec, TypeSpec> postprocess(Set<TypeSpec> typeSpecs) {
        Map<TypeSpec, TypeSpec> changes = new HashMap<>();
        for (Map.Entry<Shape, TypeSpec> shapeTypeSpec : shapeTypeSpecs.entrySet()) {
            Shape shape = shapeTypeSpec.getKey();
            TypeSpec typeSpec = shapeTypeSpec.getValue();
            if (typeSpec.kind != CLASS) {
                continue;
            }
            TypeSpec.Builder typeBuilder = typeSpec.toBuilder();
            Set<Shape> relevantShapes = new HashSet<Shape>();
            relevantShapes.add(shape);
            relevantShapes.addAll(
                            ShapeUtils.getShNodeShapeNodes(shape)
                                            .stream()
                                            .map(shapes::getShape)
                                            .collect(Collectors.toSet()));
            configureTypeWithNodeShapes(typeBuilder, relevantShapes, shapes, config);
            ClassName typeClass = ClassName.get(config.getPackageName(), typeBuilder.build().name);
            addToStringAllFields(typeBuilder);
            addDeepEquals(typeBuilder, typeClass);
            changes.put(typeSpec, typeBuilder.build());
        }
        return changes;
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
                Optional<String> propertyName = NameUtils.propertyNameForPath(path);
                if (!propertyName.isPresent()) {
                    continue;
                }
                logger.debug("generating property '{}' of {}", propertyName.get(),
                                NameUtils.nameForShape(relevantShape));
                Set<PropertySpec> propertySpecs = propSpecsPerPath.get(path);
                if (logger.isDebugEnabled()) {
                    for (PropertySpec propertySpec : propertySpecs) {
                        logger.debug("\tfound property spec: {}", propertySpec);
                    }
                }
                boolean addTypeSuffix = propertySpecs.size() > 1;
                for (PropertySpec propertySpec : propertySpecs) {
                    addFieldWithPropertySpec(shapes, propertyName.get(), path, propertySpec, typeBuilder, fieldsPerPath,
                                    config,
                                    addTypeSuffix);
                }
            }
            fieldsPerPath.entrySet().stream().forEach(pathToFields -> {
                Set<FieldSpec> fieldSpecs = pathToFields.getValue();
                if (fieldSpecs.size() < 2) {
                    return;
                }
                Path path = pathToFields.getKey();
                Optional<String> fieldName = propertyNameForPath(path);
                if (fieldName.isPresent()) {
                    addUnionGetter(fieldName.get(), path, typeBuilder, fieldSpecs, config);
                }
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
                            Optional<ClassName> type = individualClassNames.get(obj);
                            if (!type.isPresent()) {
                                return;
                            }
                            // we found a static property, add to property spec:
                            IndividualPropertySpec spec = new IndividualPropertySpec(pred, type.get());
                            spec.addObject(obj);
                            individualPropertySpecs.add(spec);
                        });
        return individualPropertySpecs;
    }

    private void addUnionGetter(String fieldName, Path path, TypeSpec.Builder typeBuilder,
                    Set<FieldSpec> fieldSpecs, Shacl2JavaConfig config) {
        logger.debug("adding union getter for fields [{}]",
                        fieldSpecs.stream().map(f -> f.name).collect(Collectors.joining(",")));
        fieldName = plural(fieldName) + config.getUnionGetterSuffix();
        TypeName commonSuperclass = findCommonSuperclassOrSuperinterface(fieldSpecs, typeSpecNames.asMap(), config);
        MethodSpec.Builder unionGetterbuilder = MethodSpec
                        .methodBuilder(getterNameForField(fieldName))
                        .returns(
                                        ParameterizedTypeName.get(
                                                        ClassName.get(Set.class),
                                                        commonSuperclass))
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

    private void addFieldWithPropertySpec(Shapes shapes, String propertyName, Path path, PropertySpec propertySpec,
                    TypeSpec.Builder typeBuilder, Map<Path, Set<FieldSpec>> fieldsPerPath, Shacl2JavaConfig config,
                    boolean addTypeSuffix) {
        logger.debug("adding field {} for propertySpec: {}", propertyName, propertySpec);
        PropertyHelper helper = new PropertyHelper(shapes, propertyName, propertySpec, config, addTypeSuffix);
        TypeName fieldType = helper.getTypeName();
        String fieldName = helper.getFieldName();
        FieldSpec field = FieldSpec.builder(fieldType,
                        fieldName, PRIVATE)
                        .addAnnotation(
                                        AnnotationSpec.builder(PropertyPath.class)
                                                        .addMember(
                                                                        "value",
                                                                        "$S",
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

    private static String getAccessorJavadocGeneratedForNodeShape(Shape shape, PropertySpec propertySpec) {
        String uri = shape.getShapeNode().getURI();
        if (propertySpec.isSingletonProperty()) {
            return String.format("Value conforms to shape <a href=\"%s\">%s</a>", uri, uri);
        }
        return String.format("Values conform to shape <a href=\"%s\">%s</a>", uri, uri);
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

    private static void addDeepEquals(TypeSpec.Builder typeBuilder, ClassName typeClass) {
        MethodSpec.Builder deepEqualsBuilder = MethodSpec.methodBuilder("deepEquals")
                        .addModifiers(PUBLIC)
                        .addParameter(TypeName.OBJECT, "o")
                        .addParameter(ClassName.get(ArrayDeque.class), "visited")
                        .returns(TypeName.BOOLEAN)
                        .addStatement("if (this == o) return true")
                        .addStatement("if (o == null || getClass() != o.getClass()) return false")
                        .beginControlFlow("if (visited.contains(this))")
                        .addStatement("return true")
                        .endControlFlow()
                        .addStatement("visited.push(this)")
                        .addStatement("$T obj = ($T) o", typeClass, typeClass)
                        .addStatement("boolean sub = false");
        for (FieldSpec subElement : typeBuilder.fieldSpecs) {
            TypeName _subElTyp = subElement.type;
            TypeName rawSubElementType = null;
            if (subElement.type instanceof ParameterizedTypeName) {
                _subElTyp = ((ParameterizedTypeName) subElement.type).typeArguments.get(0);
                rawSubElementType = ((ParameterizedTypeName) subElement.type).rawType;
            }
            final TypeName subElementType = _subElTyp;
            if (subElement.type instanceof ParameterizedTypeName) {
                logger.debug("adding recursion for field {}", subElement.name);
                String getter = getterNameForField(subElement.name);
                if (rawSubElementType != null && rawSubElementType.equals(TypeName.get(Set.class))) {
                    deepEqualsBuilder
                                    .beginControlFlow("sub = this.$N().stream().allMatch(child -> ", getter)
                                    .beginControlFlow("return obj.$N().stream().anyMatch( objChild -> ", getter)
                                    .beginControlFlow("try ")
                                    .addStatement("$T m = child.getClass().getMethod($S, $T.class, $T.class)",
                                                    Method.class,
                                                    "deepEquals",
                                                    Object.class, ArrayDeque.class)
                                    .addStatement("return (Boolean) m.invoke(child, objChild, visited)")
                                    .nextControlFlow("catch ($T e)", NoSuchMethodException.class)
                                    .addStatement("return child.equals(objChild)")
                                    .nextControlFlow("catch (Exception e)")
                                    .addStatement("throw new $T($S, e)", RuntimeException.class, "Cannot compare")
                                    .endControlFlow()
                                    .endControlFlow(")")
                                    .endControlFlow(")")
                                    .beginControlFlow("if (!sub)")
                                    .addStatement("visited.pop()")
                                    .addStatement("return false")
                                    .endControlFlow();
                } else {
                    deepEqualsBuilder
                                    .addStatement("$T child = this.$N()", subElementType, getter)
                                    .beginControlFlow("if (child != null)")
                                    .beginControlFlow("try ")
                                    .addStatement("$T m = child.getClass().getMethod($S, $T.class, $T.class)",
                                                    Method.class,
                                                    "deepEquals",
                                                    Object.class, ArrayDeque.class)
                                    .addStatement("return (Boolean) m.invoke(child, objChild, visited)")
                                    .nextControlFlow("catch ($T e)", NoSuchMethodException.class)
                                    .addStatement("return child.equals(objChild)")
                                    .nextControlFlow("catch (Exception e)")
                                    .addStatement("throw new $T($S, e)", RuntimeException.class, "Cannot compare")
                                    .endControlFlow()
                                    .beginControlFlow("if (!sub)")
                                    .addStatement("visited.pop()")
                                    .addStatement("return false")
                                    .endControlFlow()
                                    .endControlFlow();
                }
            }
        }
        deepEqualsBuilder
                        .addStatement("visited.pop()")
                        .addStatement("return true");
        typeBuilder.addMethod(deepEqualsBuilder.build());
    }

    private static void addToStringAllFields(TypeSpec.Builder typeBuilder) {
        MethodSpec.Builder toStringAllFieldsBuilder = MethodSpec.methodBuilder("toStringAllFields");
        toStringAllFieldsBuilder.addModifiers(PUBLIC)
                        .returns(TypeName.get(String.class))
                        .addStatement("$T sb = new $T()", StringBuilder.class, StringBuilder.class)
                        .addStatement(" sb.append(getClass().getSimpleName()).append($S)", "{");
        typeBuilder.fieldSpecs.stream()
                        .forEach(fs -> toStringAllFieldsBuilder
                                        .addStatement("sb.append($S).append($S)", fs.name, "=")
                                        .beginControlFlow("if ($N != null) ", fs.name)
                                        .addStatement("sb.append($N.toString())", fs.name)
                                        .nextControlFlow("else")
                                        .addStatement("sb.append($S)", "null")
                                        .endControlFlow()
                                        .addStatement("sb.append($S)", ", "));
        toStringAllFieldsBuilder
                        .addStatement("sb.delete(sb.length() - 2, sb.length())")
                        .addStatement("sb.append($S)", "}")
                        .addStatement("return sb.toString()");
        typeBuilder.addMethod(toStringAllFieldsBuilder.build());
    }

    private class PropertyHelper {
        private TypeName typeName;
        private ClassName baseType;
        private String basePropertyName;
        private PropertySpec propertySpec;
        private Shacl2JavaConfig config;
        private String typeSuffix = "";
        private String propertyKindSuffix = "";
        private boolean addTypeIndicator = false;
        private Shapes shapes;

        public PropertyHelper(Shapes shapes, String basePropertyName, PropertySpec propertySpec,
                        Shacl2JavaConfig config,
                        boolean addTypeIndicator) {
            this.shapes = shapes;
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
            } else if (propertySpec.hasShClass()) {
                Optional<Shape> shape = shapeTargetClasses.get(propertySpec.getShClass());
                if (shape.isPresent()) {
                    Optional<TypeSpec> typeSpec = shapeTypeSpecs.get(shape.get());
                    if (typeSpec.isPresent()) {
                        type = ClassName.get(config.getPackageName(), typeSpec.get().name);
                    }
                }
            }
            if (type == null) {
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

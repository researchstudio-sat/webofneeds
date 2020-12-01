package won.shacl2java;

import io.github.classgraph.*;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.shacl.Shapes;
import org.apache.jena.shacl.engine.ShaclPaths;
import org.apache.jena.shacl.parser.NodeShape;
import org.apache.jena.shacl.parser.PropertyShape;
import org.apache.jena.shacl.parser.Shape;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.path.Path;
import org.apache.jena.sparql.path.PathParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.shacl2java.annotation.Individual;
import won.shacl2java.annotation.Individuals;
import won.shacl2java.annotation.PropertyPath;
import won.shacl2java.annotation.ShapeNode;
import won.shacl2java.sourcegen.DataNodesAndShapes;
import won.shacl2java.sourcegen.DerivedInstantiationContext;
import won.shacl2java.sourcegen.InstantiationContext;
import won.shacl2java.util.CollectionUtils;
import won.shacl2java.util.NameUtils;
import won.shacl2java.util.ShapeUtils;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.*;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.apache.jena.shacl.validation.VLib.focusNodes;
import static org.apache.jena.shacl.validation.VLib.isFocusNode;
import static won.shacl2java.util.NameUtils.adderNameForFieldNameInPlural;
import static won.shacl2java.util.NameUtils.setterNameForField;

public class Shacl2JavaEntityFactory {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private Shapes shapes;
    private String[] packagesToScan;
    private InstantiationContext baseInstantiationContext;
    private InstantiationContext dataInstantiationContext;

    public Shacl2JavaEntityFactory(Shapes shapes, String... packagesToScan) {
        this.shapes = shapes;
        this.packagesToScan = packagesToScan;
        this.baseInstantiationContext = new InstantiationContext(shapes.getGraph());
        scanPackages(baseInstantiationContext);
        instantiateAll(baseInstantiationContext);
    }

    /**
     * Removes the results of any prior calls to <code>load(Graph)</code>.
     */
    public void reset() {
        this.dataInstantiationContext = null;
    }

    /**
     * Instantiates everything found in data. Replaces any results from previous
     * calls to <code>load(Graph)</code>.
     * 
     * @param data
     */
    public void load(Graph data) {
        reset();
        DerivedInstantiationContext dataContext = new DerivedInstantiationContext(data, baseInstantiationContext);
        instantiateAll(dataContext);
        this.dataInstantiationContext = dataContext;
    }

    private void scanPackages(InstantiationContext ctx) {
        try (ScanResult scanResult = new ClassGraph().enableAllInfo().acceptPackages(packagesToScan)
                        .scan()) {
            ClassInfoList shapeClassInfoList = scanResult.getClassesWithAnnotation(ShapeNode.class.getName());
            for (ClassInfo shapeClassInfo : shapeClassInfoList) {
                AnnotationInfo annotationInfo = shapeClassInfo.getAnnotationInfo(ShapeNode.class.getName());
                AnnotationParameterValueList paramVals = annotationInfo.getParameterValues();
                AnnotationParameterValue nodes = paramVals.get("value");
                String[] shapeNodes = (String[]) nodes.getValue();
                for (int i = 0; i < shapeNodes.length; i++) {
                    ctx.setClassForShape(shapeNodes[i], shapeClassInfo.loadClass());
                }
            }
            ClassInfoList individualClassInfoList = scanResult.getClassesWithAnnotation(Individuals.class.getName());
            for (ClassInfo individualClassInfo : individualClassInfoList) {
                Class<?> individualType = individualClassInfo.loadClass();
                for (FieldInfo field : individualClassInfo.getFieldInfo()) {
                    Object instance = null;
                    try {
                        Field rField = individualType.getField(field.getName());
                        if (rField != null) {
                            instance = rField.get(null);
                        } else {
                            continue;
                        }
                        // extract shape and focusNode, and add them to the data structures
                        AnnotationInfo annotationInfo = field.getAnnotationInfo(ShapeNode.class.getName());
                        AnnotationParameterValueList paramVals = annotationInfo.getParameterValues();
                        AnnotationParameterValue nodes = paramVals.get("value");
                        String shapeNodeStr = ((String[]) nodes.getValue())[0];
                        Node shapeNode = NodeFactory.createURI(shapeNodeStr);
                        annotationInfo = field.getAnnotationInfo(Individual.class.getName());
                        paramVals = annotationInfo.getParameterValues();
                        nodes = paramVals.get("value");
                        String focusNodeStr = ((String[]) nodes.getValue())[0];
                        Node focusNode = NodeFactory.createURI(focusNodeStr);
                        Method m = instance.getClass().getMethod("set_node", Node.class);
                        if (m != null) {
                            m.invoke(instance, focusNode);
                        } else {
                            continue;
                        }
                        ctx.setInstanceForFocusNode(focusNode, instance);
                        ctx.setFocusNodeForInstance(instance, focusNode);
                        ctx.addShapeForFocusNode(focusNode, this.shapes.getShape(shapeNode));
                    } catch (Exception e) {
                        throw new IllegalStateException("Cannot set node using set_node() on instance " + instance, e);
                    }
                }
            }
        }
    }

    private void instantiateAll(InstantiationContext ctx) {
        Set<DataNodesAndShapes> toInstantiate = null;
        while (toInstantiate == null || toInstantiate.size() > 0) {
            if (toInstantiate == null) {
                toInstantiate = StreamSupport.stream(shapes.spliterator(), true).flatMap(shape -> {
                    return instantiate(null, shape, false, ctx).stream();
                }).collect(Collectors.toSet());
            } else {
                toInstantiate = toInstantiate.parallelStream().flatMap(dataNodesAndShapes -> {
                    if (dataNodesAndShapes.getShapeNodes().isEmpty()) {
                        return dataNodesAndShapes.getDataNodes().stream()
                                        .flatMap(focusNode -> StreamSupport.stream(shapes.spliterator(), true).flatMap(
                                                        shape -> instantiate(focusNode, shape, false, ctx).stream()));
                    } else {
                        return dataNodesAndShapes.getShapeNodes().parallelStream()
                                        .flatMap(shapeNode -> dataNodesAndShapes.getDataNodes().parallelStream()
                                                        .flatMap(focusNode -> instantiate(focusNode,
                                                                        shapes.getShape(shapeNode), true, ctx)
                                                                                        .stream()));
                    }
                }).collect(Collectors.toSet());
            }
        }
        ctx.getMappedInstances().parallelStream().forEach(entry -> {
            Node node = entry.getKey();
            Object instance = entry.getValue();
            if (logger.isDebugEnabled()) {
                logger.debug("wiring dependencies of instance {} ", node);
            }
            wireDependencies(instance, ctx);
        });
    }

    public <T> Set<T> getEntitiesOfType(Class<T> type) {
        if (dataInstantiationContext == null) {
            return Collections.emptySet();
        }
        return dataInstantiationContext.getEntitiesOfType(type);
    }

    public Map<String, Object> getInstanceMap() {
        if (dataInstantiationContext == null) {
            return Collections.emptyMap();
        }
        return dataInstantiationContext.getInstanceMap();
    }

    public Collection<Object> getInstances() {
        if (dataInstantiationContext == null) {
            return Collections.emptySet();
        }
        return dataInstantiationContext.getInstances();
    }

    /**
     * Creates all instances for the given node / shape combination.
     *
     * @param node may be null, in which case all target nodes of the shape are
     * chosen.
     * @param shape
     * @return the set of nodes reached during instantiation that have not been
     * instantiated yet
     */
    public Set<DataNodesAndShapes> instantiate(Node node, Shape shape, boolean forceApplyShape,
                    InstantiationContext ctx) {
        if (shape.getShapeNode().isBlank()) {
            if (node == null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Not instantiating entity for shape {}", shape.getShapeNode().getBlankNodeLabel());
                }
            } else {
                throw new IllegalArgumentException(
                                String.format("Cannot instantiate entity for node %s: shape %s is a blank node",
                                                node.getLocalName(),
                                                shape.getShapeNode().getBlankNodeLabel()));
            }
        }
        Collection<Node> focusNodes;
        if (node != null) {
            if (!forceApplyShape) {
                if (!isFocusNode(shape, node, ctx.getData())) {
                    return Collections.emptySet();
                }
            }
            if (ctx.hasInstanceForFocusNode(node)) {
                ctx.addShapeForFocusNode(node, shape);
                return Collections.emptySet();
            }
            focusNodes = Collections.singleton(node);
        } else {
            focusNodes = focusNodes(ctx.getData(), shape);
        }
        if (focusNodes.isEmpty()) {
            return Collections.emptySet();
        }
        String shapeURI = shape.getShapeNode().getURI();
        Class<?> classForShape = ctx.getClassForShape(shapeURI);
        if (classForShape == null) {
            throw new IllegalArgumentException(String.format("No class found to instantiate for shape %s",
                            shape.getShapeNode().getLocalName()));
        }
        // our shape might reference other shapes via sh:node (maybe through other
        // operators such as sh:or)
        // * we remember them for wiring
        // * we return them as dependencies to instantiate
        Set<Shape> allRelevantShapes = ShapeUtils.getNodeShapes((NodeShape) shape, shapes);
        allRelevantShapes.add(shape);
        DataNodesAndShapes.SetBuilder resultBuilder = DataNodesAndShapes.setBuilder();
        for (Node focusNode : focusNodes) {
            try {
                Object instance = null;
                if (logger.isDebugEnabled()) {
                    logger.debug("attempting to instantiate focus node {} with shape {}", focusNode,
                                    shape.getShapeNode());
                }
                instance = instantiate(shape, shapeURI, focusNode, ctx);
                if (instance == null) {
                    // we failed to instantiate an enum constant for the focus node of what looked
                    // like an enum shape.
                    // maybe another shape works better
                    continue;
                }
                ctx.setInstanceForFocusNode(focusNode, instance);
                ctx.setFocusNodeForInstance(instance, focusNode);
                ctx.setClassForInstance(instance, classForShape);
                ctx.addShapesForFocusNode(focusNode, allRelevantShapes);
            } catch (NoSuchMethodException e) {
                throw new IllegalArgumentException(
                                String.format("Cannot instantiate %s for shape %s",
                                                classForShape.getName(), shape.getShapeNode().getLocalName())
                                                + ": no parameterless constructor found",
                                e);
            } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
                throw new IllegalArgumentException(String.format("Cannot instantiate %s for shape %s - %s: %s",
                                classForShape.getName(), shape.getShapeNode().getLocalName(),
                                e.getClass().getSimpleName(),
                                e.getMessage()), e);
            }
            if (logger.isDebugEnabled()) {
                logger.debug("Collecting dependencies of focus node {}", focusNode);
            }
            Map<Path, Set<PropertyShape>> propertyShapesPerPath = getPropertyShapesByPath(allRelevantShapes);
            for (Path path : propertyShapesPerPath.keySet()) {
                if (logger.isDebugEnabled()) {
                    logger.debug("\twiring properties for path {} ", path);
                }
                Set<Node> valueNodes = ShaclPaths.valueNodes(ctx.getData(), focusNode, path);
                for (PropertyShape propertyShape : propertyShapesPerPath.get(path)) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("\t\tFound property shape {} ", propertyShape.getShapeNode());
                    }
                    if (!valueNodes.isEmpty()) {
                        Set<Node> candidateNodeShapes = ShapeUtils.getShNodeShapes(propertyShape, shapes)
                                        .stream()
                                        .filter(s -> !s.getShapeNode().isBlank())
                                        .map(Shape::getShapeNode)
                                        .collect(Collectors.toSet());
                        if (logger.isDebugEnabled()) {
                            logger.debug("\t\t\tAdding to dependencies: {} with candidate shapes {}",
                                            Arrays.toString(valueNodes.toArray()),
                                            Arrays.toString(candidateNodeShapes.toArray()));
                        }
                        resultBuilder.dataNodes(valueNodes);
                        resultBuilder.shapeNodes(candidateNodeShapes);
                        resultBuilder.newSet();
                    }
                }
            }
        }
        return resultBuilder.build();
    }

    public Object instantiate(Shape shape, String shapeURI, Node focusNode, InstantiationContext ctx)
                    throws InstantiationException,
                    IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        Object instance;
        Class<Enum> type = (Class<Enum>) ctx.getClassForShape(shapeURI);
        if (type.isEnum()) {
            if (ShapeUtils.checkForEnumShape(shape).isEnumShape()) {
                return instantiateEnum(focusNode, type);
            }
        }
        return instance = instantiateClass(shapeURI, focusNode, ctx);
    }

    public Object instantiateClass(String shapeURI, Node focusNode, InstantiationContext ctx)
                    throws InstantiationException,
                    IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        Object instance;
        instance = ctx.getClassForShape(shapeURI).getConstructor().newInstance();
        Method nodeSetter = instance.getClass().getMethod("set_node", Node.class);
        if (nodeSetter != null) {
            nodeSetter.invoke(instance, focusNode);
        }
        return instance;
    }

    public Object instantiateEnum(Node focusNode, Class<Enum> type) {
        String enumName = NameUtils.enumConstantName(focusNode);
        if (enumName != null) {
            Enum[] enumConstants = type.getEnumConstants();
            for (int i = 0; i < enumConstants.length; i++) {
                Enum enumConstant = enumConstants[i];
                if (enumConstant.name().equals(enumName)) {
                    return enumConstant;
                }
            }
        }
        return null;
    }

    private void wireDependencies(Object instance, InstantiationContext ctx) {
        Map<Path, Set<Field>> fieldsByPath = new HashMap<>();
        Class<?> type = ctx.getClassForInstance(instance);
        if (type == null) {
            throw new IllegalStateException(
                            String.format("No class found for instance %s of class %s", instance, instance.getClass()));
        }
        Field[] instanceFields = type.getDeclaredFields();
        for (Field field : instanceFields) {
            extractPath(fieldsByPath, field);
            wireIndividuals(instance, field, ctx);
        }
        Node focusNode = ctx.getFocusNodeForInstance(instance);
        if (focusNode == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("No focus node found for instance {}", instance);
            }
            return;
        }
        Set<Shape> shapesForFocusNode = ctx.getShapesForFocusNode(focusNode);
        if (shapesForFocusNode == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("No shapes found for focus node {}", focusNode);
            }
            return;
        }
        Set<Path> pathsFromPropertyShapes = ctx.getShapesForFocusNode(focusNode)
                        .stream()
                        .flatMap(s -> s.getPropertyShapes().stream())
                        .map(PropertyShape::getPath)
                        .collect(Collectors.toSet());
        for (Path path : pathsFromPropertyShapes) {
            Set<Node> valueNodes = ShaclPaths.valueNodes(ctx.getData(), focusNode, path);
            Set<Field> fieldsForPath = fieldsByPath.get(path);
            if (fieldsForPath != null && !fieldsForPath.isEmpty()) {
                if (valueNodes.size() > 0) {
                    for (Node valueNode : valueNodes) {
                        Object dependency = ctx.getInstanceForFocusNode(valueNode);
                        if (dependency == null) {
                            // dependency could be a blank node, literal or IRI that is not explicitly
                            // covered by a shape
                            if (valueNode.isLiteral()) {
                                dependency = valueNode.getLiteralDatatype()
                                                .parse(valueNode.getLiteralLexicalForm());
                            } else if (valueNode.isURI()) {
                                dependency = URI.create(valueNode.getURI());
                            }
                        }
                        Field field = null;
                        try {
                            field = selectFieldByType(fieldsForPath, instance, dependency);
                            if (field == null) {
                                throw new IllegalArgumentException(
                                                makeWiringErrorMessage(instance, focusNode, dependency, null, null)
                                                                + ": unable to identify appropriate field to set");
                            }
                            setDependency(instance, field.getName(), dependency);
                            if (logger.isDebugEnabled()) {
                                logger.debug("wired {} ",
                                                makeWiringMessage(instance, focusNode, dependency, field));
                            }
                        } catch (Throwable e) {
                            throw new IllegalArgumentException(
                                            makeWiringErrorMessage(instance, focusNode, dependency, field, e), e);
                        }
                    }
                }
            }
        }
    }

    private void extractPath(Map<Path, Set<Field>> fieldsByPath, Field field) {
        PropertyPath propertyPath = field.getAnnotation(PropertyPath.class);
        if (propertyPath == null) {
            return;
        }
        String[] shapeURIs = propertyPath.value();
        if (shapeURIs == null || shapeURIs.length == 0) {
            return;
        }
        Stream.of(shapeURIs)
                        .map(pathStr -> PathParser.parse(pathStr, PrefixMapping.Standard))
                        .forEach(
                                        path -> CollectionUtils.addToMultivalueMap(fieldsByPath, path, field));
    }

    private void wireIndividuals(Object instance, Field field, InstantiationContext ctx) {
        Individual individual = field.getAnnotation(Individual.class);
        if (individual == null) {
            return;
        }
        String[] individualUris = individual.value();
        if (individualUris == null || individualUris.length == 0) {
            return;
        }
        Stream.of(individualUris)
                        .map(individualUri -> ctx.getInstanceForFocusNode(NodeFactory.createURI(individualUri)))
                        .forEach(individualInstance -> setDependency(instance, field.getName(), individualInstance));
    }

    private String makeWiringErrorMessage(Object instance, Node focusNode, Object dependency, Field field,
                    Throwable e) {
        if (e == null) {
            return "Cannot wire " + makeWiringMessage(instance, focusNode, dependency, field);
        }
        return String.format("Cannot wire %s - %s: %s", makeWiringMessage(instance, focusNode, dependency, field),
                        e.getClass().getSimpleName(), e.getMessage());
    }

    private String makeWiringMessage(Object instance, Node focusNode, Object dependency, Field field) {
        String instClassStr = Optional.ofNullable(instance).map(o -> o.getClass().getSimpleName())
                        .orElse("[instance:null]");
        String focusNodeStr = Optional.ofNullable(focusNode).map(Object::toString).orElse("[focusnode:null]");
        String fieldStr = Optional.ofNullable(field).map(Field::getName).orElse("[field:null]");
        String dependencyStr = Optional.ofNullable(dependency).map(Object::toString).orElse("[dependency:null]");
        return String.format("((%s) %s).%s = %s", instClassStr, focusNodeStr, fieldStr, dependencyStr);
    }

    private Field selectFieldByType(Set<Field> fields, Object instance, Object dependency)
                    throws NoSuchFieldException {
        for (Field field : fields) {
            if (field.getType().isAssignableFrom(dependency.getClass())) {
                return field;
            }
        }
        for (Field field : fields) {
            if (field.getType().isAssignableFrom(Set.class)) {
                Type typeArg = ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
                if (typeArg instanceof Class && ((Class<?>) typeArg).isAssignableFrom(dependency.getClass())) {
                    return field;
                }
            }
        }
        throw new IllegalStateException(String.format(
                        "Could not find appropriate field for instance %s and dependency %s", instance, dependency));
    }

    private void setDependency(Object instance, String fieldName, Object dependency) {
        try {
            if (setUsingSetter(instance, fieldName, dependency)) {
                return;
            }
            if (setUsingAdder(instance, fieldName, dependency)) {
                return;
            }
        } catch (Exception e) {
            throw new RuntimeException(
                            String.format("Error using setter/adder method for %s on %s", fieldName, instance), e);
        }
        throw new RuntimeException(
                        String.format("Could not find setter/adder method for %s on %s", fieldName, instance));
    }

    private boolean setUsingSetter(Object instance, String fieldName, Object dependency)
                    throws IllegalAccessException, InvocationTargetException {
        String methodName = setterNameForField(fieldName);
        return setUsingMethodName(instance, dependency, methodName);
    }

    private boolean setUsingAdder(Object instance, String fieldName, Object dependency)
                    throws IllegalAccessException, InvocationTargetException {
        String methodName = adderNameForFieldNameInPlural(fieldName);
        return setUsingMethodName(instance, dependency, methodName);
    }

    private boolean setUsingMethodName(Object instance, Object dependency, String methodName)
                    throws IllegalAccessException, InvocationTargetException {
        Method[] methods = instance.getClass().getDeclaredMethods();
        for (int i = 0; i < methods.length; i++) {
            Method candidate = methods[i];
            if (!candidate.getName().equals(methodName)) {
                continue;
            }
            Class<?>[] params = candidate.getParameterTypes();
            if (params.length != 1) {
                continue;
            }
            if (params[0].isAssignableFrom(dependency.getClass())) {
                candidate.invoke(instance, dependency);
                return true;
            }
        }
        return false;
    }

    private static Map<Path, Set<PropertyShape>> getPropertyShapesByPath(Set<Shape> nodeShapes) {
        Map<Path, Set<PropertyShape>> propertyShapesPerPath = nodeShapes
                        .stream()
                        .flatMap(s -> s.getPropertyShapes().stream())
                        .collect(Collectors.toMap(
                                        s -> s.getPath(),
                                        s -> Collections.singleton(s),
                                        (left, right) -> {
                                            Set union = new HashSet(left);
                                            union.addAll(right);
                                            return union;
                                        }));
        return propertyShapesPerPath;
    }
}

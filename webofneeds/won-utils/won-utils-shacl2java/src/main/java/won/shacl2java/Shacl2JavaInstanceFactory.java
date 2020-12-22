package won.shacl2java;

import io.github.classgraph.AnnotationInfo;
import io.github.classgraph.AnnotationParameterValue;
import io.github.classgraph.AnnotationParameterValueList;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ClassInfoList;
import io.github.classgraph.FieldInfo;
import io.github.classgraph.ScanResult;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.apache.commons.collections4.SetUtils;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.shacl.Shapes;
import org.apache.jena.shacl.engine.ShaclPaths;
import org.apache.jena.shacl.engine.ValidationContext;
import org.apache.jena.shacl.parser.NodeShape;
import org.apache.jena.shacl.parser.PropertyShape;
import org.apache.jena.shacl.parser.Shape;
import org.apache.jena.shacl.validation.VLib;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.graph.GraphFactory;
import org.apache.jena.sparql.path.Path;
import org.apache.jena.sparql.path.PathParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.shacl2java.annotation.Individual;
import won.shacl2java.annotation.Individuals;
import won.shacl2java.annotation.PropertyPath;
import won.shacl2java.annotation.ShapeNode;
import won.shacl2java.instantiation.DataNodeAndShapes;
import won.shacl2java.instantiation.DerivedInstantiationContext;
import won.shacl2java.instantiation.InstantiationContext;
import won.shacl2java.util.CollectionUtils;
import won.shacl2java.util.NameUtils;
import won.shacl2java.util.ShapeUtils;

import static org.apache.jena.shacl.validation.VLib.focusNodes;
import static org.apache.jena.shacl.validation.VLib.isFocusNode;
import static won.shacl2java.util.NameUtils.adderNameForFieldNameInPlural;
import static won.shacl2java.util.NameUtils.setterNameForField;

public class Shacl2JavaInstanceFactory {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private Shapes shapes;
    private String[] packagesToScan;
    private InstantiationContext baseInstantiationContext;
    private InstantiationContext dataInstantiationContext;
    private boolean instantiateViolatingNodes = false;

    public Shacl2JavaInstanceFactory(Shapes shapes, String... packagesToScan) {
        this.shapes = shapes;
        this.packagesToScan = packagesToScan;
        InstantiationContext ctx = new InstantiationContext(shapes);
        scanPackages(ctx);
        instantiateAll(ctx);
        this.baseInstantiationContext = ctx;
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
     * @param data the data to load
     * @param instantiateViolatingNodes if <code>true</code>, instances are
     * generated for nodes even if they violate one or more of their shapes.
     */
    public void load(Graph data, boolean instantiateViolatingNodes) {
        reset();
        this.instantiateViolatingNodes = instantiateViolatingNodes;
        DerivedInstantiationContext dataContext = new DerivedInstantiationContext(data, baseInstantiationContext);
        instantiateAll(dataContext);
        this.dataInstantiationContext = dataContext;
    }

    public void load(Graph data) {
        load(data, false);
    }

    public Set<Object> getInstances(String uri) {
        if (dataInstantiationContext == null) {
            return Collections.emptySet();
        }
        return dataInstantiationContext.getInstances(uri);
    }

    public <T> Set<T> getInstancesOfType(Class<T> type) {
        if (dataInstantiationContext == null) {
            return Collections.emptySet();
        }
        return dataInstantiationContext.getInstanceOfType(type);
    }

    public <T> Optional<T> getInstanceOfType(String uri, Class<T> type) {
        if (dataInstantiationContext == null) {
            return Optional.empty();
        }
        return dataInstantiationContext.getInstanceOfType(uri, type);
    }

    public Map<String, Set<Object>> getInstanceMap() {
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

    private void scanPackages(InstantiationContext ctx) {
        // we pass this graph to individuals to hide their triples in the
        // base graph when generating RDF using RdfOutput
        Graph graphForIndividuals = GraphFactory.createGraphMem();
        try (ScanResult scanResult = new ClassGraph().enableAllInfo().acceptPackages(packagesToScan)
                        .scan()) {
            ClassInfoList shapeClassInfoList = scanResult.getClassesWithAnnotation(ShapeNode.class.getName());
            for (ClassInfo shapeClassInfo : shapeClassInfoList) {
                AnnotationInfo annotationInfo = shapeClassInfo.getAnnotationInfo(ShapeNode.class.getName());
                AnnotationParameterValueList paramVals = annotationInfo.getParameterValues();
                AnnotationParameterValue nodes = paramVals.get("value");
                String[] shapeNodes = (String[]) nodes.getValue();
                for (int i = 0; i < shapeNodes.length; i++) {
                    ctx.addClassForShape(shapeNodes[i], shapeClassInfo.loadClass());
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
                        Method m = instance.getClass().getMethod("setNode", Node.class);
                        if (m != null) {
                            m.invoke(instance, focusNode);
                        } else {
                            continue;
                        }
                        m = instance.getClass().getMethod("setGraph", Graph.class);
                        if (m != null) {
                            m.invoke(instance, graphForIndividuals);
                        } else {
                            continue;
                        }
                        ctx.addInstanceForFocusNode(focusNode, instance);
                        ctx.setFocusNodeForInstance(instance, focusNode);
                        ctx.addShapeForFocusNode(focusNode, this.shapes.getShape(shapeNode));
                    } catch (Exception e) {
                        throw new IllegalStateException("Cannot set node using setNode() on instance " + instance, e);
                    }
                }
            }
        }
    }

    private Set<DataNodeAndShapes> deduplicate(Set<DataNodeAndShapes> sets) {
        return sets.parallelStream().collect(Collectors.toMap(
                        // calculate union of all shapes collected per value node
                        dnas -> dnas.getDataNode(),
                        dnas -> dnas.getShapeNodes(),
                        (left, right) -> {
                            Set<Node> merged = new HashSet<>();
                            merged.addAll(left);
                            merged.addAll(right);
                            return merged;
                        })) // transform back into DataNodeAndShapes objects
                        .entrySet()
                        .parallelStream()
                        .map(entry -> new DataNodeAndShapes(entry.getKey(), entry.getValue()))
                        .collect(Collectors.toSet());
    }

    private void instantiateAll(InstantiationContext ctx) {
        Set<DataNodeAndShapes> toInstantiate = null;
        while (toInstantiate == null || toInstantiate.size() > 0) {
            if (toInstantiate == null) {
                Set<Shape> shapeSet = new HashSet();
                Iterator<Shape> it = shapes.iteratorAll();
                while (it.hasNext()) {
                    shapeSet.add(it.next());
                }
                toInstantiate = shapeSet.parallelStream().map(shape -> {
                    return instantiate(null, shape, false, ctx);
                }).reduce(SetUtils::union).orElseGet(() -> Collections.emptySet());
                toInstantiate = deduplicate(toInstantiate);
            } else {
                toInstantiate = toInstantiate.parallelStream().map(dataNodeAndShapes -> {
                    if (dataNodeAndShapes.getShapeNodes().isEmpty()) {
                        return StreamSupport
                                        .stream(shapes.spliterator(), true)
                                        .map(shape -> instantiate(dataNodeAndShapes.getDataNode(),
                                                        shape, false, ctx))
                                        .reduce(SetUtils::union).orElseGet(() -> Collections.emptySet());
                    } else {
                        return dataNodeAndShapes
                                        .getShapeNodes()
                                        .parallelStream()
                                        .map(shapeNode -> instantiate(dataNodeAndShapes.getDataNode(),
                                                        shapes.getShape(shapeNode), true, ctx))
                                        .reduce(SetUtils::union).orElseGet(() -> Collections.emptySet());
                    }
                }).reduce(SetUtils::union).orElseGet(() -> Collections.emptySet());
                toInstantiate = deduplicate(toInstantiate);
            }
        }
        ctx.getInstancesByNode().parallelStream().forEach(entry -> {
            Node node = entry.getKey();
            Set<Object> instances = entry.getValue();
            instances.parallelStream().forEach(instance -> {
                if (logger.isDebugEnabled()) {
                    logger.debug("wiring dependencies of instance {} ", node);
                }
                wireDependencies(instance, ctx);
            });
        });
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
    public Set<DataNodeAndShapes> instantiate(Node node, Shape shape, boolean forceApplyShape,
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
            if (ctx.hasInstanceForFocusNode(node) && !ctx.isNewShapeForFocusNode(node, shape)) {
                return Collections.emptySet();
            }
            if (logger.isDebugEnabled()) {
                logger.debug("processing node {} with shape {}", node, shape);
            }
            focusNodes = Collections.singleton(node);
        } else {
            focusNodes = focusNodes(ctx.getData(), shape);
        }
        if (focusNodes.isEmpty()) {
            return Collections.emptySet();
        }
        final Set<Node> finalFocusNodes = removeNodesInstantiatedInBaseContext(focusNodes)
                        .stream().filter(fnode -> {
                            if (instantiateViolatingNodes) {
                                return true;
                            }
                            // check if focusnode conforms to shape
                            ValidationContext vCtx = ctx.newValidationContext();
                            VLib.validateShape(vCtx, ctx.getData(), shape, fnode);
                            if (vCtx.hasViolation()) {
                                if (logger.isDebugEnabled()) {
                                    logger.debug("skipping node {} as it does not conform to shape {}", fnode,
                                                    shape.getShapeNode());
                                }
                                return false;
                            } else {
                                if (logger.isDebugEnabled()) {
                                    logger.debug("accepting node {} as it conforms to shape {}", fnode,
                                                    shape.getShapeNode());
                                }
                            }
                            return true;
                        }).collect(Collectors.toSet());
        String shapeURI = shape.getShapeNode().getURI();
        Set<Class<?>> classesForShape = ctx.getClassesForShape(shapeURI);
        if (classesForShape == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("No class found to instantiate for shape {}.",
                                shape.getShapeNode().getLocalName());
                logger.debug("Instantiation context:");
                logger.debug(ctx.getFormattedState());
            }
            throw new IllegalArgumentException(String.format(
                            "No class found to instantiate for shape %s, more information is logged on loglevel debug",
                            shape.getShapeNode().getLocalName()));
        }
        Set ret = classesForShape.parallelStream().map(classForShape -> {
            // our shape might reference other shapes via sh:node (maybe through other
            // operators such as sh:or)
            // * we remember them for wiring
            // * we return them as dependencies to instantiate
            Set<Shape> allRelevantShapes = ShapeUtils.getNodeShapes((NodeShape) shape, shapes);
            Map<Path, Set<PropertyShape>> propertyShapesPerPath = getPropertyShapesByPath(allRelevantShapes);
            allRelevantShapes.add(shape);
            return finalFocusNodes.parallelStream().map(focusNode -> {
                try {
                    Object instance = null;
                    if (logger.isDebugEnabled()) {
                        logger.debug("attempting to instantiate focus node {} with shape {}", focusNode,
                                        shape.getShapeNode());
                    }
                    instance = instantiate(shape, shapeURI, focusNode, classForShape, ctx);
                    if (instance == null) {
                        // we failed to instantiate an enum constant for the focus node of what looked
                        // like an enum shape.
                        // maybe another shape works better
                        return Collections.emptySet();
                    }
                    ctx.addInstanceForFocusNode(focusNode, instance);
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
                return propertyShapesPerPath
                                .entrySet()
                                .parallelStream()
                                .map(pathToShapes -> {
                                    Path path = pathToShapes.getKey();
                                    Set<PropertyShape> propertyShapes = pathToShapes.getValue();
                                    if (logger.isDebugEnabled()) {
                                        logger.debug("\tcollecting new focus nodes via path {} ", path);
                                    }
                                    Set<Node> valueNodes = ShaclPaths.valueNodes(ctx.getData(), focusNode, path);
                                    if (valueNodes.isEmpty()) {
                                        return Collections.emptySet();
                                    }
                                    return valueNodes
                                                    .stream()
                                                    .map(valueNode -> propertyShapes
                                                                    .stream()
                                                                    .map(ctx::getNodeShapesForPropertyShape)
                                                                    .map(nodeShapeNodes -> new DataNodeAndShapes(
                                                                                    valueNode,
                                                                                    nodeShapeNodes))
                                                                    .collect(Collectors.toSet()))
                                                    .reduce(SetUtils::union).orElse(Collections.emptySet());
                                }).reduce(SetUtils::union).orElse(Collections.emptySet());
            }).reduce(SetUtils::union).orElse(Collections.emptySet());
        }).reduce(SetUtils::union).orElse(Collections.emptySet());
        return ret;
    }

    public Set<Node> removeNodesInstantiatedInBaseContext(Collection<Node> focusNodes) {
        return focusNodes.stream()
                        .filter(Objects::nonNull)
                        .filter(n -> {
                            if (baseInstantiationContext != null
                                            && baseInstantiationContext.hasInstanceForFocusNode(n)) {
                                if (logger.isDebugEnabled()) {
                                    logger.debug("Skipping instantiation of node {} as it is already an instance in the base context",
                                                    n);
                                }
                                return false;
                            } else {
                                return true;
                            }
                        }).collect(Collectors.toSet());
    }

    public Object instantiate(Shape shape, String shapeURI, Node focusNode, Class<?> classForShape,
                    InstantiationContext ctx)
                    throws InstantiationException,
                    IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        Object instance;
        Class<Enum> type = (Class<Enum>) classForShape;
        if (type.isEnum()) {
            if (ShapeUtils.checkForEnumShape(shape).isEnumShape()) {
                return instantiateEnum(focusNode, type);
            }
        }
        return instantiateClass(focusNode, classForShape, ctx.getData());
    }

    public Object instantiateClass(Node focusNode, Class<?> classForShape, Graph graph)
                    throws InstantiationException,
                    IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        Object instance;
        instance = classForShape.getDeclaredConstructor(Node.class, Graph.class).newInstance(focusNode, graph);
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
        pathsFromPropertyShapes.parallelStream().forEach(path -> {
            Set<Node> valueNodes = ShaclPaths.valueNodes(ctx.getData(), focusNode, path);
            Set<Field> fieldsForPath = fieldsByPath.get(path);
            if (fieldsForPath != null && !fieldsForPath.isEmpty()) {
                if (valueNodes.size() > 0) {
                    for (Node valueNode : valueNodes) {
                        Set<Object> dependencyCandidates = ctx.getInstancesForFocusNode(valueNode);
                        if (dependencyCandidates == null) {
                            // dependency could be a blank node, literal or IRI that is not explicitly
                            // covered by a shape
                            if (valueNode.isLiteral()) {
                                dependencyCandidates = Collections.singleton(valueNode.getLiteralDatatype()
                                                .parse(valueNode.getLiteralLexicalForm()));
                            } else if (valueNode.isURI()) {
                                dependencyCandidates = Collections.singleton(URI.create(valueNode.getURI()));
                            }
                        }
                        Field field = null;
                        try {
                            Map<Field, Object> fieldsToDependencies = selectFieldByType(fieldsForPath, instance,
                                            dependencyCandidates);
                            if (fieldsToDependencies == null || fieldsToDependencies.size() == 0) {
                                throw new IllegalArgumentException(
                                                makeWiringErrorMessage(instance, focusNode, dependencyCandidates, null,
                                                                null)
                                                                + ": unable to identify appropriate field to set");
                            }
                            for (Map.Entry<Field, Object> fieldsToSet : fieldsToDependencies.entrySet()) {
                                field = fieldsToSet.getKey();
                                Object dependency = fieldsToSet.getValue();
                                setDependency(instance, field.getName(), dependency);
                                if (logger.isDebugEnabled()) {
                                    logger.debug("wired {} ",
                                                    makeWiringMessage(instance, focusNode, dependency, field));
                                }
                            }
                        } catch (Throwable e) {
                            throw new IllegalArgumentException(
                                            makeWiringErrorMessage(instance, focusNode, dependencyCandidates, field, e),
                                            e);
                        }
                    }
                }
            }
        });
    }

    private void extractPath(Map<Path, Set<Field>> fieldsByPath, Field field) {
        PropertyPath propertyPath = field.getAnnotation(PropertyPath.class);
        if (propertyPath == null) {
            return;
        }
        String propertyPathStr = propertyPath.value();
        if (propertyPathStr == null || propertyPathStr.trim().length() == 0) {
            return;
        }
        CollectionUtils.addToMultivalueMap(fieldsByPath, PathParser.parse(propertyPathStr, PrefixMapping.Standard),
                        field);
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
                        .flatMap(individualUri -> ctx.getInstancesForFocusNode(NodeFactory.createURI(individualUri))
                                        .stream())
                        .parallel()
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

    private Map<Field, Object> selectFieldByType(Set<Field> fields, Object instance, Set<Object> dependencyCandidates)
                    throws NoSuchFieldException {
        Map<Field, Object> mapped = new HashMap<>();
        for (Field field : fields) {
            for (Object dependency : dependencyCandidates) {
                if (field.getType().isAssignableFrom(dependency.getClass())) {
                    mapped.put(field, dependency);
                }
            }
        }
        if (mapped.size() == dependencyCandidates.size()) {
            return mapped;
        }
        for (Field field : fields) {
            if (mapped.containsKey(field)) {
                continue;
            }
            if (Set.class.isAssignableFrom(field.getType())) {
                try {
                    Type typeArg = ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
                    for (Object dependency : dependencyCandidates) {
                        if (typeArg instanceof Class && ((Class<?>) typeArg).isAssignableFrom(dependency.getClass())) {
                            mapped.put(field, dependency);
                        }
                    }
                } catch (ClassCastException e) {
                    throw e;
                }
            }
        }
        if (!mapped.isEmpty()) {
            return mapped;
        }
        throw new IllegalStateException(String.format(
                        "Could not find appropriate field for instance %s and dependency candidates %s",
                        instance, Arrays.toString(dependencyCandidates.toArray())));
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
                        .parallelStream()
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

    public int size() {
        return dataInstantiationContext.size();
    }
}

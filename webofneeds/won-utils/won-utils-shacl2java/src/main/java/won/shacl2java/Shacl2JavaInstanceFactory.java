package won.shacl2java;

import io.github.classgraph.*;
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
import won.shacl2java.runtime.model.GraphEntity;
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

public class Shacl2JavaInstanceFactory {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private Shapes shapes;
    private String[] packagesToScan;
    private InstantiationContext baseInstantiationContext;

    public Shacl2JavaInstanceFactory(Shapes shapes, String... packagesToScan) {
        this.shapes = shapes;
        this.packagesToScan = packagesToScan;
        InstantiationContext ctx = new InstantiationContext(shapes);
        scanPackages(ctx);
        instantiateAll(ctx);
        this.baseInstantiationContext = ctx;
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

    /**
     * Instantiates everything found in data.
     * Returns an {@link Accessor} object for obtaining the instances.
     *
     * @param data the data to load
     */
    public Accessor accessor(Graph data) {
        DerivedInstantiationContext dataContext = new DerivedInstantiationContext(data, baseInstantiationContext);
        instantiateAll(dataContext);
        return new Accessor(dataContext);
    }

    /**
     * Returns an {@link Accessor} object for obtaining the instances found in the base data.
     *
     * @return
     */
    public Accessor accessor() {
        return new Accessor();
    }

    public class Accessor {
        private Accessor() {
        }

        private Accessor(InstantiationContext dataInstantiationContext) {
            Accessor.this.dataInstantiationContext = dataInstantiationContext;
        }

        private InstantiationContext dataInstantiationContext;

        public int size() {
            return dataInstantiationContext.size();
        }

        public Set<Object> getInstances(String uri) {
            return getInstances(uri, false);
        }

        public Set<Object> getInstances(String uri, boolean includeBaseContext) {
            Set<Object> result = new HashSet<>();
            if (includeBaseContext) {
                result.addAll(baseInstantiationContext.getInstances(uri));
            }
            if (dataInstantiationContext != null) {
                result.addAll(dataInstantiationContext.getInstances(uri));
            }
            return result;
        }

        public <T> Set<T> getInstancesOfType(Class<T> type) {
            return getInstancesOfType(type, false);
        }

        public <T> Set<T> getInstancesOfType(Class<T> type, boolean includeBaseContext) {
            Set<T> result = new HashSet<>();
            if (includeBaseContext) {
                result.addAll(baseInstantiationContext.getInstancesOfType(type));
            }
            if (dataInstantiationContext != null) {
                result.addAll(dataInstantiationContext.getInstancesOfType(type));
            }
            return result;
        }

        public <T> Optional<T> getInstanceOfType(String uri, Class<T> type) {
            return getInstanceOfType(uri, type, false);
        }

        public <T> Optional<T> getInstanceOfType(String uri, Class<T> type,
                        boolean includeBaseContext) {
            if (includeBaseContext) {
                Optional<T> inst = baseInstantiationContext.getInstanceOfType(uri, type);
                if (inst.isPresent()) {
                    return inst;
                }
            }
            if (dataInstantiationContext != null) {
                return dataInstantiationContext.getInstanceOfType(uri, type);
            }
            return Optional.empty();
        }

        public Map<String, Set<Object>> getInstanceMap() {
            return getInstanceMap(false);
        }

        public Map<String, Set<Object>> getInstanceMap(boolean includeBaseContext) {
            Map<String, Set<Object>> result = new HashMap<>();
            if (includeBaseContext) {
                result.putAll(baseInstantiationContext.getInstanceMap());
            }
            if (dataInstantiationContext != null) {
                result.putAll(dataInstantiationContext.getInstanceMap());
            }
            return result;
        }

        public Collection<Object> getInstances() {
            return getInstances(false);
        }

        public Collection<Object> getInstances(boolean includeBaseContext) {
            Set<Object> result = new HashSet<>();
            if (includeBaseContext) {
                result.addAll(baseInstantiationContext.getInstances());
            }
            if (dataInstantiationContext == null) {
                result.addAll(dataInstantiationContext.getInstances());
            }
            return result;
        }
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
                        ctx.setClassForInstance(instance, instance.getClass());
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
                toInstantiate = shapeSet.stream().map(shape -> {
                    return instantiate(null, shape, false, ctx);
                }).reduce(CollectionUtils::union).orElseGet(() -> Collections.emptySet());
                toInstantiate = deduplicate(toInstantiate);
            } else {
                toInstantiate = toInstantiate.parallelStream().map(dataNodeAndShapes -> {
                    if (dataNodeAndShapes.getShapeNodes().isEmpty()) {
                        return StreamSupport
                                        .stream(shapes.spliterator(), true)
                                        .map(shape -> instantiate(dataNodeAndShapes.getDataNode(),
                                                        shape, false, ctx))
                                        .reduce(CollectionUtils::union).orElseGet(() -> Collections.emptySet());
                    } else {
                        return dataNodeAndShapes
                                        .getShapeNodes()
                                        .parallelStream()
                                        .map(shapeNode -> instantiate(dataNodeAndShapes.getDataNode(),
                                                        shapes.getShape(shapeNode), true, ctx))
                                        .reduce(CollectionUtils::union).orElseGet(() -> Collections.emptySet());
                    }
                }).reduce(CollectionUtils::union).orElseGet(() -> Collections.emptySet());
                toInstantiate = deduplicate(toInstantiate);
            }
        }
        ctx.getInstancesByNode().stream().forEach(entry -> {
            Node node = entry.getKey();
            Set<Object> instances = entry.getValue();
            for (Object instance : instances) {
                if (logger.isDebugEnabled()) {
                    logger.debug("wiring dependencies of instance {} ", node);
                }
                wireDependencies(instance, ctx);
            }
        });
    }

    /**
     * Creates all instances for the given node / shape combination.
     *
     * @param node  may be null, in which case all target nodes of the shape are
     *              chosen.
     * @param shape
     * @return the set of nodes reached during instantiation that have not been
     * instantiated yet
     */
    private Set<DataNodeAndShapes> instantiate(Node node, Shape shape, boolean forceApplyShape,
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
                                                    .reduce(CollectionUtils::union).orElse(Collections.emptySet());
                                }).reduce(CollectionUtils::union).orElse(Collections.emptySet());
            }).reduce(CollectionUtils::union).orElse(Collections.emptySet());
        }).reduce(CollectionUtils::union).orElse(Collections.emptySet());
        return ret;
    }

    private Set<Node> removeNodesInstantiatedInBaseContext(Collection<Node> focusNodes) {
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

    private Object instantiate(Shape shape, String shapeURI, Node focusNode, Class<?> classForShape,
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

    private Object instantiateClass(Node focusNode, Class<?> classForShape, Graph graph)
                    throws InstantiationException,
                    IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        Object instance;
        instance = classForShape.getDeclaredConstructor(Node.class, Graph.class).newInstance(focusNode, graph);
        return instance;
    }

    private Object instantiateEnum(Node focusNode, Class<Enum> type) {
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
        Class<?> type = instance.getClass();
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
                            dependencyCandidates = new HashSet<>();
                        }
                        // dependency could be a blank node, literal or IRI that is not explicitly
                        // covered by a shape
                        if (valueNode.isLiteral()) {
                            dependencyCandidates.add(valueNode.getLiteralDatatype()
                                            .parse(valueNode.getLiteralLexicalForm()));
                        } else if (valueNode.isURI()) {
                            dependencyCandidates.add(URI.create(valueNode.getURI()));
                        }
                        Field field = null;
                        try {
                            SatisfiedFieldDependency fieldDependency = selectFieldByType(fieldsForPath, instance,
                                            dependencyCandidates);
                            if (fieldDependency == null) {
                                throw new IllegalArgumentException(
                                                makeWiringErrorMessage(instance, focusNode, dependencyCandidates, null,
                                                                null)
                                                                + ": unable to identify appropriate field to set");
                            }
                            field = fieldDependency.getField();
                            Object dependency = fieldDependency.getDependency();
                            setDependency(instance, field.getName(), dependency);
                            if (logger.isDebugEnabled()) {
                                logger.debug("wired {} ",
                                                makeWiringMessage(instance, focusNode, dependency, field));
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

    private static class SatisfiedFieldDependency {
        private final Field field;
        private final Object dependency;

        public SatisfiedFieldDependency(Field field, Object dependency) {
            this.field = field;
            this.dependency = dependency;
        }

        public Object getDependency() {
            return dependency;
        }

        public Field getField() {
            return field;
        }
    }

    /**
     * Finds the best dependency from the object's fields for the set of
     * dependencies. The priority order is:
     * <ol>
     * <li>instances of (subclasses of) GraphEntity,</li>
     * <li>Sets of GraphEntity</li>
     * <li>URIs</li>
     * <li>other values</li>
     * </ol>
     * For any set of candidates, only one field and one candidate is selected.
     *
     * @param fields
     * @param instance
     * @param dependencyCandidates
     * @return
     * @throws NoSuchFieldException
     */
    private SatisfiedFieldDependency selectFieldByType(Set<Field> fields, Object instance,
                    Set<Object> dependencyCandidates)
                    throws NoSuchFieldException {
        Optional<SatisfiedFieldDependency> result = dependencyCandidates
                        .stream()
                        .filter(dep -> GraphEntity.class.isAssignableFrom(dep.getClass()) || dep.getClass().isEnum())
                        .map(dep -> fields
                                        .stream()
                                        .filter(f -> isAssignableTypeOrSetOf(f,
                                                        dep.getClass()))
                                        .findAny()
                                        .map(f -> new SatisfiedFieldDependency(f, dep))
                                        .orElse(null))
                        .filter(Objects::nonNull)
                        .findAny();
        if (result.isPresent()) {
            return result.get();
        }
        // no GraphEntity or Set<? extends GraphEntity> found, try any other type
        result = dependencyCandidates
                        .stream()
                        .map(dep -> fields
                                        .stream()
                                        .filter(f -> isAssignableTypeOrSetOf(f,
                                                        dep.getClass()))
                                        .findAny()
                                        .map(f -> new SatisfiedFieldDependency(f, dep))
                                        .orElse(null))
                        .filter(Objects::nonNull)
                        .findAny();
        if (result.isPresent()) {
            return result.get();
        }
        throw new IllegalStateException(String.format(
                        "Could not find appropriate field for instance %s and dependency candidates %s",
                        instance, Arrays.toString(dependencyCandidates.toArray())));
    }

    private boolean isAssignableTypeOrSetOf(Field field, Class<?> dependencyType) {
        if (field.getType().isAssignableFrom(dependencyType)) {
            return true;
        }
        if (Set.class.isAssignableFrom(field.getType())) {
            Type typeArg = ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
            if (typeArg instanceof Class && ((Class<?>) typeArg).isAssignableFrom(dependencyType)) {
                return true;
            }
        }
        return false;
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
                            String.format("Error using setter/adder method to set %s.%s = %s", instance,
                                            fieldName, dependency),
                            e);
        }
        if (logger.isDebugEnabled()) {
            logger.debug(String.format("Did not find setter/adder method to set %s.%s = %s", instance, fieldName,
                            dependency));
        }
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
}

package won.shacl2java.instantiation;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.shacl.Shapes;
import org.apache.jena.shacl.engine.ValidationContext;
import org.apache.jena.shacl.parser.PropertyShape;
import org.apache.jena.shacl.parser.Shape;
import won.shacl2java.util.ShapeUtils;

import static won.shacl2java.util.CollectionUtils.addToMultivalueConcurrentHashMap;

public class InstantiationContext {
    private ConcurrentHashMap<String, Set<Class<?>>> shapeClasses = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Object, Class<?>> instanceToClass = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Object, Node> instanceToFocusNode = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Node, Set<Shape>> focusNodeToShapes = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Node, Set<Object>> focusNodeToInstance = new ConcurrentHashMap<>();
    private ConcurrentHashMap<PropertyShape, Set<Node>> propertyShapeToNodeShape = new ConcurrentHashMap<>();
    protected Graph data;
    protected Shapes shapes;

    protected InstantiationContext() {
        this.data = null;
        this.shapes = null;
    }

    public InstantiationContext(Shapes shapes) {
        this.data = shapes.getGraph();
        this.shapes = shapes;
    }

    public int size() {
        return focusNodeToInstance.size();
    }

    public <T> Set<T> getInstanceOfType(Class<T> type) {
        return this.focusNodeToInstance.values().stream()
                        .flatMap(Collection::stream)
                        .filter(v -> type.isAssignableFrom(v.getClass()))
                        .map(v -> type.cast(v))
                        .collect(Collectors.toSet());
    }

    public Set<Object> getInstances(String uri) {
        return focusNodeToInstance.get(NodeFactory.createURI(uri));
    }

    public <T> Optional<T> getInstanceOfType(String uri, Class<T> type) {
        Set<Object> instances = getInstances(uri);
        if (instances == null)
            return Optional.empty();
        return instances.stream()
                        .filter(v -> v.getClass().isAssignableFrom(type))
                        .map(v -> type.cast(v))
                        .findFirst(); // there cannot be more than one. If there
    }

    public Map<String, Set<Object>> getInstanceMap() {
        return Collections.unmodifiableMap(this.focusNodeToInstance
                        .entrySet()
                        .stream()
                        .collect(
                                        Collectors.toMap(
                                                        e -> e.getKey().toString(),
                                                        e -> e.getValue())));
    }

    public Set<Map.Entry<Node, Set<Object>>> getInstancesByNode() {
        return Collections.unmodifiableSet(focusNodeToInstance.entrySet());
    }

    public Collection<Object> getInstances() {
        return Collections.unmodifiableCollection(
                        focusNodeToInstance.values().stream().flatMap(Collection::stream).collect(Collectors.toSet()));
    }

    public Set<Class<?>> getClassesForShape(String shapeUri) {
        return shapeClasses.get(shapeUri);
    }

    public boolean hasClassForShape(String shapeUri) {
        return shapeClasses.containsKey(shapeUri);
    }

    public void addClassForShape(String shapeUri, Class<?> type) {
        Class<?> existing = null;
        addToMultivalueConcurrentHashMap(shapeClasses, shapeUri, type);
    }

    public Class<?> getClassForInstance(Object instance) {
        return instanceToClass.get(instance);
    }

    public boolean hasClassForInstance(Object instance) {
        return instanceToClass.containsKey(instance);
    }

    public void setClassForInstance(Object instance, Class<?> type) {
        Class<?> existing = null;
        existing = instanceToClass.get(instance);
        if (existing == null) {
            // we have to check again, a parrallel thread may have put something here.
            // If so, exisiting is that previous value
            existing = instanceToClass.putIfAbsent(instance, type);
        }
        if (existing != null) {
            // we only have a problem if the values differ
            if (!existing.equals(type)) {
                throw new IllegalStateException(String.format(
                                "Forbidden: replacing the existing instance-type mapping for %s - %s with %s",
                                instance, existing, type));
            }
        }
    }

    public Node getFocusNodeForInstance(Object instance) {
        return instanceToFocusNode.get(instance);
    }

    public boolean hasFocusNodeForInstance(Object instance) {
        return instanceToFocusNode.containsKey(instance);
    }

    public void setFocusNodeForInstance(Object instance, Node focusNode) {
        Node existing = null;
        existing = instanceToFocusNode.get(instance);
        if (existing == null) {
            // we have to check again, a parrallel thread may have put something here.
            // If so, exisiting is that previous value
            existing = instanceToFocusNode.putIfAbsent(instance, focusNode);
        }
        if (existing != null) {
            // we only have a problem if the values differ
            if (!existing.equals(focusNode)) {
                throw new IllegalStateException(String.format(
                                "Forbidden: replacing the existing instance-focusNode mapping for %s - %s with %s",
                                instance, existing, focusNode));
            }
        }
    }

    public Set<Object> getInstancesForFocusNode(Node focusNode) {
        return focusNodeToInstance.get(focusNode);
    }

    public boolean hasInstanceForFocusNode(Node focusNode) {
        return focusNodeToInstance.containsKey(focusNode);
    }

    public void addInstanceForFocusNode(Node focusNode, Object instance) {
        addToMultivalueConcurrentHashMap(focusNodeToInstance, focusNode, instance);
    }

    public Set<Shape> getShapesForFocusNode(Node node) {
        return focusNodeToShapes.get(node);
    }

    public boolean hasShapesForFocusNode(Node node) {
        return focusNodeToShapes.containsKey(node);
    }

    public boolean hasShapeForFocusNode(Node node, Shape shape) {
        Set<Shape> shapes = focusNodeToShapes.get(node);
        if (shapes == null || shapes.size() == 0) {
            return false;
        }
        return shapes.contains(shape);
    }

    public boolean isNewShapeForFocusNode(Node node, Shape shape) {
        return addToMultivalueConcurrentHashMap(focusNodeToShapes, node, shape);
    }

    public void addShapeForFocusNode(Node node, Shape shape) {
        addToMultivalueConcurrentHashMap(focusNodeToShapes, node, shape);
    }

    public void addShapesForFocusNode(Node node, Set<Shape> shapes) {
        addToMultivalueConcurrentHashMap(focusNodeToShapes, node, shapes);
    }

    public Graph getData() {
        return data;
    };

    public Set<Node> getNodeShapesForPropertyShape(PropertyShape propertyShape) {
        return propertyShapeToNodeShape.computeIfAbsent(propertyShape,
                        pshape -> ShapeUtils.getShNodeShapes(pshape, shapes)
                                        .parallelStream()
                                        .filter(s -> !s.getShapeNode().isBlank())
                                        .map(Shape::getShapeNode)
                                        .collect(Collectors.toSet()));
    }

    public String getFormattedState() {
        StringBuilder sb = new StringBuilder();
        sb.append("InstantiationContext\n");
        sb.append("Shape-class mapping:\n");
        shapeClasses.entrySet().forEach(e -> sb.append("\t")
                        .append(e.getKey())
                        .append(" -> ")
                        .append(e.getValue())
                        .append("\n"));
        sb.append("uri-instance mapping:\n");
        focusNodeToInstance.entrySet().forEach(e -> sb.append("\t")
                        .append(e.getKey())
                        .append(" -> ")
                        .append(e.getValue())
                        .append("\n"));
        sb.append("uri-shape mapping:\n");
        focusNodeToShapes.entrySet().forEach(e -> sb.append("\t")
                        .append(e.getKey())
                        .append(" -> ")
                        .append(e.getValue())
                        .append("\n"));
        return sb.toString();
    }

    public ValidationContext newValidationContext() {
        return ValidationContext.create(shapes, data);
    }
}

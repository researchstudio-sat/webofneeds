package won.shacl2java.sourcegen;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.shacl.parser.Shape;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static won.shacl2java.util.CollectionUtils.addToMultivalueMap;

public class InstantiationContext {
    private ConcurrentHashMap<String, Class<?>> shapeClasses = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Object, Class<?>> instanceToClass = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Object, Node> instanceToFocusNode = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Node, Set<Shape>> focusNodeToShapes = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Node, Object> focusNodeToInstance = new ConcurrentHashMap<>();
    private Graph data;

    public InstantiationContext(Graph data) {
        this.data = data;
    }

    public int size() {
        return focusNodeToInstance.size();
    }

    public <T> Set<T> getEntitiesOfType(Class<T> type) {
        return this.focusNodeToInstance.values().stream()
                        .filter(v -> v.getClass().isAssignableFrom(type))
                        .map(v -> type.cast(v))
                        .collect(Collectors.toSet());
    }

    public Map<String, Object> getInstanceMap() {
        return this.focusNodeToInstance
                        .entrySet()
                        .stream()
                        .collect(
                                        Collectors.toMap(
                                                        e -> e.getKey().toString(),
                                                        e -> e.getValue()));
    }

    public void withMappedInstances(BiConsumer<Node, Object> consumer) {
        for (Map.Entry<Node, Object> nodeObjectEntry : this.focusNodeToInstance.entrySet()) {
            consumer.accept(nodeObjectEntry.getKey(), nodeObjectEntry.getValue());
        }
    }

    public Set<Map.Entry<Node, Object>> getMappedInstances() {
        return Collections.unmodifiableSet(focusNodeToInstance.entrySet());
    }

    public Collection<Object> getInstances() {
        return Collections.unmodifiableCollection(focusNodeToInstance.values());
    }

    public Class<?> getClassForShape(String shapeUri) {
        return shapeClasses.get(shapeUri);
    }

    public boolean hasClassForShape(String shapeUri) {
        return shapeClasses.containsKey(shapeUri);
    }

    public void setClassForShape(String shapeUri, Class<?> type) {
        Class<?> existing = null;
        if ((existing = shapeClasses.put(shapeUri, type)) != null) {
            if (!existing.equals(type)) {
                throw new IllegalStateException(
                                String.format("Forbidden: replacing the existing shape-class mapping for %s - %s with %s",
                                                shapeUri, existing.getName(), type.getName()));
            }
        }
    }

    public Class<?> getClassForInstance(Object instance) {
        return instanceToClass.get(instance);
    }

    public boolean hasClassForInstance(Object instance) {
        return instanceToClass.containsKey(instance);
    }

    public void setClassForInstance(Object instance, Class<?> type) {
        Class<?> existing = null;
        if ((existing = instanceToClass.put(instance, type)) != null) {
            if (!existing.equals(type)) {
                throw new IllegalStateException(String.format(
                                "Forbidden: replacing the existing instance-class mapping for %s - %s with %s",
                                instance,
                                existing.getName(), type.getName()));
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
        if ((existing = instanceToFocusNode.put(instance, focusNode)) != null) {
            if (!existing.equals(focusNode)) {
                throw new IllegalStateException(String.format(
                                "Forbidden: replacing the existing instance-focusNode mapping for %s - %s with %s",
                                instance, existing, focusNode));
            }
        }
    }

    public Object getInstanceForFocusNode(Node focusNode) {
        return focusNodeToInstance.get(focusNode);
    }

    public boolean hasInstanceForFocusNode(Node focusNode) {
        return focusNodeToInstance.containsKey(focusNode);
    }

    public void setInstanceForFocusNode(Node focusNode, Object instance) {
        Object existing = null;
        if ((existing = focusNodeToInstance.put(focusNode, instance)) != null) {
            if (!existing.equals(instance)) {
                throw new IllegalStateException(String.format(
                                "Forbidden: replacing the existing focusNode-instance mapping for %s - %s with %s",
                                focusNode, existing, instance));
            }
        }
    }

    public Set<Shape> getShapesForFocusNode(Node node) {
        return focusNodeToShapes.get(node);
    }

    public boolean hasShapesForFocusNode(Node node) {
        return focusNodeToShapes.containsKey(node);
    }

    public void addShapeForFocusNode(Node node, Shape shape) {
        addToMultivalueMap(focusNodeToShapes, node, shape);
    }

    public void addShapesForFocusNode(Node node, Set<Shape> shapes) {
        addToMultivalueMap(focusNodeToShapes, node, shapes);
    }

    public Graph getData() {
        return data;
    };
}

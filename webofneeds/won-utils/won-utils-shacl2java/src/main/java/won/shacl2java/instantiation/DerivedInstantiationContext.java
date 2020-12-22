package won.shacl2java.instantiation;

import java.util.Objects;
import java.util.Set;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.shacl.engine.ValidationContext;
import org.apache.jena.shacl.parser.PropertyShape;
import org.apache.jena.shacl.parser.Shape;

public class DerivedInstantiationContext extends InstantiationContext {
    private InstantiationContext parentContext;

    public DerivedInstantiationContext(Graph data, InstantiationContext parentContext) {
        super();
        Objects.requireNonNull(data);
        Objects.requireNonNull(parentContext);
        this.data = data;
        this.parentContext = parentContext;
    }

    @Override
    public Set<Class<?>> getClassesForShape(String shapeUri) {
        return parentContext.getClassesForShape(shapeUri);
    }

    @Override
    public boolean hasClassForShape(String shapeUri) {
        return parentContext.hasClassForShape(shapeUri);
    }

    @Override
    public Class<?> getClassForInstance(Object instance) {
        if (super.hasClassForInstance(instance)) {
            return super.getClassForInstance(instance);
        }
        return parentContext.getClassForInstance(instance);
    }

    @Override
    public boolean hasClassForInstance(Object instance) {
        return super.hasClassForInstance(instance) || parentContext.hasClassForInstance(instance);
    }

    @Override
    public Node getFocusNodeForInstance(Object instance) {
        if (super.hasFocusNodeForInstance(instance)) {
            return super.getFocusNodeForInstance(instance);
        }
        return parentContext.getFocusNodeForInstance(instance);
    }

    @Override
    public boolean hasFocusNodeForInstance(Object instance) {
        return super.hasFocusNodeForInstance(instance) || parentContext.hasFocusNodeForInstance(instance);
    }

    @Override
    public Set<Object> getInstancesForFocusNode(Node focusNode) {
        if (super.hasInstanceForFocusNode(focusNode)) {
            return super.getInstancesForFocusNode(focusNode);
        }
        return parentContext.getInstancesForFocusNode(focusNode);
    }

    @Override
    public boolean hasInstanceForFocusNode(Node focusNode) {
        return super.hasInstanceForFocusNode(focusNode) || parentContext.hasInstanceForFocusNode(focusNode);
    }

    @Override
    public Set<Shape> getShapesForFocusNode(Node node) {
        if (super.hasShapesForFocusNode(node)) {
            return super.getShapesForFocusNode(node);
        }
        return parentContext.getShapesForFocusNode(node);
    }

    @Override
    public boolean hasShapesForFocusNode(Node node) {
        return super.hasShapesForFocusNode(node) || parentContext.hasShapesForFocusNode(node);
    }

    @Override
    public boolean hasShapeForFocusNode(Node node, Shape shape) {
        return super.hasShapeForFocusNode(node, shape) || parentContext.hasShapeForFocusNode(node, shape);
    }

    @Override
    public Set<Object> getInstances(String uri) {
        Node uriNode = NodeFactory.createURI(uri);
        if (super.hasInstanceForFocusNode(uriNode)) {
            return super.getInstances(uri);
        }
        return parentContext.getInstances(uri);
    }

    @Override
    public Set<Node> getNodeShapesForPropertyShape(PropertyShape propertyShape) {
        return parentContext.getNodeShapesForPropertyShape(propertyShape);
    }

    @Override
    public String getFormattedState() {
        StringBuilder sb = new StringBuilder();
        sb.append(super.getFormattedState());
        if (parentContext != null) {
            sb.append("\nparent context:\n")
                            .append(parentContext.getFormattedState());
        }
        return sb.toString();
    }

    @Override
    public ValidationContext newValidationContext() {
        return ValidationContext.create(parentContext.shapes, this.data);
    }
}

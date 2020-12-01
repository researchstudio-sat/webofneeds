package won.shacl2java.sourcegen;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.shacl.parser.Shape;

import java.util.Objects;
import java.util.Set;

public class DerivedInstantiationContext extends InstantiationContext {
    private InstantiationContext parentContext;

    public DerivedInstantiationContext(Graph data, InstantiationContext parentContext) {
        super(data);
        Objects.requireNonNull(data);
        Objects.requireNonNull(parentContext);
        this.parentContext = parentContext;
    }

    @Override
    public Class<?> getClassForShape(String shapeUri) {
        return parentContext.getClassForShape(shapeUri);
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
    public Object getInstanceForFocusNode(Node focusNode) {
        if (super.hasInstanceForFocusNode(focusNode)) {
            return super.getInstanceForFocusNode(focusNode);
        }
        return parentContext.getInstanceForFocusNode(focusNode);
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
}

package won.shacl2java.constraints;

import org.apache.jena.graph.Node;
import org.apache.jena.shacl.Shapes;
import org.apache.jena.shacl.parser.*;

import java.util.HashSet;
import java.util.Set;

public class AllowedConcretePropertyValueShapesCollector implements ShapeVisitor {
    private Set<Shape> shapes = null;
    private boolean negated = false;

    public AllowedConcretePropertyValueShapesCollector() {
    }

    public Set<Shape> getShapes() {
        return shapes;
    }

    @Override
    public void visit(PropertyShape propertyShape) {
    }

    @Override
    public void visit(NodeShape nodeShape) {
    }
}

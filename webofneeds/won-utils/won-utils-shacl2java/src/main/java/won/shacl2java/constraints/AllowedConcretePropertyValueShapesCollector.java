package won.shacl2java.constraints;

import java.util.Set;
import org.apache.jena.shacl.parser.NodeShape;
import org.apache.jena.shacl.parser.PropertyShape;
import org.apache.jena.shacl.parser.Shape;
import org.apache.jena.shacl.parser.ShapeVisitor;

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

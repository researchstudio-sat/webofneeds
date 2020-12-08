package won.shacl2java.constraints;

import org.apache.jena.shacl.engine.constraint.ConstraintOpN;
import org.apache.jena.shacl.parser.*;

import java.util.HashSet;
import java.util.Set;

public class ShPropertyShapeCollector extends ConstraintVisitorBase {
    private Set<PropertyShape> propertyShapes = new HashSet<>();

    public ShPropertyShapeCollector() {
    }

    public Set<PropertyShape> getPropertyShapes() {
        return propertyShapes;
    }

    public static Set<PropertyShape> collectFrom(Shape shape) {
        ShPropertyShapeCollector coll = new ShPropertyShapeCollector();
        coll.visitRecursively(shape);
        return coll.getPropertyShapes();
    }

    private void visitRecursively(Shape shape) {
        for (Constraint constraint : shape.getConstraints()) {
            visitRecursively(constraint);
        }
        propertyShapes.addAll(shape.getPropertyShapes());
    }

    private void visitRecursively(Constraint constraint) {
        if (constraint instanceof ConstraintOpN) {
            ((ConstraintOpN) constraint).getOthers().forEach(subShape -> {
                visitRecursively(subShape);
            });
        }
        constraint.visit(this);
    }
}

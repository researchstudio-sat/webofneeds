package won.shacl2java.constraints;

import org.apache.jena.graph.Node;
import org.apache.jena.shacl.engine.constraint.ConstraintOp1;
import org.apache.jena.shacl.engine.constraint.ConstraintOpN;
import org.apache.jena.shacl.parser.Constraint;
import org.apache.jena.shacl.parser.ConstraintVisitor;
import org.apache.jena.shacl.parser.Shape;

import java.util.HashSet;
import java.util.Set;

public class ConstraintVisitorAlgorithm {
    public static void visitBreadthFirst(Shape shape, ConstraintVisitor visitor) {
        visitRecursively(shape, visitor, false);
    }

    public static void visitBreadthFirst(Constraint constraint, ConstraintVisitor visitor) {
        visitRecursively(constraint, visitor, false);
    }

    public static void visitDepthFirst(Shape shape, ConstraintVisitor visitor) {
        visitRecursively(shape, visitor, true);
    }

    public static void visitDepthFirst(Constraint constraint, ConstraintVisitor visitor) {
        visitRecursively(constraint, visitor, true);
    }

    private static void visitRecursively(Shape shape, ConstraintVisitor visitor, boolean depthFirst) {
        visitRecursively(shape, visitor, depthFirst, new HashSet<>());
    }

    private static void visitRecursively(Shape shape, ConstraintVisitor visitor, boolean depthFirst,
                    Set<Node> visited) {
        visited.add(shape.getShapeNode());
        for (Constraint constraint : shape.getConstraints()) {
            visitRecursively(constraint, visitor, depthFirst, visited);
        }
    }

    private static void visitRecursively(Constraint constraint, ConstraintVisitor visitor, boolean depthFirst) {
        visitRecursively(constraint, visitor, depthFirst, new HashSet<>());
    }

    private static void visitRecursively(Constraint constraint, ConstraintVisitor visitor, boolean depthFirst,
                    Set<Node> visited) {
        if (!depthFirst) {
            constraint.visit(visitor);
        }
        if (constraint instanceof ConstraintOp1) {
            Shape other = ((ConstraintOp1) constraint).getOther();
            if (!visited.contains(other)) {
                visitRecursively(other, visitor, depthFirst, visited);
            }
        }
        if (constraint instanceof ConstraintOpN) {
            ((ConstraintOpN) constraint).getOthers().forEach(subShape -> {
                if (!visited.contains(subShape)) {
                    visitRecursively(subShape, visitor, depthFirst, visited);
                }
            });
        }
        if (depthFirst) {
            constraint.visit(visitor);
        }
    }

    public static void visitShallow(Shape shape, ConstraintVisitor visitor) {
        for (Constraint constraint : shape.getConstraints()) {
            constraint.visit(visitor);
        }
    }
}

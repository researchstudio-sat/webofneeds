package won.shacl2java.constraints;

import java.util.HashSet;
import java.util.Set;
import org.apache.jena.graph.Node;
import org.apache.jena.shacl.engine.constraint.ConstraintOp1;
import org.apache.jena.shacl.engine.constraint.ConstraintOpN;
import org.apache.jena.shacl.engine.constraint.ShNode;
import org.apache.jena.shacl.engine.constraint.ShNot;
import org.apache.jena.shacl.parser.Constraint;
import org.apache.jena.shacl.parser.ConstraintVisitor;
import org.apache.jena.shacl.parser.ConstraintVisitorBase;
import org.apache.jena.shacl.parser.NodeShape;
import org.apache.jena.shacl.parser.Shape;

/**
 * Collects all node shapes that apply to the origin node shape (i.e., shapes
 * referenced via or/and/xor/xone/node(anon) etc.)
 */
public class NodeShapeCollector extends ConstraintVisitorBase {
    private Set<Node> shapeNodes = null;
    private boolean negated = false;

    public NodeShapeCollector() {
    }

    public Set<Node> getShapeNodes() {
        return shapeNodes;
    }

    @Override
    public void visit(ShNot shNot) {
        negated = !negated;
    }

    public Set<Node> collectFrom(NodeShape shape) {
        shapeNodes = new HashSet<>();
        visitRecursively(shape, this);
        return shapeNodes;
    }

    private void visitRecursively(Shape shape, ConstraintVisitor visitor) {
        if (!negated) {
            shapeNodes.add(shape.getShapeNode());
        }
        for (Constraint constraint : shape.getConstraints()) {
            visitRecursively(constraint, visitor);
        }
    }

    private void visitRecursively(Constraint constraint, ConstraintVisitor visitor) {
        if (constraint instanceof ConstraintOp1) {
            // we don't want to follow ShNode if it references a named shape - we just
            // remember which node is referenced in that case
            if (constraint instanceof ShNode) {
                if (shouldRecurseInto((ShNode) constraint)) {
                    visitRecursively(((ConstraintOp1) constraint).getOther(), visitor);
                }
            } else {
                visitRecursively(((ConstraintOp1) constraint).getOther(), visitor);
            }
        }
        if (constraint instanceof ConstraintOpN) {
            ((ConstraintOpN) constraint).getOthers().forEach(subShape -> {
                visitRecursively(subShape, visitor);
            });
        }
        constraint.visit(visitor);
    }

    private boolean shouldRecurseInto(ShNode constraint) {
        return true || constraint.getOther().getShapeNode().isBlank();
    }
}

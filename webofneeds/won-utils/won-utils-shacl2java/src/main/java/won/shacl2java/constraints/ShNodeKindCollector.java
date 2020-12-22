package won.shacl2java.constraints;

import java.util.HashSet;
import java.util.Set;
import org.apache.jena.graph.Node;
import org.apache.jena.shacl.engine.constraint.NodeKindConstraint;
import org.apache.jena.shacl.engine.constraint.ShNot;
import org.apache.jena.shacl.parser.Constraint;
import org.apache.jena.shacl.parser.ConstraintVisitorBase;
import org.apache.jena.shacl.parser.Shape;

public class ShNodeKindCollector extends ConstraintVisitorBase {
    private Set<Node> kinds = null;
    private boolean negated = false;

    public ShNodeKindCollector() {
    }

    public Set<Node> getShapeNodes() {
        return kinds;
    }

    @Override
    public void visit(NodeKindConstraint nodeKindConstraint) {
        if (!negated) {
            kinds.add(nodeKindConstraint.getKind());
        }
    }

    @Override
    public void visit(ShNot shNot) {
        negated = !negated;
    }

    public Set<Node> collectFrom(Shape shape) {
        kinds = new HashSet<>();
        ConstraintVisitorAlgorithm.visitBreadthFirst(shape, this);
        return kinds;
    }

    public Set<Node> collectFrom(Constraint constraint) {
        kinds = new HashSet<>();
        ConstraintVisitorAlgorithm.visitBreadthFirst(constraint, this);
        return kinds;
    }
}

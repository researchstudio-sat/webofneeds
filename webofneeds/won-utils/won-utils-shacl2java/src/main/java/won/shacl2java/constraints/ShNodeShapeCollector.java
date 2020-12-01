package won.shacl2java.constraints;

import org.apache.jena.graph.Node;
import org.apache.jena.shacl.engine.constraint.ShNode;
import org.apache.jena.shacl.engine.constraint.ShNot;
import org.apache.jena.shacl.parser.Constraint;
import org.apache.jena.shacl.parser.Shape;

import java.util.HashSet;
import java.util.Set;

public class ShNodeShapeCollector implements ConstraintVisitor {
    private Set<Node> shapeNodes = null;
    private boolean negated = false;

    public ShNodeShapeCollector() {
    }

    public Set<Node> getShapeNodes() {
        return shapeNodes;
    }

    @Override
    public void visit(ShNode shNode) {
        if (!negated) {
            shapeNodes.add(shNode.getOther().getShapeNode());
        }
    }

    @Override
    public void visit(ShNot shNot) {
        negated = !negated;
    }

    public Set<Node> collectFrom(Shape shape) {
        shapeNodes = new HashSet<>();
        ConstraintVisitorAlgorithm.visitBreadthFirst(shape, this);
        return shapeNodes;
    }

    public Set<Node> collectFrom(Constraint constraint) {
        shapeNodes = new HashSet<>();
        ConstraintVisitorAlgorithm.visitBreadthFirst(constraint, this);
        return shapeNodes;
    }
}

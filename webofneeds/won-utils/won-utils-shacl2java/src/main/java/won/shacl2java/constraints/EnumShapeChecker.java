package won.shacl2java.constraints;

import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.shacl.engine.constraint.ClosedConstraint;
import org.apache.jena.shacl.engine.constraint.DatatypeConstraint;
import org.apache.jena.shacl.engine.constraint.InConstraint;
import org.apache.jena.shacl.engine.constraint.NodeKindConstraint;
import org.apache.jena.shacl.parser.ConstraintVisitor;
import org.apache.jena.shacl.parser.ConstraintVisitorBase;
import org.apache.jena.shacl.parser.Shape;

import java.util.List;

public class EnumShapeChecker extends ConstraintVisitorBase {
    private List<Node> values;
    private RDFDatatype datatype;
    private Node nodeKind;
    private Shape shape;
    private boolean closed = false;

    private EnumShapeChecker(Shape shape) {
        this.shape = shape;
    }

    public List<Node> getValues() {
        return values;
    }

    public RDFDatatype getDatatype() {
        return datatype;
    }

    public Node getNodeKind() {
        return nodeKind;
    }

    public boolean isEnumShape() {
        return closed && shape != null && shape.isNodeShape() && values != null && values.size() > 0;
    }

    public Shape getShape() {
        return shape;
    }

    @Override
    public void visit(NodeKindConstraint nodeKindConstraint) {
        if (!nodeKindConstraint.isCanBeBlankNode()) {
            this.nodeKind = nodeKindConstraint.getKind();
        }
    }

    @Override
    public void visit(InConstraint inConstraint) {
        this.values = inConstraint.getValues();
    }

    @Override
    public void visit(DatatypeConstraint datatypeConstraint) {
        this.datatype = datatypeConstraint.getRDFDatatype();
    }

    @Override
    public void visit(ClosedConstraint closedConstraint) {
        this.closed = closedConstraint.isActive();
    }

    public static EnumShapeChecker check(Shape shape) {
        EnumShapeChecker checker = new EnumShapeChecker(shape);
        ConstraintVisitorAlgorithm.visitShallow(shape, checker);
        return checker;
    }
}

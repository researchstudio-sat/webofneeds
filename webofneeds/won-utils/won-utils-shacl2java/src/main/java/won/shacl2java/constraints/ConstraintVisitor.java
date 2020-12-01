package won.shacl2java.constraints;

import org.apache.jena.shacl.engine.constraint.*;
import org.apache.jena.shacl.parser.Constraint;

public interface ConstraintVisitor {
    default void visit(Constraint constraint) {
        if (constraint instanceof ShNode) {
            visit((ShNode) constraint);
        } else if (constraint instanceof ShNot) {
            visit((ShNot) constraint);
        } else if (constraint instanceof ShOr) {
            visit((ShOr) constraint);
        } else if (constraint instanceof ShXone) {
            visit((ShXone) constraint);
        } else if (constraint instanceof ShAnd) {
            visit((ShAnd) constraint);
        } else if (constraint instanceof InConstraint) {
            visit((InConstraint) constraint);
        } else if (constraint instanceof HasValueConstraint) {
            visit((HasValueConstraint) constraint);
        } else if (constraint instanceof ClassConstraint) {
            visit((ClassConstraint) constraint);
        } else if (constraint instanceof DatatypeConstraint) {
            visit((DatatypeConstraint) constraint);
        } else if (constraint instanceof MinCount) {
            visit((MinCount) constraint);
        } else if (constraint instanceof MaxCount) {
            visit((MaxCount) constraint);
        } else if (constraint instanceof NodeKindConstraint) {
            visit((NodeKindConstraint) constraint);
        } else if (constraint instanceof ClosedConstraint) {
            visit((ClosedConstraint) constraint);
        } else if (constraint instanceof JLogConstraint) {
            visit((JLogConstraint) constraint);
        } else {
            throw new IllegalArgumentException(String.format("cannot visit constraint of type %s: not implemented",
                            constraint.getClass().getName()));
        }
    }

    default void visit(ShNode shNode) {
    }

    default void visit(ShNot shNot) {
    }

    default void visit(ShOr shOr) {
    }

    default void visit(ShXone shXone) {
    }

    default void visit(ShAnd shAnd) {
    }

    default void visit(InConstraint inConstraint) {
    }

    default void visit(HasValueConstraint hasValueConstraint) {
    }

    default void visit(ClassConstraint classConstraint) {
    }

    default void visit(DatatypeConstraint datatypeConstraint) {
    }

    default void visit(MinCount minCount) {
    }

    default void visit(MaxCount maxCount) {
    }

    default void visit(NodeKindConstraint nodeKindConstraint) {
    }

    default void visit(ClosedConstraint closedConstraint) {
    }

    default void visit(JLogConstraint jLogConstraint) {
    }
}

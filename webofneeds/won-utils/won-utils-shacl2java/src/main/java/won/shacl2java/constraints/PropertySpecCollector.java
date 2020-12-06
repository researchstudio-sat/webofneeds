package won.shacl2java.constraints;

import org.apache.jena.shacl.engine.constraint.*;
import org.apache.jena.shacl.parser.Constraint;
import org.apache.jena.shacl.parser.Shape;
import org.apache.jena.shacl.vocabulary.SHACL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static won.shacl2java.constraints.ConstraintVisitorAlgorithm.visitDepthFirst;

public class PropertySpecCollector implements ConstraintVisitor {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private boolean negated = false;
    Stack<Set<PropertySpec>> stack = new Stack<>();

    private PropertySpecCollector() {
        stack.push(new HashSet<>());
    }

    public static Set<PropertySpec> collectFrom(Shape shape) {
        PropertySpecCollector collector = new PropertySpecCollector();
        collector.visitRecursively(shape, collector);
        return collector.getPropertySpecs();
    }

    public Set<PropertySpec> getPropertySpecs() {
        return stack.pop();
    }

    // Operator constraints: we have to push onto the stack
    @Override
    public void visit(ShNot shNot) {
        Set<PropertySpec> deeperLevel = stack.pop();
        deeperLevel.forEach(p -> p.setNegated(!p.isNegated()));
        Set<PropertySpec> thisLevel = stack.pop();
        stack.push(crossMerge(thisLevel, deeperLevel));
    }

    @Override
    public void visit(ShOr shOr) {
        // join all sets from lower level, join with current level
        Set<PropertySpec> specs = new HashSet<>();
        for (int i = 0; i < shOr.getOthers().size(); i++) {
            specs.addAll(stack.pop());
        }
        stack.push(crossMerge(stack.pop(), specs));
    }

    @Override
    public void visit(ShXone shXone) {
        // join all sets from lower level, join with current level
        Set<PropertySpec> specs = new HashSet<>();
        for (int i = 0; i < shXone.getOthers().size(); i++) {
            specs.addAll(stack.pop());
        }
        stack.push(crossMerge(stack.pop(), specs));
    }

    private Set<PropertySpec> crossMerge(Set<PropertySpec> left, Set<PropertySpec> right) {
        return left.stream()
                        .flatMap(pleft -> right.stream()
                                        .map(pright -> pright
                                                        .mergeForConjuction(pleft)))
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .collect(Collectors.toSet());
    }

    @Override
    public void visit(ShAnd shAnd) {
        // cross-merge all branches
        Set<PropertySpec> specs = stack.stream().reduce(new HashSet<>(), this::crossMerge);
        stack.push(specs);
    }

    // property configuraiton constraints
    @Override
    public void visit(ShNode shNode) {
        if (shouldRecurseInto(shNode)) {
            Set<PropertySpec> deeperLevel = stack.pop();
            Set<PropertySpec> thisLevel = stack.pop();
            stack.push(crossMerge(thisLevel, deeperLevel));
        } else {
            Set<PropertySpec> propertySpecs = stack.peek();
            propertySpecs.forEach(p -> p.setShNodeShape(shNode.getOther()));
        }
    }

    @Override
    public void visit(InConstraint inConstraint) {
        Set<PropertySpec> propertySpecs = stack.peek();
        propertySpecs.forEach(p -> p.addShInOrHasValue(inConstraint.getValues()));
    }

    @Override
    public void visit(HasValueConstraint hasValueConstraint) {
        Set<PropertySpec> propertySpecs = stack.peek();
        propertySpecs.forEach(p -> p.addShInOrHasValue(hasValueConstraint.getValue()));
    }

    @Override
    public void visit(ClassConstraint classConstraint) {
        logger.warn("TODO: sh:class not handled for generating fields of java classes");
    }

    @Override
    public void visit(DatatypeConstraint datatypeConstraint) {
        Set<PropertySpec> propertySpecs = stack.peek();
        propertySpecs.forEach(p -> p.setShDatatype(datatypeConstraint.getRDFDatatype()));
    }

    @Override
    public void visit(MinCount minCount) {
        Set<PropertySpec> propertySpecs = stack.peek();
        propertySpecs.forEach(p -> p.setShMinCount(minCount.getMinCount()));
    }

    @Override
    public void visit(MaxCount maxCount) {
        Set<PropertySpec> propertySpecs = stack.peek();
        propertySpecs.forEach(p -> p.setShMaxCount(maxCount.getMaxCount()));
    }

    @Override
    public void visit(NodeKindConstraint nodeKindConstraint) {
        Set<PropertySpec> propertySpecs = stack.pop();
        propertySpecs = propertySpecs.stream().flatMap(p -> {
            Stream.Builder<PropertySpec> b = Stream.builder();
            if (nodeKindConstraint.isCanBeBlankNode()) {
                PropertySpec clone = p.clone();
                clone.setShNodeKind(SHACL.BlankNode);
                b.accept(clone);
            }
            if (nodeKindConstraint.isCanBeIRI()) {
                PropertySpec clone = p.clone();
                clone.setShNodeKind(SHACL.IRI);
                b.accept(clone);
            }
            if (nodeKindConstraint.isCanBeLiteral()) {
                p.setShNodeKind(SHACL.Literal);
                b.accept(p);
            }
            return b.build();
        }).collect(Collectors.toSet());
        stack.push(propertySpecs);
    }

    private void visitRecursively(Shape shape, ConstraintVisitor visitor) {
        stack.push(Stream.of(new PropertySpec()).collect(Collectors.toSet()));
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
        visitor.visit(constraint);
    }

    private boolean shouldRecurseInto(ShNode constraint) {
        return constraint.getOther().getShapeNode().isBlank();
    }
}

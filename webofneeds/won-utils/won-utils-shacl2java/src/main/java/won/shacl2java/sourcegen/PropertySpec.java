package won.shacl2java.sourcegen;

import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.shacl.parser.Shape;

import java.util.*;

public class PropertySpec {
    private Integer shMinCount;
    private Integer shMaxCount;
    private RDFDatatype shDatatype;
    private Node shNodeKind;
    private Shape shNodeShape;
    private boolean negated = false;
    private Set<Node> shInOrHasValue;

    public PropertySpec clone() {
        PropertySpec clone = new PropertySpec();
        clone.setShNodeKind(shNodeKind);
        clone.setShNodeShape(shNodeShape);
        clone.setShDatatype(shDatatype);
        clone.setShMaxCount(shMaxCount);
        clone.setShMinCount(shMinCount);
        if (shInOrHasValue != null) {
            Set<Node> values = new HashSet<>();
            values.addAll(shInOrHasValue);
            clone.setShInOrHasValue(values);
        }
        clone.setNegated(negated);
        return clone;
    }

    public PropertySpec() {
    }

    public Optional<PropertySpec> mergeForConjuction(PropertySpec other) {
        if (this.isNegated()) {
            if (other.isNegated()) {
                return Optional.empty();
            } else {
                return Optional.of(other.clone());
            }
        } else if (other.isNegated()) {
            return Optional.of(clone());
        }
        PropertySpec copy = clone();
        Set<Node> values = copy.shInOrHasValue;
        if (values == null) {
            values = new HashSet<>();
        }
        Set<Node> otherValues = other.getShInOrHasValue();
        if (otherValues != null) {
            values.retainAll(otherValues);
        }
        copy.shInOrHasValue = values;
        copy.setShDatatype(other.getShDatatype());
        copy.setShMaxCount(other.getShMaxCount());
        copy.setShMinCount(other.getShMinCount());
        copy.setShNodeKind(other.getShNodeKind());
        copy.setShNodeShape(other.getShNodeShape());
        return Optional.of(copy);
    }

    public boolean isNegated() {
        return negated;
    }

    public void setNegated(boolean negated) {
        this.negated = negated;
    }

    public Set<Node> getShInOrHasValue() {
        return shInOrHasValue;
    }

    public void setShInOrHasValue(Set<Node> shInOrHasValue) {
        this.shInOrHasValue = ensureNotModifying(this.shInOrHasValue, shInOrHasValue);
    }

    public void addShInOrHasValue(Node toAdd) {
        if (this.shInOrHasValue == null) {
            this.shInOrHasValue = new HashSet<>();
        }
        this.shInOrHasValue.add(toAdd);
    }

    public void addShInOrHasValue(Collection<Node> toAdd) {
        if (this.shInOrHasValue == null) {
            this.shInOrHasValue = new HashSet<>();
        }
        this.shInOrHasValue.addAll(toAdd);
    }

    public boolean isSingletonProperty() {
        return Integer.valueOf(1).equals(shMaxCount)
                        || (shInOrHasValue != null && shInOrHasValue.size() == 1);
    }

    public boolean isRequiredProperty() {
        return shMinCount != null && shMinCount > 0
                        || (shInOrHasValue != null && shInOrHasValue.size() > 0);
    }

    public Integer getShMinCount() {
        return shMinCount;
    }

    public void setShMinCount(Integer shMinCount) {
        ;
        this.shMinCount = ensureNotModifying(this.shMinCount, shMinCount);
    }

    public Integer getShMaxCount() {
        return shMaxCount;
    }

    public void setShMaxCount(Integer shMaxCount) {
        this.shMaxCount = ensureNotModifying(this.shMaxCount, shMaxCount);
    }

    public RDFDatatype getShDatatype() {
        return shDatatype;
    }

    public void setShDatatype(RDFDatatype shDatatype) {
        this.shDatatype = ensureNotModifying(this.shDatatype, shDatatype);
        ;
    }

    public Node getShNodeKind() {
        return shNodeKind;
    }

    public void setShNodeKind(Node shNodeKind) {
        this.shNodeKind = ensureNotModifying(this.shNodeKind, shNodeKind);
        ;
    }

    public boolean hasNamedNodeShape() {
        return shNodeShape != null && !shNodeShape.getShapeNode().isBlank();
    }

    public Shape getShNodeShape() {
        return shNodeShape;
    }

    public void setShNodeShape(Shape shNodeShape) {
        this.shNodeShape = ensureNotModifying(this.shNodeShape, shNodeShape);
        ;
    }

    private <T> T ensureNotModifying(T instanceValue, T newValue) {
        if (instanceValue == null) {
            return newValue;
        }
        if (newValue == null) {
            return instanceValue;
        }
        if (instanceValue.equals(newValue)) {
            return instanceValue;
        }
        throw new IllegalArgumentException("Modifying properties is not allowed");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        PropertySpec that = (PropertySpec) o;
        return negated == that.negated &&
                        Objects.equals(shMinCount, that.shMinCount) &&
                        Objects.equals(shMaxCount, that.shMaxCount) &&
                        Objects.equals(shDatatype, that.shDatatype) &&
                        Objects.equals(shNodeKind, that.shNodeKind) &&
                        Objects.equals(shNodeShape, that.shNodeShape) &&
                        Objects.equals(shInOrHasValue, that.shInOrHasValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(shMinCount, shMaxCount, shDatatype, shNodeKind, shNodeShape, negated, shInOrHasValue);
    }

    @Override
    public String toString() {
        return "PropertySpec{" +
                        "shMinCount=" + shMinCount +
                        ", shMaxCount=" + shMaxCount +
                        ", shDatatype=" + shDatatype +
                        ", shNodeKind=" + shNodeKind +
                        ", shNodeShape=" + shNodeShape +
                        ", negated=" + negated +
                        ", shInOrHasValue=" + shInOrHasValue +
                        '}';
    }
}

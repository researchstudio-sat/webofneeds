package won.shacl2java.sourcegen;

import org.apache.jena.graph.Node;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Holds a data Node, and optionally a node identifying a shape that the data
 * node has been found to be a target for.
 */
public class DataNodesAndShapes {
    private Set<Node> dataNodes;
    private Set<Node> shapeNodes;
    private static final DataNodesAndShapes EMPTY = new DataNodesAndShapes();

    public DataNodesAndShapes(Set<Node> dataNodes) {
        this.dataNodes = dataNodes;
        this.shapeNodes = new HashSet<>();
    }

    public DataNodesAndShapes(Set<Node> dataNodes, Set<Node> shapeNodes) {
        this.dataNodes = dataNodes;
        this.shapeNodes = shapeNodes;
    }

    public DataNodesAndShapes() {
        this.shapeNodes = new HashSet<>();
        this.dataNodes = new HashSet<>();
    }

    public Set<Node> getDataNodes() {
        return dataNodes;
    }

    public Set<Node> getShapeNodes() {
        return shapeNodes;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static SetBuilder setBuilder() {
        return new SetBuilder();
    }

    public static DataNodesAndShapes emptyInstance() {
        return EMPTY;
    }

    public boolean isEmpty() {
        return this.dataNodes.isEmpty() && this.shapeNodes.isEmpty();
    }

    public static class Builder {
        private DataNodesAndShapes product = new DataNodesAndShapes();

        private Builder() {
        }

        public Builder dataNode(Node node) {
            product.dataNodes.add(node);
            return this;
        }

        public DataNodesAndShapes build() {
            if (this.product == null)
                throw new IllegalStateException("build() cannot be called more than once");
            DataNodesAndShapes finalProduct = product;
            this.product = null;
            return finalProduct;
        }

        public Builder dataNodes(Collection<Node> nodes) {
            product.dataNodes.addAll(nodes);
            return this;
        }

        public Builder shapeNode(Node node) {
            product.shapeNodes.add(node);
            return this;
        }

        public Builder shapeNodes(Collection<Node> nodes) {
            product.shapeNodes.addAll(nodes);
            return this;
        }
    }

    public static class SetBuilder {
        private Set<DataNodesAndShapes> product = new HashSet<>();
        private Builder productBuilder = builder();

        private SetBuilder() {
        }

        public SetBuilder dataNode(Node node) {
            productBuilder.dataNode(node);
            return this;
        }

        public Set<DataNodesAndShapes> build() {
            if (this.product == null)
                throw new IllegalStateException("build() cannot be called more than once");
            Set<DataNodesAndShapes> finalProduct = product;
            this.product = null;
            return finalProduct;
        }

        public SetBuilder dataNodes(Collection<Node> nodes) {
            productBuilder.dataNodes(nodes);
            return this;
        }

        public SetBuilder shapeNode(Node node) {
            productBuilder.shapeNode(node);
            return this;
        }

        public SetBuilder shapeNodes(Collection<Node> nodes) {
            productBuilder.shapeNodes(nodes);
            return this;
        }

        public SetBuilder newSet() {
            product.add(productBuilder.build());
            productBuilder = builder();
            return this;
        }
    }
}

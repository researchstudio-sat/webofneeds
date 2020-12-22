package won.shacl2java.model;

import java.util.Objects;
import java.util.function.Consumer;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;

public class GraphEntity {
    private Node node;
    private Graph graph;

    public GraphEntity(Node node, Graph graph) {
        this.node = node;
        this.graph = graph;
    }

    public GraphEntity() {
    }

    public void detach() {
        this.graph = null;
    }

    public void accept(GraphEntityVisitor visitor) {
        visitor.visit(this);
    }

    public Node getNode() {
        return this.node;
    }

    public void setNode(Node node) {
        this.node = node;
    }

    public Graph getGraph() {
        return this.graph;
    }

    public void setGraph(Graph graph) {
        this.graph = graph;
    }

    /**
     * Passes all triples in the entity's graph to the consumer which have its
     * <code>node</code> as their subject.
     * 
     * @param consumer sink for triples
     */
    public void toRdf(Consumer<Triple> consumer) {
        if (this.graph == null) {
            throw new IllegalStateException("Cannot export RDF from a detached GraphEntity");
        }
        if (this.node == null) {
            throw new IllegalStateException(
                            "Cannot export RDF from GraphEntity without a node. Call createNode() first.");
        }
        graph.find(this.node, null, null).forEach(consumer);
    }

    public void createNode() {
        if (this.node != null) {
            throw new IllegalStateException("GraphEntity already has a node");
        }
        this.node = NodeFactory.createBlankNode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        GraphEntity that = (GraphEntity) o;
        return Objects.equals(node, that.node);
    }

    @Override
    public int hashCode() {
        return Objects.hash(node);
    }

    @Override
    public String toString() {
        return "GraphEntity{" +
                        "attached=" + (this.graph != null) +
                        ", node=" + node +
                        '}';
    }
}

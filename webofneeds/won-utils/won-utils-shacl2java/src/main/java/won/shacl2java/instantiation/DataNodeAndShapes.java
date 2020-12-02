package won.shacl2java.instantiation;

import org.apache.jena.graph.Node;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Holds a data Node, and optionally a node identifying a shape that the data
 * node has been found to be a target for.
 */
public class DataNodeAndShapes {
    private Node dataNode;
    private Set<Node> shapeNodes;
    private static final DataNodeAndShapes EMPTY = new DataNodeAndShapes();

    public DataNodeAndShapes(Node dataNode) {
        this.dataNode = dataNode;
        this.shapeNodes = new HashSet<>();
    }

    public DataNodeAndShapes(Node dataNode, Set<Node> shapeNodes) {
        this.dataNode = dataNode;
        this.shapeNodes = shapeNodes;
    }

    public DataNodeAndShapes() {
        this.shapeNodes = new HashSet<>();
        this.dataNode = null;
    }

    public Node getDataNode() {
        return dataNode;
    }

    public Set<Node> getShapeNodes() {
        return shapeNodes;
    }

    public static DataNodeAndShapes emptyInstance() {
        return EMPTY;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        DataNodeAndShapes that = (DataNodeAndShapes) o;
        return Objects.equals(dataNode, that.dataNode) &&
                        Objects.equals(shapeNodes, that.shapeNodes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dataNode, shapeNodes);
    }
}

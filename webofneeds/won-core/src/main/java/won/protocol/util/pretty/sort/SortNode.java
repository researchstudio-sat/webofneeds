package won.protocol.util.pretty.sort;

import java.util.ArrayList;
import java.util.List;

public class SortNode<T> {
    private T id;
    private List<T> neighbors;

    public SortNode(T id) {
        this.id = id;
        this.neighbors = new ArrayList<>();
    }

    public void addNeighbor(T e) {
        this.neighbors.add(e);
    }

    public T getId() {
        return id;
    }

    public List<T> getNeighbors() {
        return neighbors;
    }

    @Override
    public String toString() {
        return "Node{" +
                        "id=" + id +
                        ", neighbors=" + neighbors +
                        "}" + "\n";
    }
}
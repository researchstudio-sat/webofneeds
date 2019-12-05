package won.protocol.util.pretty.sort;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Topoplogical sort adapted from
 * https://stackabuse.com/topological-sorting-in-java/
 * 
 * @author fkleedorfer
 */
public class SortGraph<T> {
    private List<SortNode<T>> nodes;

    public SortGraph() {
        this.nodes = new ArrayList<>();
    }

    public SortGraph(List<SortNode<T>> nodes) {
        this.nodes = nodes;
    }

    public void addSortNode(SortNode<T> e) {
        this.nodes.add(e);
    }

    public List<SortNode<T>> getSortNodes() {
        return nodes;
    }

    public SortNode<T> getSortNode(T searchId) {
        for (SortNode<T> node : this.getSortNodes()) {
            if (node.getId().equals(searchId)) {
                return node;
            }
        }
        return null;
    }

    public int getSize() {
        return this.nodes.size();
    }

    @Override
    public String toString() {
        return "SortGraph{" +
                        "nodes=" + nodes +
                        "}";
    }

    public static <T> List<T> topologicalSort(SortGraph<T> g) {
        // Fetching the number of nodes in the graph
        int V = g.getSize();
        // List where we'll be storing the topological order
        List<T> order = new ArrayList<>();
        // Map which indicates if a node is visited (has been processed by the
        // algorithm)
        Map<T, Boolean> visited = new HashMap<>();
        for (SortNode<T> tmp : g.getSortNodes())
            visited.put(tmp.getId(), false);
        // We go through the nodes using black magic
        for (SortNode<T> tmp : g.getSortNodes()) {
            if (!visited.get(tmp.getId()))
                blackMagic(g, tmp.getId(), visited, order);
        }
        // We reverse the order we constructed to get the
        // proper toposorting
        return order;
    }

    private static <T> void blackMagic(SortGraph<T> g, T v, Map<T, Boolean> visited, List<T> order) {
        // Mark the current node as visited
        visited.replace(v, true);
        // We reuse the algorithm on all adjacent nodes to the current node
        for (T neighborId : g.getSortNode(v).getNeighbors()) {
            if (!visited.get(neighborId))
                blackMagic(g, neighborId, visited, order);
        }
        // Put the current node in the array
        order.add(v);
    }
}
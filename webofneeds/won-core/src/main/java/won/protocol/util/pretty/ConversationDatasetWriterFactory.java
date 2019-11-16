package won.protocol.util.pretty;

import static org.apache.jena.riot.writer.WriterConst.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.jena.atlas.io.IndentedWriter;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.WriterDatasetRIOT;
import org.apache.jena.riot.WriterDatasetRIOTFactory;
import org.apache.jena.riot.system.PrefixMap;
import org.apache.jena.riot.writer.TriGWriterBase;
import org.apache.jena.riot.writer.TurtleShell;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.util.Context;
import org.apache.jena.util.iterator.ExtendedIterator;

import won.protocol.message.WonMessageUtils;
import won.protocol.util.pretty.sort.SortGraph;
import won.protocol.util.pretty.sort.SortNode;

public class ConversationDatasetWriterFactory implements WriterDatasetRIOTFactory {
    @Override
    public WriterDatasetRIOT create(RDFFormat syntaxForm) {
        return new WonConversationDatasetWriter();
    }

    private static List<Node> orderGraphNames(DatasetGraph dsg) {
        Map<String, List<Node>> messageURIToGraph = new HashMap<String, List<Node>>();
        // first pass: list graphs, strip fragments -> get (presumed) message URIs.
        // remember mappings
        Iterator<Node> graphsIt = dsg.listGraphNodes();
        while (graphsIt.hasNext()) {
            Node graphNode = graphsIt.next();
            String messageURI = WonMessageUtils.stripFragment(URI.create(graphNode.getURI())).toString();
            List<Node> graphUris = messageURIToGraph.get(messageURI);
            if (graphUris == null) {
                graphUris = new ArrayList<>();
            }
            graphUris.add(graphNode);
            messageURIToGraph.put(messageURI, graphUris);
        }
        // second pass: check which graph refers to another graph, or to a
        // message that is associated with graphs, build a graph from that information
        // (the 'sortGraph')
        SortGraph<String> sg = new SortGraph<String>();
        graphsIt = dsg.listGraphNodes();
        while (graphsIt.hasNext()) {
            Node graphNode = graphsIt.next();
            Graph g = dsg.getGraph(graphNode);
            String messageURI = WonMessageUtils.stripFragment(URI.create(graphNode.getURI())).toString();
            SortNode<String> searchNode = sg.getSortNode(messageURI);
            if (searchNode == null) {
                searchNode = new SortNode<String>(messageURI);
            }
            ExtendedIterator<Triple> it = g.find();
            while (it.hasNext()) {
                Triple t = it.next();
                Node linkedGraphNode = t.getObject();
                if (!linkedGraphNode.isURI()) {
                    continue;
                }
                String linkedMessageUri = WonMessageUtils.stripFragment(URI.create(linkedGraphNode.getURI()))
                                .toString();
                if (dsg.containsGraph(linkedGraphNode)) {
                    // graph name is object of triple: that's a direct mention, add it
                    searchNode.addNeighbor(linkedMessageUri);
                } else {
                    // graph name might be a message uri. if so, add all graphs of the message to
                    // the neighbours
                    if (messageURIToGraph.keySet().contains(linkedMessageUri)) {
                        searchNode.addNeighbor(linkedMessageUri);
                    }
                }
            }
            sg.addSortNode(searchNode);
        }
        // run the topological sort on the sortGraph
        return SortGraph.topologicalSort(sg)
                        .stream()
                        .flatMap(x -> messageURIToGraph.get(x).stream()
                                        .sorted((a, b) -> a.getURI().compareTo(b.getURI())))
                        .collect(Collectors.toList());
    }

    /** copied/adapted from jena's TriGWriter.java. */
    private static class WonConversationDatasetWriter extends TriGWriterBase {
        @Override
        protected void output(IndentedWriter iOut, DatasetGraph dsg, PrefixMap prefixMap, String baseURI,
                        Context context) {
            TriGWriter$ w = new TriGWriter$(iOut, prefixMap, baseURI, context);
            w.write(dsg);
        }

        private static class TriGWriter$ extends TurtleShell {
            TriGWriter$(IndentedWriter out, PrefixMap prefixMap, String baseURI, Context context) {
                super(out, prefixMap, baseURI, context);
            }

            private void write(DatasetGraph dsg) {
                List<Node> orderedGraphNames = orderGraphNames(dsg);
                writeBase(baseURI);
                writePrefixes(prefixMap);
                if (!prefixMap.isEmpty() && !dsg.isEmpty())
                    out.println();
                Iterator<Node> graphNames = orderedGraphNames.iterator();
                String lastEntity = null;
                boolean anyGraphOutput = writeGraphTriG(dsg, null);
                for (; graphNames.hasNext();) {
                    if (anyGraphOutput)
                        out.println();
                    Node gn = graphNames.next();
                    String entity = WonMessageUtils.stripFragment(URI.create(gn.getURI())).toString();
                    if (lastEntity == null || !lastEntity.equals(entity)) {
                        out.println("# graphs of entity " + entity);
                        out.println("");
                        lastEntity = entity;
                    }
                    anyGraphOutput |= writeGraphTriG(dsg, gn);
                }
            }

            /** Return true if anything written */
            private boolean writeGraphTriG(DatasetGraph dsg, Node name) {
                boolean dftGraph = (name == null || name == Quad.defaultGraphNodeGenerated);
                if (dftGraph && dsg.getDefaultGraph().isEmpty())
                    return false;
                if (dftGraph && !GDFT_BRACE) {
                    // Non-empty default graph, no braces.
                    // No indenting.
                    writeGraphTTL(dsg, name);
                    return true;
                }
                // The graph will go in braces, whether non-empty default graph or a named
                // graph.
                boolean NL_START = (dftGraph ? NL_GDFT_START : NL_GNMD_START);
                boolean NL_END = (dftGraph ? NL_GDFT_END : NL_GNMD_END);
                int INDENT_GRAPH = (dftGraph ? INDENT_GDFT : INDENT_GNMD);
                if (!dftGraph) {
                    writeNode(name);
                    out.print(" ");
                }
                out.print("{");
                if (NL_START)
                    out.println();
                else
                    out.print(" ");
                out.incIndent(INDENT_GRAPH);
                writeGraphTTL(dsg, name);
                out.decIndent(INDENT_GRAPH);
                if (NL_END)
                    out.ensureStartOfLine();
                out.println("}");
                return true;
            }
        }
    }
}
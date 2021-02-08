package won.utils.content;

import org.apache.jena.graph.Graph;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.shacl.Shapes;
import org.apache.jena.sparql.graph.GraphFactory;
import won.shacl2java.Shacl2JavaInstanceFactory;

public class ContentUtils {
    private static final Shapes shapes = Shapes.parse(readShapesData());

    private static Graph readShapesData() {
        Graph graph = GraphFactory.createGraphMem();
        RDFDataMgr.read(graph,
                        ContentUtils.class.getClassLoader().getResourceAsStream("shacl/won-content-shapes.ttl"),
                        Lang.TTL);
        return graph;
    }

    public static Shapes shapes() {
        return shapes;
    }

    public static Shacl2JavaInstanceFactory newInstanceFactory() {
        return new Shacl2JavaInstanceFactory(shapes(), "won.utils.content.model");
    }
}

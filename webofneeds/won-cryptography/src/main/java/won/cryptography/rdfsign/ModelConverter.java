package won.cryptography.rdfsign;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.util.Iterator;
import java.util.List;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;

import de.uni_koblenz.aggrimm.icp.crypto.sign.graph.GraphCollection;
import de.uni_koblenz.aggrimm.icp.crypto.sign.graph.NamedGraph;
import de.uni_koblenz.aggrimm.icp.crypto.sign.graph.Prefix;
import de.uni_koblenz.aggrimm.icp.crypto.sign.graph.Triple;
import de.uni_koblenz.aggrimm.icp.crypto.sign.trigplus.TriGPlusWriter;

/**
 * Created by ypanchenko on 09.07.2014.
 */
public class ModelConverter {
    /**
     * Converts Signingframework's NamedGraph into Jena's Model. It is required,
     * that GraphCollection stores the prefixes (if present), but that they are not
     * applied (i.e. its applyPrefixes() method should not be called). Otherwise
     * there could be errors when converting, since Signingframework when applying
     * prefixes just looks whether the resource uri starts with that prefix uri,
     * which would be true in both cases below: <code>
     * &#64;prefix : &lt;http://www.example.com/resource/atom/12#&gt; .
     * &#64;prefix atom: &lt;http://www.example.com/resource/atom/12&gt; . Also, applying
     * prefixes in NamedGraph in cases like
     * &#64;prefix : &lt;http://www.example.com/resource/atom/12/&gt;
     * &lt;http://www.example.com/resource/atom/12/connections/&gt; a ldp:Container .
     * </code> would result in a wrong RDF triple: :connections/ a ldp:Container .
     */
    public static Model namedGraphToModel(String graphName, GraphCollection gc) throws Exception {
        NamedGraph graph = null;
        for (NamedGraph g : gc.getGraphs()) {
            if (g.getName().equals(graphName)) {
                graph = g;
                break;
            }
        }
        return namedGraphToModel(graph, gc.getPrefixes());
    }

    /**
     * Converts Jena's Model into Signingframework's NamedGraph of GraphCollection.
     * Resulting GraphCollection contains exactly one NamedGraph that corresponds to
     * the provided Model (modelURI) from the provided Dataset. The resulting
     * GraphCollection stores the prefixes (if were present in the Datastore), but
     * they are not applied (for reasons, see namedGraphToModel() doc).
     */
    public static GraphCollection modelToGraphCollection(String modelURI, Dataset dataset) {
        Model model = dataset.getNamedModel(modelURI);
        return modelToGraphCollection(modelURI, model);
    }

    public static GraphCollection modelsToGraphCollection(Dataset dataset, String... modelURIs) {
        GraphCollection graphc = new GraphCollection();
        for (int i = 0; i < modelURIs.length; i++) {
            String name = modelURIs[i];
            Model model = dataset.getNamedModel(name);
            if (model == null) {
                throw new IllegalArgumentException("No model named '" + name + "' found in dataset");
            }
            graphc.addGraph(fromModel(name, model));
        }
        return graphc;
    }

    private static Model namedGraphToModel(NamedGraph graph, List<Prefix> prefixes) throws Exception {
        // Here, the simplest, but probably not the most efficient, approach is
        // applied: Signingframework's reader and Jena's writer are used to
        // transform data from one data structure to another. Works fine.
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os));
        String dfPref = "";
        // write prefixes
        for (Prefix p : prefixes) {
            writer.write(p.toString());
            if (p.getPrefix().equals(":")) {
                dfPref = p.getIriContent();
            }
        }
        // write NamedGraph
        TriGPlusWriter.writeGraph(writer, graph, 3);
        writer.close();
        // String content = os.toString();
        // read the result with Jena as Dataset
        ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());
        Dataset dataset = DatasetFactory.createGeneral();
        RDFDataMgr.read(dataset, is, RDFFormat.TRIG.getLang());
        // extract the Model that corresponds to the NamedGraph
        String modelName = graphNameToModelName(graph.getName(), dfPref);
        Model model = dataset.getNamedModel(modelName);
        for (Prefix pref : prefixes) {
            model.setNsPrefix(pref.getPrefix().replace(":", ""), pref.getIriContent());
        }
        return model;
    }

    private static String graphNameToModelName(final String graphName, final String dfPref) {
        if (graphName.startsWith("<")) {
            return graphName.substring(1, graphName.length() - 1);
        } else {
            return dfPref + graphName.replace(":", "");
        }
    }

    public static GraphCollection fromDataset(Dataset dataset) {
        GraphCollection graphc = new GraphCollection();
        String name = null;
        Iterator<String> namesIt = dataset.listNames();
        while (namesIt.hasNext()) {
            name = namesIt.next();
            graphc.addGraph(fromModel(name, dataset.getNamedModel(name)));
        }
        return graphc;
    }

    private static GraphCollection modelToGraphCollection(String name, Model model) {
        // Convert each subj pred obj in Jena Statement into String and add to
        // SigningFramework's NamedGraph.
        // The simpler approach with just using Jena's writer and Signingframework's
        // reader to transform data between data structures won't work since
        // Signingframework has problems with recognizing the [] structure
        GraphCollection graphc = new GraphCollection();
        NamedGraph namedGraph = fromModel(name, model);
        graphc.addGraph(namedGraph);
        // don't apply prefixes since it can result in funny things like:
        // pref:/connections/, and also the collision on the prefix uris
        // that starts the same. E.g. having prefixes below in atom rdf
        // would cause errors
        // @prefix : <http://www.example.com/resource/atom/12#> .
        // @prefix atom: <http://www.example.com/resource/atom/12> .
        // graphc.applyPrefixes();
        return graphc;
    }

    public static NamedGraph fromModel(String name, Model model) {
        NamedGraph namedGraph = new NamedGraph(enclose(name, "<", ">"), 0, null);
        StmtIterator iterator = model.listStatements();
        while (iterator.hasNext()) {
            Statement stmt = iterator.nextStatement();
            String subjString = rdfNodeAsString(stmt.getSubject());
            String objString = rdfNodeAsString(stmt.getObject());
            String predString = enclose(stmt.getPredicate().asResource().getURI(), "<", ">");
            Triple gcTriple = new Triple(subjString, predString, objString);
            namedGraph.addTriple(gcTriple);
        }
        return namedGraph;
    }

    private static String rdfNodeAsString(final RDFNode rdfNode) {
        String result;
        if (rdfNode.isURIResource()) {
            String uri = rdfNode.asResource().getURI();
            result = enclose(uri, "<", ">");
        } else if (rdfNode.isLiteral()) {
            result = enclose(rdfNode.asLiteral().getLexicalForm(), "\"", "\"");
            if (rdfNode.asLiteral().getDatatypeURI() != null) {
                result = enclose(result, "", "^^" + "<" + rdfNode.asLiteral().getDatatypeURI() + ">");
            } else if (rdfNode.asLiteral().getLanguage() != null && !rdfNode.asLiteral().getLanguage().isEmpty()) {
                result = enclose(result, "", "@" + rdfNode.asLiteral().getLanguage());
            }
        } else if (rdfNode.isAnon()) {
            result = enclose(rdfNode.asResource().getId().getLabelString(), "_:", "");
        } else {
            // TODO It might need to be improved as some syntax cases might not be covered
            // so far
            // a collection??
            throw new UnsupportedOperationException("support missing for converting: " + rdfNode.toString());
        }
        return result;
    }

    private static String enclose(String string, String start, String end) {
        return start + string + end;
    }
}

package won.cryptography.rdfsign;

import java.util.ArrayList;
import java.util.LinkedList;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;

import de.uni_koblenz.aggrimm.icp.crypto.sign.graph.GraphCollection;
import de.uni_koblenz.aggrimm.icp.crypto.sign.graph.NamedGraph;
import de.uni_koblenz.aggrimm.icp.crypto.sign.graph.Prefix;
import de.uni_koblenz.aggrimm.icp.crypto.sign.graph.SignatureData;
import de.uni_koblenz.aggrimm.icp.crypto.sign.graph.Triple;
import de.uni_koblenz.aggrimm.icp.crypto.sign.ontology.Ontology;
import won.protocol.util.RdfUtils;
import won.protocol.util.WonRdfUtils;
import won.protocol.vocabulary.WONMSG;

/**
 * Created by ypanchenko on 08.07.2014. A utility class for assembling the
 * calculated signature inside the GraphCollection into the original Dataset;
 * for removing signature graphs from a Dataset.
 */
public class WonAssembler {
    private static final String SIG_GRAPH_NAME_TEMP = "<http://localhost:8080/won/SIG-GRAPH-PLACEHOLDER-TEMP>";
    private static final String SIG_BNODE_NAME = "_:sig-1";

    /**
     * Assumes that namedSignedGraph is GraphCollection containing exactly one named
     * graph that is part of the Dataset graphOrigin, and that for this named graph
     * the signature is already calculated inside the GraphCollection internally
     * (i.e. an Algorithm's methods canonicalize(namedSignedGraph),
     * postCanonicalize(namedSignedGraph), hash(namedSignedGraph, envHashAlgorithm),
     * postHash(namedSignedGraph), sign(namedSignedGraph, privateKey) have already
     * been applied. The method assembles the signature from provided
     * GraphCollection into the origin Dataset by putting signature triples inside
     * the default graph of the Dataset. Intended for the use by WonSigner.
     *
     * @param namedSignedGraph GraphCollection containing one named graph with its
     * calculated signature
     * @param graphOrigin Dataset that contains the graph that has was used to
     * construct the namedSignedGraph
     * @throws Exception
     */
    @Deprecated
    public static void assemble(GraphCollection namedSignedGraph, Dataset graphOrigin) throws Exception {
        Ontology o = prepareSignatureOntology(namedSignedGraph);
        verifyGraphCollectionContainsExactlyOneNamedGraph(namedSignedGraph);
        addSignatureTriplesToOrigin(namedSignedGraph, o, graphOrigin);
    }

    /**
     * Assumes that namedSignedGraph is GraphCollection containing exactly one named
     * graph that is part of the Dataset graphOrigin, and that for this named graph
     * the signature is already calculated inside the GraphCollection internally
     * (i.e. an Algorithm's methods canonicalize(namedSignedGraph),
     * postCanonicalize(namedSignedGraph), hash(namedSignedGraph, envHashAlgorithm),
     * postHash(namedSignedGraph), sign(namedSignedGraph, privateKey) have already
     * been applied. The method assembles the signature from provided
     * GraphCollection into the origin Dataset by putting signature triples inside
     * the named graph of the Dataset. Intended for the use by WonSigner.
     *
     * @param namedSignedGraph GraphCollection containing one named graph with its
     * calculated signature
     * @param graphOrigin Dataset that contains the graph that has was used to
     * construct the namedSignedGraph
     * @param sigGraphURI the name (URI) of the graph that should be assigned to the
     * signature graph
     * @throws Exception
     */
    public static void assemble(GraphCollection namedSignedGraph, Dataset graphOrigin, String sigGraphURI)
                    throws Exception {
        Ontology o = prepareSignatureOntology(namedSignedGraph);
        verifyGraphCollectionContainsExactlyOneNamedGraph(namedSignedGraph);
        addSignatureAsNamedGraphToOrigin(namedSignedGraph, o, graphOrigin, sigGraphURI);
    }

    /**
     * Removes signature graphs from the Dataset. Can be useful to use after
     * verification is done, when the signatures are no longer required for further
     * actions on the signed data of the Dataset.
     *
     * @param dataset from which graphs representing signatures have to be removed
     */
    public static void removeSignatureGraphs(Dataset dataset) {
        for (String name : RdfUtils.getModelNames(dataset)) {
            if (WonRdfUtils.SignatureUtils.isSignatureGraph(name, dataset.getNamedModel(name))) {
                dataset.removeNamedModel(name);
            }
        }
    }

    private static void verifyGraphCollectionContainsExactlyOneNamedGraph(GraphCollection gc) {
        LinkedList<NamedGraph> graphs = gc.getGraphs();
        if (graphs.size() == 1 && !graphs.get(0).getName().isEmpty()) {
            // it's OK
        } else if (graphs.size() == 2 && graphs.get(0).getName().isEmpty() || graphs.get(1).getName().isEmpty()) {
            // it's OK
        } else {
            // it's not OK
            throw new IllegalArgumentException(WonAssembler.class.getName() + " expects exactly one named graph, found "
                            + (graphs.size() - 1));
        }
    }

    private static Ontology prepareSignatureOntology(GraphCollection gc) {
        // Get Signature Data
        SignatureData sigData = gc.getSignature();
        // Prepare Ontology
        Ontology o = new Ontology(sigData);
        // Choose an unused prefix for signatures to avoid prefix collisions
        // Add number to default prefix in case it is used in graph already with other
        // IRI
        String sigPrefix = o.getSigPrefix(); // Get signature prefix from Ontology
        String sigIri = Ontology.getSigIri(); // Get signature IRI from Ontology
        String sigPre = sigPrefix;
        for (int prefixCounter = 2; true; prefixCounter++) {
            // Find equal prefix with different IRI
            boolean prefixUsed = false;
            for (Prefix p : gc.getPrefixes()) {
                if (p.getPrefix().equals(sigPre)) {
                    if (!p.getIri().equals("<" + sigIri + ">")) {
                        // Found!
                        prefixUsed = true;
                        break;
                    }
                }
            }
            if (prefixUsed) {
                // Prefix is used with different IRI! Try again with another one (add higher
                // number)!
                sigPre = sigPrefix + prefixCounter;
            } else {
                // Prefix is not used with a different IRI! Continue!
                break;
            }
        }
        o.setSigPrefix(sigPre);
        return o;
    }

    private static NamedGraph getSignatureAsGraph(GraphCollection gc, Ontology o) {
        gc.addPrefix(new Prefix(o.getSigPrefix() + ":", "<" + Ontology.getSigIri() + ">"));
        String name = gc.getGraphs().get(0).getName();
        if (name.isEmpty()) {
            name = gc.getGraphs().get(1).getName();
        }
        NamedGraph sigGraph = new NamedGraph(SIG_GRAPH_NAME_TEMP, 0, null);
        ArrayList<Triple> sigGraphTriples = sigGraph.getTriples();
        // this graph is signed by the signature
        sigGraphTriples.add(new Triple(SIG_BNODE_NAME, "<" + WONMSG.signedGraph + ">", name));
        for (Triple t : o.getTriples()) {
            String subj = t.getSubject();
            sigGraphTriples.add(new Triple(subj, t.getPredicate(), t.getObject()));
        }
        gc.addGraph(sigGraph);
        return sigGraph;
    }

    private static NamedGraph getSignatureAsGraph(GraphCollection gc, String sigGraphURI, Ontology o) {
        gc.addPrefix(new Prefix(o.getSigPrefix() + ":", "<" + Ontology.getSigIri() + ">"));
        // the signed graph
        String name = gc.getGraphs().get(0).getName();
        if (name.isEmpty()) {
            name = gc.getGraphs().get(1).getName();
        }
        NamedGraph sigGraph = new NamedGraph(SIG_GRAPH_NAME_TEMP, 0, null);
        ArrayList<Triple> sigGraphTriples = sigGraph.getTriples();
        // this graph is signed by the signature
        sigGraphTriples.add(new Triple("<" + sigGraphURI + ">", "<" + WONMSG.signedGraph + ">", name));
        for (Triple t : o.getTriples()) {
            String subj = t.getSubject();
            if (subj.equals(SIG_BNODE_NAME)) {
                // this graph represents the signature and contains the signature triples
                subj = "<" + sigGraphURI + ">";
            }
            sigGraphTriples.add(new Triple(subj, t.getPredicate(), t.getObject()));
        }
        gc.addGraph(sigGraph);
        return sigGraph;
    }

    private static void addSignatureAsNamedGraphToOrigin(GraphCollection namedSignedGraph, Ontology o,
                    Dataset graphOrigin, String sigGraphURI) throws Exception {
        NamedGraph signatureAsGraph = getSignatureAsGraph(namedSignedGraph, sigGraphURI, o);
        Model signatureAsModel = ModelConverter.namedGraphToModel(signatureAsGraph.getName(), namedSignedGraph);
        graphOrigin.addNamedModel(sigGraphURI, signatureAsModel);
        addPrefixesToDefaultGraph(signatureAsModel, graphOrigin);
    }

    private static void addSignatureTriplesToOrigin(GraphCollection namedSignedGraph, Ontology o, Dataset graphOrigin)
                    throws Exception {
        NamedGraph signatureAsGraph = getSignatureAsGraph(namedSignedGraph, o);
        Model signatureAsModel = ModelConverter.namedGraphToModel(signatureAsGraph.getName(), namedSignedGraph);
        graphOrigin.getDefaultModel().add(signatureAsModel);
        addPrefixesToDefaultGraph(signatureAsModel, graphOrigin);
    }

    private static void addPrefixesToDefaultGraph(final Model signatureAsModel, final Dataset graphOrigin) {
        for (String prefix : signatureAsModel.getNsPrefixMap().keySet()) {
            graphOrigin.getDefaultModel().setNsPrefix(prefix, signatureAsModel.getNsPrefixMap().get(prefix));
        }
        graphOrigin.getDefaultModel().getNsPrefixMap().putAll(signatureAsModel.getNsPrefixMap());
    }
}

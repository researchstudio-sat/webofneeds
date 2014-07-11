package won.cryptography.rdfsign;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;
import de.uni_koblenz.aggrimm.icp.crypto.sign.graph.*;
import de.uni_koblenz.aggrimm.icp.crypto.sign.ontology.Ontology;

import java.util.ArrayList;
import java.util.LinkedList;

/**
 * Created by ypanchenko on 08.07.2014.
 *
 * The class with methods to assemble a signature of the graph data and the graph data itself into
 * one graph.
 * The idea is borrowed from de.uni_koblenz.aggrimm.icp.crypto.sign.algorithm.generic.Assembler
 * But the implementation differ in that the data to be signed is assumed to be in one named graph
 * and resulting signature is placed separately as triples (basically in the default unnamed graph)
 * outside of the named graph containing the corresponding signed data.
 */
public class WonAssembler {

    public static String SIG_GRAPH_NAME_TEMP = ":SIG-GRAPH-PLACEHOLDER-TEMP";

    /**
     * Assumes that namedSignedGraph is GraphCollection containing exactly one named graph
     * with the name graphName, and that for this namedSignedGraph the signature is already
     * calculated internally (i.e. an Algorithm methods canonicalize(namedSignedGraph),
     * postCanonicalize(namedSignedGraph), hash(namedSignedGraph, envHashAlgorithm),
     * postHash(namedSignedGraph), sign(namedSignedGraph, privateKey, "\"cert\"") have
     * already been applied.
     *
     * @param namedSignedGraph
     * @param graphName
     * @param graphOrigin
     * @throws Exception
     */
    public static void assemble(GraphCollection namedSignedGraph, String graphName, Dataset graphOrigin) throws Exception {

        Ontology o = prepareSignatureOntology(namedSignedGraph);
        verifyGraphCollectionContainsExactlyOneNamedGraph(namedSignedGraph);
        addSignatureTriplesToOrigin(namedSignedGraph, o, graphName, graphOrigin);

    }

    public static void assemble(GraphCollection gc, String signatureGraphSuffix) throws Exception {
        assemble(gc, signatureGraphSuffix, true);
    }


    public static void assemble(GraphCollection gc, String signatureGraphSuffix, Boolean addSignature) throws Exception {

        Ontology o = prepareSignatureOntology(gc);
        verifyGraphCollectionContainsExactlyOneNamedGraph(gc);
        addSignatureAsSeparateGraph(gc, o);

        //String sigGraphName = foundNamedGraph.getName() + "-" + signatureGraphSuffix;
        //TODO signature suffix name will be managed by WonSigner...
        //and this name will be used in this case for named graphs with corresponding
        //to this graph signature

    }

    private static void verifyGraphCollectionContainsExactlyOneNamedGraph(GraphCollection gc) {
        LinkedList<NamedGraph> graphs = gc.getGraphs();
        if (graphs.size() == 1 && !graphs.get(0).getName().isEmpty()) {
            // it's OK
        } else if (graphs.size() == 2
                && graphs.get(0).getName().isEmpty() || graphs.get(1).getName().isEmpty()) {
            // it's OK
        } else {
            // it's not OK
            throw new IllegalArgumentException(WonAssembler.class.getName() +
                    " expects exactly one named graph, found " + (graphs.size() - 1));
        }
    }

    private static Ontology prepareSignatureOntology(GraphCollection gc) {



        //Get Signature Data
        SignatureData sigData=gc.getSignature();

        //Prepare Ontology
        Ontology o=new Ontology(sigData);

        //Choose an unused prefix for signatures to avoid prefix collisions
        //Add number to default prefix in case it is used in graph already with other IRI
        String sigPrefix=o.getSigPrefix();			//Get signature prefix from Ontology
        String sigIri=Ontology.getSigIri();			//Get signature IRI from Ontology
        String sigPre=sigPrefix;
        for (int prefixCounter=2; true; prefixCounter++ ){
            //Find equal prefix with different IRI
            boolean prefixUsed=false;
            for (Prefix p:gc.getPrefixes()){
                if (p.getPrefix().equals(sigPre)){
                    if (!p.getIri().equals("<"+sigIri+">")){
                        //Found!
                        prefixUsed=true;
                        break;
                    }
                }
            }
            if (prefixUsed){
                //Prefix is used with different IRI! Try again with another one (add higher number)!
                sigPre=sigPrefix+prefixCounter;
            }else{
                //Prefix is not used with a different IRI! Continue!
                break;
            }
        }
        o.setSigPrefix(sigPre);

        return o;
    }

    private static NamedGraph getSignatureAsGraph(GraphCollection gc, Ontology o) {

      String name = gc.getGraphs().get(0).getName();
      if (name.isEmpty()) {
        name = gc.getGraphs().get(1).getName();
      }

        NamedGraph sigGraph = new NamedGraph(SIG_GRAPH_NAME_TEMP, 0, null);
        sigGraph.applyPrefixes(gc.getPrefixes());
        ArrayList<Triple> sigGraphTriples = sigGraph.getTriples();
        LinkedList<Triple> signatureTriples = o.getTriples();
        for (Triple t : signatureTriples){
          String subj = t.getSubject();
          if (subj.equals("_:sig-1")) {
            subj = name;
          }
            sigGraphTriples.add( new Triple(subj,t.getPredicate(),t.getObject()) );
        }
        return sigGraph;
    }

    private static void addSignatureAsSeparateGraph(GraphCollection gc, Ontology o) {

        boolean addSignature = true;
      String name = gc.getGraphs().get(0).getName();
      if (name.isEmpty()) {
        name = gc.getGraphs().get(1).getName();
      }

        //Add signature triples from onotology
        NamedGraph sigGraph = new NamedGraph(SIG_GRAPH_NAME_TEMP, 0, null);
        ArrayList<Triple> sigGraphTriples = sigGraph.getTriples();
        LinkedList<Triple> signatureTriples;
        if (addSignature){
            signatureTriples=o.getTriples();
        }else{
            signatureTriples=o.getTriplesWithoutSignature();
        }
        for (Triple t:signatureTriples){
          String subj = t.getSubject();
          if (subj.equals("_:sig-1")) {
            subj = name;
          }
            sigGraphTriples.add( new Triple(subj,t.getPredicate(),t.getObject()) );
        }
        gc.addGraph(sigGraph);
        //Add old root triples
        //for (Triple t:rootTriples){
        //    sigGraphTriples.add(t);
        //}

        //Add signature prefix
        gc.addPrefix(new Prefix(o.getSigPrefix()+":","<"+Ontology.getSigIri()+">"));
        gc.applyPrefixes();
    }


    private static void addSignatureTriplesToOrigin(
        GraphCollection namedSignedGraph, Ontology o, String graphName, Dataset graphOrigin) throws Exception {
        namedSignedGraph.addPrefix(new Prefix(o.getSigPrefix()+":","<"+Ontology.getSigIri()+">"));
        namedSignedGraph.applyPrefixes();
        NamedGraph signatureAsGraph = getSignatureAsGraph(namedSignedGraph, o);

        //Model signatureAsModel = ModelConverter.namedGraphToModel(signatureAsGraph, namedSignedGraph.getPrefixes());
      Model signatureAsModel = ModelConverter.namedGraphToModel(signatureAsGraph.getName(), namedSignedGraph);
        graphOrigin.getDefaultModel().add(signatureAsModel);
        for (String prefix : signatureAsModel.getNsPrefixMap().keySet()) {
            graphOrigin.getDefaultModel().setNsPrefix(prefix, signatureAsModel.getNsPrefixMap().get(prefix));
        }
        graphOrigin.getDefaultModel().getNsPrefixMap().putAll(signatureAsModel.getNsPrefixMap());
    }
}

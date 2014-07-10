package won.cryptography.rdfsign;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;
import de.uni_koblenz.aggrimm.icp.crypto.sign.graph.*;
import de.uni_koblenz.aggrimm.icp.crypto.sign.ontology.Ontology;

import java.util.ArrayList;
import java.util.Iterator;
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

    public static String SIG_GRAPH_NAME_TEMP = "SIG-GRAPH-PLACEHOLDER-TEMP";

    public static void assemble(GraphCollection gc, String signatureGraphSuffix) throws Exception {
        assemble(gc, signatureGraphSuffix, true);
    }


    public static void assemble(GraphCollection gc, String signatureGraphSuffix, Boolean addSignature) throws Exception {

        /*
        Start copy from de.uni_koblenz.aggrimm.icp.crypto.sign.algorithm.generic.Assembler
        assemble(GraphCollection gc, String signatureGraphName, Boolean addSignature)
         */

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

        /*
        End copy from de.uni_koblenz.aggrimm.icp.crypto.sign.algorithm.generic.Assembler
        assemble(GraphCollection gc, String signatureGraphName, Boolean addSignature)
         */


        //Create a signature for each named graph

        //Cache old root level graph list
        //LinkedList<NamedGraph> temp = gc.getGraphs();

        LinkedList<NamedGraph> graphs = gc.getGraphs();
        if (graphs.size() != 2) {
            throw new IllegalArgumentException(WonAssembler.class.getName() +
                    " expects exactly one named graph, found " + (graphs.size() - 1));
        }
        NamedGraph namedGraph = graphs.get(0);
        if (namedGraph.getName().equals("")) {
            namedGraph = graphs.get(1);
        }
        //String sigGraphName = foundNamedGraph.getName() + "-" + signatureGraphSuffix;
        //TODO signature suffix name will be managed by WonSigner...
        //and this name will be used in this case for named graphs with corresponding
        //to this graph signature

        //Create signature graph
        //NamedGraph sigGraph=new NamedGraph(signatureGraphName,0,null);

        //Put old graphs into signature graph
        //sigGraph.setChildren(temp);

        //New list for graphs at root level
        //LinkedList<NamedGraph> graphs=new LinkedList<NamedGraph>();

        //Add virtual graph
        //graphs.add(new NamedGraph("",-1,null));

        //Add signature graph
        //graphs.add(sigGraph);

        //Set this list as new root level graph list
        //gc.setGraphs(graphs);

        //Copy root level triples (in old root level virtual graph)
        //ArrayList<Triple> rootTriples = new ArrayList<Triple>();
        //Iterator<NamedGraph> it = temp.iterator();
//        while (it.hasNext()) {
//            NamedGraph checkGraph=it.next();
//            //Is virtual graph?
//            if (checkGraph.getDepth()==-1 && checkGraph.getName().length()==0){
//                //Triples
//                for (Triple t:checkGraph.getTriples()){
//                    rootTriples.add(t);
//                }
//                //MSGs
//                if (checkGraph.getMSGs()!=null){
//                    for (MSG msg:checkGraph.getMSGs()){
//                        for (Triple t:msg.getTriples()){
//                            rootTriples.add(t);
//                        }
//                    }
//                }
//                it.remove();
//                break;
//            }
//        }
        //Update depths of modified graph collection
        //gc.updateDepths();

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
            sigGraphTriples.add( new Triple(t.getSubject(),t.getPredicate(),t.getObject()) );
        }
        gc.addGraph(sigGraph);
        //Add old root triples
        //for (Triple t:rootTriples){
        //    sigGraphTriples.add(t);
        //}

        //Add signature prefix
        gc.addPrefix(new Prefix(sigPre+":","<"+sigIri+">"));
        gc.applyPrefixes();
    }
}

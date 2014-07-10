package won.cryptography.rdfsign;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;
import de.uni_koblenz.aggrimm.icp.crypto.sign.algorithm.algorithm.SignatureAlgorithmFisteus2010;
import de.uni_koblenz.aggrimm.icp.crypto.sign.graph.GraphCollection;
import de.uni_koblenz.aggrimm.icp.crypto.sign.trigplus.TriGPlusWriter;

import java.io.IOException;
import java.security.*;
import java.util.Iterator;

/**
 * Created by ypanchenko on 18.06.2014.
 */
public class Fisteus2010AlgorithmWrapper extends SignatureAlgorithmFisteus2010 implements SigningAlgorithm2 {

    private SignatureAlgorithmFisteus2010 algorithm = new SignatureAlgorithmFisteus2010();
    private static String envHashAlgorithm = "sha-256";

    @Override
    public Dataset sign(Dataset dataset, PrivateKey privateKey) throws Exception {

        Iterator<String> datasetIterator = dataset.listNames();
        //TODO put signature(s) from default model into separate graphs
        //and sign them as well
        //Model dfModel = dataset.getDefaultModel();
        //StmtIterator si = dfModel.listStatements();
        //System.out.println(si.hasNext());

        while (datasetIterator.hasNext()) {
            String name = datasetIterator.next();
            //System.out.println("name=" + name);
            Model model = dataset.getNamedModel(name);

            // TODO transform model/read model into the GraphCollection
            GraphCollection inputGraph = null;

            algorithm.canonicalize(inputGraph);
            algorithm.postCanonicalize(inputGraph);
            algorithm.hash(inputGraph, envHashAlgorithm);
            algorithm.postHash(inputGraph);
            algorithm.sign(inputGraph, privateKey, "\"cert\"");
            // use re-implemented method that assembles differently
            // namely, just adds corresponding triples to the default graph of the dataset
            //algorithm.assemble(inputGraph, "_:sigGraph");
            //assemble(inputGraph);
        }




        // transform resulting output graph into the Model that is signed

        return null;
    }

    @Override
    public boolean verify(Dataset signedDataset, PublicKey publicKey) throws Exception {
        return false;
    }
}

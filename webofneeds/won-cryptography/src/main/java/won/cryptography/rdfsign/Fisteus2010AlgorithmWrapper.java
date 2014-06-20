package won.cryptography.rdfsign;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;
import de.uni_koblenz.aggrimm.icp.crypto.sign.algorithm.algorithm.SignatureAlgorithmFisteus2010;
import de.uni_koblenz.aggrimm.icp.crypto.sign.graph.GraphCollection;
import de.uni_koblenz.aggrimm.icp.crypto.sign.trigplus.TriGPlusWriter;

import java.io.IOException;
import java.security.*;

/**
 * Created by ypanchenko on 18.06.2014.
 */
public class Fisteus2010AlgorithmWrapper implements SigningAlgorithm2 {

    private SignatureAlgorithmFisteus2010 algorithm = new SignatureAlgorithmFisteus2010();
    private static String envHashAlgorithm = "sha-256";

    @Override
    public Dataset sign(Model model, PrivateKey privateKey) throws Exception {

        // transform model/read model into the GraphCollection
        GraphCollection inputGraph = null;

        algorithm.canonicalize(inputGraph);
        algorithm.postCanonicalize(inputGraph);
        algorithm.hash(inputGraph, envHashAlgorithm);
        algorithm.postHash(inputGraph);
        algorithm.sign(inputGraph, privateKey, "\"cert\"");
        algorithm.assemble(inputGraph, "_:sigGraph");

        // transform resulting output graph into the Model that is signed

        return null;
    }

    @Override
    public boolean verify(Dataset signedDataset, PublicKey publicKey) throws Exception {
        return false;
    }
}

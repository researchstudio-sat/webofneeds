package won.cryptography.rdfsign;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;

import java.io.IOException;
import java.security.*;

/**
 * Created by ypanchenko on 18.06.2014.
 */
public interface SigningAlgorithm2 {

    // canonicalize, hash, sign, add signature(s) (assemble)
    public Dataset sign(Dataset dataset, PrivateKey privateKey) throws Exception;
    // alternatively list of models as input


    // canonicalize, hash, verify, maybe output VerificationResult (e.g. if parts are
    // verified sucessfully and parts are not)
    public boolean verify(Dataset dataset, PublicKey publicKey) throws Exception;

    //public boolean verify(Model model, PublicKey publicKey) throws Exception;
}

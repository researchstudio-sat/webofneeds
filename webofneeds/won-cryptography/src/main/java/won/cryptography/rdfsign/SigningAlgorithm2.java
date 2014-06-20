package won.cryptography.rdfsign;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;

import java.io.IOException;
import java.security.*;

/**
 * Created by ypanchenko on 18.06.2014.
 */
public interface SigningAlgorithm2 {

    public Dataset sign(Model model, PrivateKey privateKey) throws Exception;

    public boolean verify(Dataset model, PublicKey publicKey) throws Exception;

    //public boolean verify(Model model, PublicKey publicKey) throws Exception;
}

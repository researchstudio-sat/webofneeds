package won.cryptography.rdfsign;

import com.hp.hpl.jena.rdf.model.Model;

import java.io.IOException;
import java.security.*;

/**
 * Created by ypanchenko on 12.06.2014.
 */
//TODO each algorithm should have its own URI, so that from
// uri one could recover what algorithm to use when e.g.
// verifying the attached signature
public interface SigningAlgorithm {

    public String sign(Model model, PrivateKey privateKey) throws Exception;

    public boolean verify(Model model, PublicKey publicKey, String signature) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException, IOException, NoSuchProviderException;

    //public boolean verify(Model model, PublicKey publicKey) throws Exception;
}

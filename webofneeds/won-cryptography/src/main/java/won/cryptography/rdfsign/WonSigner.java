package won.cryptography.rdfsign;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;
import de.uni_koblenz.aggrimm.icp.crypto.sign.algorithm.SignatureAlgorithmInterface;
import de.uni_koblenz.aggrimm.icp.crypto.sign.graph.GraphCollection;
import de.uni_koblenz.aggrimm.icp.crypto.sign.graph.SignatureData;
import org.apache.commons.codec.binary.Base64;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.protocol.message.SignatureReference;

import java.security.PrivateKey;
import java.security.Provider;
import java.security.Signature;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by ypanchenko on 12.06.2014.
 */
public class WonSigner
{

  private final Logger logger = LoggerFactory.getLogger(getClass());
  // TODO make it configurable which algorithm is used RSA or ECDSA

  public static final String SIGNING_ALGORITHM_NAME = "NONEwithECDSA";
  public static final String SIGNING_ALGORITHM_PROVIDER = "BC";
  //TODO which hashing algorithm to use?
  public static final String ENV_HASH_ALGORITHM = "sha-256";


  private SignatureAlgorithmInterface algorithm;
  private Dataset dataset;


  public WonSigner(Dataset dataset, SignatureAlgorithmInterface algorithm) {
    this.dataset = dataset;
    this.algorithm = algorithm;

    Provider provider = new BouncyCastleProvider();
  }


  /**
   * Signs the graphs of the dataset with the provided private key and referencing
   * the provided certificate/public key uri in signature, this uri will be used
   * to extract key by the verification party.
   *
   * @param privateKey the private key
   * @param cert the certificate reference (where the public key can be found for verification)
   * @param graphsToSign the names of the graphs that have to be signed. If not provided -
   * all the graphs that don't have signatures will be signed.
   * @throws Exception
   */
  //TODO chng exceptions to won exceptions?
  public List<SignatureReference> sign(PrivateKey privateKey, String cert, String ... graphsToSign) throws Exception {

    List<SignatureReference> sigRefs = new ArrayList<>(graphsToSign.length);

    for (String name : graphsToSign) {
      Model model = dataset.getNamedModel(name);
      //TODO should be generated in a more proper way and not here - check of the name already exists etc.
      String sigName = name + "-sig";
      // create GraphCollection with one NamedGraph that corresponds to this Model
      GraphCollection inputGraph = ModelConverter.modelToGraphCollection(name, dataset);
      // sign the NamedGraph inside that GraphCollection
      String sigValue = signNamedGraph(inputGraph, privateKey, cert);
      // assemble resulting signature into the original Dataset
      WonAssembler.assemble(inputGraph, dataset, sigName);

      SignatureReference sigRef = new SignatureReference(null, name, sigName, sigValue);
      sigRefs.add(sigRef);
    }

    return sigRefs;
  }

  public List<SignatureReference> sign(PrivateKey privateKey, String cert, Collection<String> graphsToSign) throws
    Exception {
    String[] array = new String[graphsToSign.size()];
    return sign(privateKey, cert, graphsToSign.toArray(array));
  }

  private String signNamedGraph(final GraphCollection inputWithOneNamedGraph, final PrivateKey privateKey, String cert)
    throws Exception {
    this.algorithm.canonicalize(inputWithOneNamedGraph);
    this.algorithm.postCanonicalize(inputWithOneNamedGraph);
    this.algorithm.hash(inputWithOneNamedGraph, ENV_HASH_ALGORITHM);
    this.algorithm.postHash(inputWithOneNamedGraph);
    return sign(inputWithOneNamedGraph, privateKey, cert);
  }

  private String sign(GraphCollection gc, PrivateKey privateKey, String verificationCertificate) throws Exception {

    if (verificationCertificate == null) {
      verificationCertificate = "\"cert\"";
    } else {
      verificationCertificate = "<" + verificationCertificate + ">";
    }
    // Signature Data existing?
    if (!gc.hasSignature()) {
      throw new Exception("GraphCollection has no signature data. Call 'canonicalize' and 'hash' methods first.");
    }

    // Get Signature Data
    SignatureData sigData = gc.getSignature();
    // Sign
    Signature sig = Signature.getInstance(SIGNING_ALGORITHM_NAME, SIGNING_ALGORITHM_PROVIDER);
    sig.initSign(privateKey);
    sig.update(sigData.getHash().toByteArray());

    byte[] signatureBytes = sig.sign();
    //String signature = new BASE64Encoder().encode(signatureBytes);
    String signature = Base64.encodeBase64String(signatureBytes);


    //Update Signature Data
    sigData.setSignature("\"\"\"" + signature + "\"\"\"");
    sigData.setSignatureMethod(privateKey.getAlgorithm().toLowerCase());
    sigData.setVerificationCertificate(verificationCertificate);

    return signature;
  }

}

package won.cryptography.rdfsign;

import java.io.StringWriter;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Signature;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.RDF;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uni_koblenz.aggrimm.icp.crypto.sign.algorithm.SignatureAlgorithmInterface;
import de.uni_koblenz.aggrimm.icp.crypto.sign.algorithm.algorithm.SignatureAlgorithmFisteus2010;
import de.uni_koblenz.aggrimm.icp.crypto.sign.graph.GraphCollection;
import de.uni_koblenz.aggrimm.icp.crypto.sign.graph.SignatureData;
import won.protocol.message.WonSignatureData;
import won.protocol.vocabulary.SFSIG;

/**
 * Created by ypanchenko on 12.06.2014.
 */
public class WonSigner {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    // TODO make it configurable which algorithm is used RSA or ECDSA

    public static final String SIGNING_ALGORITHM_NAME = "NONEwithECDSA";
    public static final String SIGNING_ALGORITHM_PROVIDER = "BC";
    // TODO which hashing algorithm to use?
    public static final String ENV_HASH_ALGORITHM = "sha-256";

    private SignatureAlgorithmInterface algorithm;
    private Dataset dataset;

    public static final Model defaultGraphSigningMethod;
    static {
        // initialize a model with the triples indicating the default graph signing method, so we are not
        // required to add it to every signature
        defaultGraphSigningMethod = ModelFactory.createDefaultModel();
        Resource bNode = defaultGraphSigningMethod.createResource();
        bNode.addProperty(RDF.type, SFSIG.GRAPH_SIGNING_METHOD);
        bNode.addProperty(SFSIG.HAS_DIGEST_METHOD, SFSIG.DIGEST_METHOD_SHA_256);
        bNode.addProperty(SFSIG.HAS_GRAPH_CANONICALIZATION_METHOD, SFSIG.GRAPH_CANONICALIZATION_METHOD_Fisteus2010);
        bNode.addProperty(SFSIG.HAS_GRAPH_DIGEST_METHOD, SFSIG.GRAPH_DIGEST_METHOD_Fisteus2010);
        bNode.addProperty(SFSIG.HAS_GRAPH_SERIALIZATION_METHOD, SFSIG.GRAPH_SERIALIZATION_METHOD_TRIG);
        bNode.addProperty(SFSIG.HAS_SIGNATURE_METHOD, SFSIG.SIGNATURE_METHOD_ECDSA);
    }

    public WonSigner(Dataset dataset) {
        this.dataset = dataset;
        // default algorithm: Fisteus2010
        this.algorithm = new SignatureAlgorithmFisteus2010();

        Provider provider = new BouncyCastleProvider();
    }

    /**
     * Signs the graphs of the dataset with the provided private key and referencing the provided certificate/public key
     * uri in signature, this uri will be used to extract key by the verification party.
     *
     * @param privateKey
     *            the private key
     * @param cert
     *            the certificate reference (where the public key can be found for verification)
     * @param graphsToSign
     *            the names of the graphs that have to be signed. If not provided - all the graphs that don't have
     *            signatures will be signed.
     * @throws Exception
     */
    // TODO chng exceptions to won exceptions?
    public List<WonSignatureData> sign(PrivateKey privateKey, String cert, PublicKey publicKey, String... graphsToSign)
            throws Exception {

        List<WonSignatureData> sigRefs = new ArrayList<>(graphsToSign.length);
        MessageDigest md = MessageDigest.getInstance(ENV_HASH_ALGORITHM, SIGNING_ALGORITHM_PROVIDER);
        String fingerprint = Base64.getEncoder().encodeToString(md.digest(publicKey.getEncoded()));

        for (String signedGraphUri : graphsToSign) {
            // TODO should be generated in a more proper way and not here - check of the name already exists etc.
            if (logger.isDebugEnabled()) {
                StringWriter sw = new StringWriter();
                RDFDataMgr.write(sw, dataset.getNamedModel(signedGraphUri), Lang.TRIG);
                logger.debug("signing graph {} with content: {}", graphsToSign, sw.toString());
            }
            String signatureUri = signedGraphUri + "-sig";
            // create GraphCollection with one NamedGraph that corresponds to this Model
            GraphCollection inputGraph = ModelConverter.modelToGraphCollection(signedGraphUri, dataset);
            // sign the NamedGraph inside that GraphCollection
            SignatureData sigValue = signNamedGraph(inputGraph, privateKey, cert);
            String hash = new String(Base64.getEncoder().encodeToString(sigValue.getHash().toByteArray()));

            WonSignatureData sigRef = new WonSignatureData(signedGraphUri, signatureUri, sigValue.getSignature(), hash,
                    fingerprint, cert);
            sigRefs.add(sigRef);
        }

        return sigRefs;
    }

    public List<WonSignatureData> sign(PrivateKey privateKey, String cert, PublicKey publicKey,
            Collection<String> graphsToSign) throws Exception {
        String[] array = new String[graphsToSign.size()];
        return sign(privateKey, cert, publicKey, graphsToSign.toArray(array));
    }

    private de.uni_koblenz.aggrimm.icp.crypto.sign.graph.SignatureData signNamedGraph(
            final GraphCollection inputWithOneNamedGraph, final PrivateKey privateKey, String cert) throws Exception {
        this.algorithm.canonicalize(inputWithOneNamedGraph);
        this.algorithm.postCanonicalize(inputWithOneNamedGraph);
        this.algorithm.hash(inputWithOneNamedGraph, ENV_HASH_ALGORITHM);
        this.algorithm.postHash(inputWithOneNamedGraph);
        return sign(inputWithOneNamedGraph, privateKey, cert);
    }

    private SignatureData sign(GraphCollection gc, PrivateKey privateKey, String verificationCertificate)
            throws Exception {

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
        // String signature = new BASE64Encoder().encode(signatureBytes);
        String signature = Base64.getEncoder().encodeToString(signatureBytes);

        // Update Signature Data
        sigData.setSignature(signature);
        sigData.setSignatureMethod(privateKey.getAlgorithm().toLowerCase());
        sigData.setVerificationCertificate(verificationCertificate);

        return sigData;
    }

}

package won.cryptography.rdfsign;

import java.io.StringWriter;
import java.lang.invoke.MethodHandles;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Signature;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.jena.ext.com.google.common.collect.Streams;
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

import de.uni_koblenz.aggrimm.icp.crypto.sign.graph.GraphCollection;
import de.uni_koblenz.aggrimm.icp.crypto.sign.graph.SignatureData;
import io.ipfs.multihash.Multihash.Type;
import won.protocol.message.WonSignatureData;
import won.protocol.vocabulary.SFSIG;

/**
 * Created by ypanchenko on 12.06.2014.
 */
public class WonSigner {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    // TODO make it configurable which algorithm is used RSA or ECDSA
    public static final String SIGNING_ALGORITHM_NAME = "NONEwithECDSA";
    public static final String SIGNING_ALGORITHM_PROVIDER = "BC";
    // TODO which hashing algorithm to use?
    public static final String ENV_HASH_ALGORITHM = "sha-256";
    public static final Type HASH_ALGORITHM_FOR_MULTIHASH = Type.sha2_256;
    private Dataset dataset;
    private WonHasher hasher;
    private static final Model defaultGraphSigningMethod;
    static {
        // initialize a model with the triples indicating the default graph signing
        // method, so we are not
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
        Provider provider = new BouncyCastleProvider();
        this.hasher = new WonHasher();
    }

    /**
     * Signs the graphs of the dataset with the provided private key and referencing
     * the provided certificate/public key uri in signature, this uri will be used
     * to extract key by the verification party.
     *
     * @param privateKey the private key
     * @param cert the certificate reference (where the public key can be found for
     * verification)
     * @param graphsToSign the names of the graphs that have to be signed. If not
     * provided - all the graphs that don't have signatures will be signed.
     * @throws Exception
     */
    // TODO chng exceptions to won exceptions?
    public List<WonSignatureData> signNamedGraphsSeparately(PrivateKey privateKey, String cert, PublicKey publicKey,
                    String... graphsToSign)
                    throws Exception {
        List<WonSignatureData> sigRefs = new ArrayList<>(graphsToSign.length);
        String fingerprint = WonHasher.hashToString(publicKey.getEncoded());
        for (String signedGraphUri : graphsToSign) {
            // TODO should be generated in a more proper way and not here - check of the
            // name already exists etc.
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
            String hash = WonHasher.hashToString(sigValue.getHash());
            WonSignatureData sigRef = new WonSignatureData(Arrays.asList(signedGraphUri), signatureUri,
                            sigValue.getSignature(), hash,
                            fingerprint, cert);
            sigRefs.add(sigRef);
        }
        return sigRefs;
    }

    public WonSignatureData signWholeDataset(PrivateKey privateKey, String cert, PublicKey publicKey,
                    String signatureUri)
                    throws Exception {
        String fingerprint = WonHasher.hashToString(publicKey.getEncoded());
        if (logger.isDebugEnabled()) {
            StringWriter sw = new StringWriter();
            RDFDataMgr.write(sw, dataset, Lang.TRIG);
            logger.debug("signing dataset with content: {}", sw.toString());
        }
        List<String> graphURIs = Streams.stream(dataset.listNames()).collect(Collectors.toList());
        // create GraphCollection with one NamedGraph that corresponds to this Model
        GraphCollection inputGraphCollection = ModelConverter.fromDataset(dataset);
        // sign the NamedGraph inside that GraphCollection
        SignatureData sigValue = sign(hasher.hashNamedGraphForSigning(inputGraphCollection), privateKey, cert);
        String hash = WonHasher.hashToString(sigValue.getHash());
        WonSignatureData sigRef = new WonSignatureData(graphURIs, signatureUri, sigValue.getSignature(), hash,
                        fingerprint, cert);
        return sigRef;
    }

    public List<WonSignatureData> sign(PrivateKey privateKey, String cert, PublicKey publicKey,
                    Collection<String> graphsToSign) throws Exception {
        String[] array = new String[graphsToSign.size()];
        return signNamedGraphsSeparately(privateKey, cert, publicKey, graphsToSign.toArray(array));
    }

    private de.uni_koblenz.aggrimm.icp.crypto.sign.graph.SignatureData signNamedGraph(
                    final GraphCollection inputWithOneNamedGraph, final PrivateKey privateKey, String cert)
                    throws Exception {
        return sign(hasher.hashNamedGraphForSigning(inputWithOneNamedGraph), privateKey, cert);
    }

    private SignatureData sign(SignatureData sigData, PrivateKey privateKey, String verificationCertificate)
                    throws Exception {
        if (verificationCertificate == null) {
            verificationCertificate = "\"cert\"";
        } else {
            verificationCertificate = "<" + verificationCertificate + ">";
        }
        // Signature Data existing?
        if (sigData == null) {
            throw new Exception("GraphCollection has no signature data. Call 'canonicalize' and 'hash' methods first.");
        }
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

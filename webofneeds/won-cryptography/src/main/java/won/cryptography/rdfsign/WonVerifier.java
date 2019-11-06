package won.cryptography.rdfsign;

import static won.cryptography.rdfsign.WonSigner.*;

import java.io.StringWriter;
import java.lang.invoke.MethodHandles;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Base64;
import java.util.Map;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uni_koblenz.aggrimm.icp.crypto.sign.algorithm.SignatureAlgorithmInterface;
import de.uni_koblenz.aggrimm.icp.crypto.sign.algorithm.algorithm.SignatureAlgorithmFisteus2010;
import de.uni_koblenz.aggrimm.icp.crypto.sign.graph.GraphCollection;
import won.protocol.message.WonSignatureData;
import won.protocol.util.Prefixer;
import won.protocol.util.RdfUtils;
import won.protocol.util.WonRdfUtils;
import won.protocol.vocabulary.WONMSG;

/**
 * User: ypanchenko Date: 15.07.2014
 */
public class WonVerifier {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final Dataset dataset;
    private final SignatureVerificationState verificationState = new SignatureVerificationState();

    public WonVerifier(Dataset dataset) {
        Provider provider = new BouncyCastleProvider();
        this.dataset = dataset;
        prepareForVerifying();
    }

    /**
     * find corresponding signature graphs for all non-signature graphs
     */
    private void prepareForVerifying() {
        for (String graphURI : RdfUtils.getModelNames(dataset)) {
            Model model = dataset.getNamedModel(graphURI);
            if (WonRdfUtils.SignatureUtils.isSignatureGraph(graphURI, model)) {
                addSignatureToResult(graphURI, model);
            } else {
                verificationState.addSignedGraphName(graphURI);
                addSignatureReferenceToResult(graphURI, model);
            }
        }
    }

    public SignatureVerificationState getVerificationResult() {
        return verificationState;
    }

    // TODO exceptions
    public boolean verify(Map<String, PublicKey> publicKeys) throws Exception {
        // check if there are any signatures at all
        if (verificationState.getSignatures().size() == 0) {
            verificationState.verificationFailed("No signatures found");
            return verificationState.isVerificationPassed();
        }
        // check that the default graph is empty
        if (dataset.getDefaultModel().listStatements().hasNext()) {
            verificationState.verificationFailed("unsigned data found in default graph");
            return verificationState.isVerificationPassed();
        }
        // Get algorithms to use from signature data
        SignatureAlgorithmInterface canonicAlgorithm = new SignatureAlgorithmFisteus2010();
        SignatureAlgorithmInterface hashingAlgorithm = canonicAlgorithm;
        MessageDigest messageDigest = MessageDigest.getInstance(ENV_HASH_ALGORITHM, SIGNING_ALGORITHM_PROVIDER);
        // verify each signature's graph
        for (WonSignatureData wonSignatureData : verificationState.getSignatures()) {
            // extract signature graph, signature data and corresponding signed graph
            if (logger.isDebugEnabled()) {
                String loaded = publicKeys.containsKey(wonSignatureData.getVerificationCertificateUri()) ? "loaded"
                                : "NOT LOADED";
                logger.debug("checking signature {} by certificate {}, which is {}",
                                new Object[] { wonSignatureData.getSignatureUri(),
                                                wonSignatureData.getVerificationCertificateUri(), loaded });
            }
            // make sure the signed graph specified in signature exists in the message
            if (!dataset.containsNamedModel(wonSignatureData.getSignedGraphUri())) {
                logger.debug("cannot verify signature {} as it is not part of this message ",
                                wonSignatureData.getSignatureUri());
                continue;
                // TODO: fetch the external reference and check it here
                // verificationState.setVerificationFailed(wonSignatureData.getSignatureUri(),
                // "No signed graph found for " +
                // "signature " + wonSignatureData.getSignatureUri());
                // return verificationState.isVerificationPassed();
            }
            // is the signature there?
            String sigString = wonSignatureData.getSignatureValue();
            if (sigString == null) {
                verificationState.setVerificationFailed(wonSignatureData.getSignatureUri(),
                                "Failed to compute a signature value " + wonSignatureData.getSignatureUri());
                return verificationState.isVerificationPassed();
            }
            if (sigString.length() == 0) {
                verificationState.setVerificationFailed(wonSignatureData.getSignatureUri(),
                                "Computed an empty signature value " + wonSignatureData.getSignatureUri());
                return verificationState.isVerificationPassed();
            }
            // do we have the public key?
            PublicKey publicKey = publicKeys.get(wonSignatureData.getVerificationCertificateUri());
            if (publicKey == null) {
                verificationState.setVerificationFailed(wonSignatureData.getSignatureUri(),
                                "No public key found for " + wonSignatureData.getSignatureUri());
                if (logger.isDebugEnabled()) {
                    logger.debug("offending message:\n" + RdfUtils.toString(Prefixer.setPrefixes(dataset)));
                }
                return verificationState.isVerificationPassed();
            }
            // check if its fingerprint matches the fingerprint in the signature
            String fingerprint = Base64.getEncoder().encodeToString(messageDigest.digest(publicKey.getEncoded()));
            if (!wonSignatureData.getPublicKeyFingerprint().equals(fingerprint)) {
                verificationState.setVerificationFailed(wonSignatureData.getSignatureUri(),
                                "Fingerprint computed for the " + "specified public key "
                                                + wonSignatureData.getVerificationCertificateUri() + " is "
                                                + fingerprint + ", "
                                                + "which differs from the value found in signature "
                                                + wonSignatureData.getSignatureUri());
                return verificationState.isVerificationPassed();
            }
            // normalize, hash and post-hash signed graph data
            GraphCollection inputGraph = ModelConverter.modelToGraphCollection(wonSignatureData.getSignedGraphUri(),
                            dataset);
            canonicAlgorithm.canonicalize(inputGraph);
            canonicAlgorithm.postCanonicalize(inputGraph);
            hashingAlgorithm.hash(inputGraph, ENV_HASH_ALGORITHM);
            hashingAlgorithm.postHash(inputGraph);
            // check the hash of the data. It must be identical to the hash in the signature
            BigInteger hashValue = inputGraph.getSignature().getHash();
            String hashString = Base64.getEncoder().encodeToString(hashValue.toByteArray());
            if (!wonSignatureData.getHash().equals(hashString)) {
                verificationState.setVerificationFailed(wonSignatureData.getSignatureUri(),
                                "Computed hash value " + hashString + " differs from value "
                                                + wonSignatureData.getHash() + " found in signature "
                                                + wonSignatureData.getSignatureUri());
                if (logger.isDebugEnabled()) {
                    StringWriter sw = new StringWriter();
                    RDFDataMgr.write(sw, dataset.getNamedModel(wonSignatureData.getSignedGraphUri()), Lang.TRIG);
                    logger.debug("wrong signature hash for graph {} with content: {}",
                                    wonSignatureData.getSignedGraphUri(), sw.toString());
                }
                return verificationState.isVerificationPassed();
            }
            // verify the signature
            Signature sig = Signature.getInstance(WonSigner.SIGNING_ALGORITHM_NAME, SIGNING_ALGORITHM_PROVIDER);
            sig.initVerify(publicKey);
            sig.update(hashValue.toByteArray());
            // Verify
            byte[] sigBytes = Base64.getDecoder().decode(sigString);
            if (!sig.verify(sigBytes)) {
                verificationState.setVerificationFailed(wonSignatureData.getSignatureUri(),
                                "Failed to verify " + wonSignatureData.getSignatureUri() + " with public key "
                                                + wonSignatureData.getVerificationCertificateUri());
                // interrupt verification process if one of the graph's verification fails
                return verificationState.isVerificationPassed();
            }
        }
        return verificationState.isVerificationPassed();
    }

    private void addSignatureToResult(final String graphUri, final Model model) {
        WonSignatureData wonSignatureData = WonRdfUtils.SignatureUtils.extractWonSignatureData(graphUri, model);
        if (wonSignatureData != null && wonSignatureData.getSignatureValue() != null) {
            verificationState.addSignatureData(wonSignatureData);
        }
    }

    private void addSignatureReferenceToResult(final String graphURI, final Model model) {
        RDFNode tempNode = null;
        StmtIterator si = model.listStatements(null, WONMSG.containsSignature, tempNode);
        while (si.hasNext()) {
            WonSignatureData sigRef = WonRdfUtils.SignatureUtils
                            .extractWonSignatureData(si.nextStatement().getObject().asResource());
            verificationState.addSignatureData(sigRef);
        }
    }
}

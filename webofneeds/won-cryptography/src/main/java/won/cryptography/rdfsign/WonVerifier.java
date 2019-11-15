package won.cryptography.rdfsign;

import static won.cryptography.rdfsign.WonSigner.*;

import java.io.StringWriter;
import java.lang.invoke.MethodHandles;
import java.math.BigInteger;
import java.net.URI;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uni_koblenz.aggrimm.icp.crypto.sign.graph.GraphCollection;
import de.uni_koblenz.aggrimm.icp.crypto.sign.graph.SignatureData;
import won.protocol.message.WonMessage;
import won.protocol.message.WonSignatureData;
import won.protocol.util.Prefixer;
import won.protocol.util.RdfUtils;
import won.protocol.util.WonMessageUriHelper;
import won.protocol.util.WonRdfUtils;
import won.protocol.vocabulary.WONMSG;

/**
 * User: ypanchenko Date: 15.07.2014
 */
public class WonVerifier {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final Dataset dataset;
    private final SignatureVerificationState verificationState = new SignatureVerificationState();
    private final WonHasher hasher = new WonHasher();
    private URI messageURI;

    public WonVerifier(WonMessage message) {
        Provider provider = new BouncyCastleProvider();
        this.dataset = message.getCompleteDataset();
        this.messageURI = message.getMessageURIRequired();
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

    private boolean checkMessageURI() throws Exception {
        String hashId = hasher.calculateHashIdForDataset(dataset);
        String idFromMessageURI = WonMessageUriHelper.getIdFromMessageURI(messageURI);
        if (Objects.equals(hashId, idFromMessageURI)) {
            verificationState.verificationFailed(
                            "messageURI is invalid: expected based on message content: '"
                                            + hashId + "', expected '" + idFromMessageURI + "'");
            return false;
        } else {
            return true;
        }
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
        if (!checkMessageURI()) {
            return verificationState.isVerificationPassed();
        }
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
            List<String> signedGraphs = wonSignatureData.getSignedGraphUris();
            for (String signedGraph : signedGraphs) {
                if (!dataset.containsNamedModel(signedGraph)) {
                    verificationState.verificationFailed(
                                    "Found signature of graph " + signedGraph + " that is not part of this message");
                }
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
            String fingerprint = WonHasher.hashToString(publicKey.getEncoded());
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
            GraphCollection inputGraph = ModelConverter.modelsToGraphCollection(dataset,
                            wonSignatureData.getSignedGraphUris()
                                            .toArray(new String[wonSignatureData.getSignedGraphUris().size()]));
            SignatureData sigData = this.hasher.hashNamedGraphForSigning(inputGraph);
            // check the hash of the data. It must be identical to the hash in the signature
            BigInteger hashValue = sigData.getHash();
            String hashString = WonHasher.hashToString(hashValue);
            if (!wonSignatureData.getHash().equals(hashString)) {
                verificationState.setVerificationFailed(wonSignatureData.getSignatureUri(),
                                "Computed hash value " + hashString + " differs from value "
                                                + wonSignatureData.getHash() + " found in signature "
                                                + wonSignatureData.getSignatureUri());
                if (logger.isDebugEnabled()) {
                    StringWriter sw = new StringWriter();
                    for (String signedGraphUri : wonSignatureData.getSignedGraphUris()) {
                        RDFDataMgr.write(sw, dataset.getNamedModel(signedGraphUri), Lang.TRIG);
                    }
                    logger.debug("wrong signature hash for graphs {} with content: {}",
                                    wonSignatureData.getSignedGraphUris(), sw.toString());
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

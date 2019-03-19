package won.protocol.message.processor.impl;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.List;
import java.util.Map;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;

import won.cryptography.rdfsign.SignatureVerificationState;
import won.cryptography.rdfsign.SigningStage;
import won.cryptography.rdfsign.WonSigner;
import won.cryptography.rdfsign.WonVerifier;
import won.protocol.message.WonMessage;
import won.protocol.message.WonSignatureData;
import won.protocol.util.WonRdfUtils;
import won.protocol.vocabulary.WONMSG;

/**
 * User: ypanchenko Date: 08.04.2015
 */
public class WonMessageSignerVerifier {

    public static WonMessage sign(PrivateKey privateKey, PublicKey publicKey, String privateKeyUri, WonMessage message)
            throws Exception {

        Dataset msgDataset = message.getCompleteDataset();
        SigningStage sigStage = new SigningStage(message);

        addUnreferencedSigReferences(msgDataset, sigStage);

        WonSigner signer = new WonSigner(msgDataset);
        signContents(msgDataset, sigStage, signer, privateKey, privateKeyUri, publicKey);
        signEnvelopes(msgDataset, sigStage, signer, privateKey, privateKeyUri, publicKey);

        return new WonMessage(msgDataset);
    }

    /**
     * If the provided signing stage has unsigned content graphs, sign them, add signature graphs to the dataset, and
     * add signatures to the envelope graph that has contains envelope property referencing signed by that signature
     * envelope graph
     * 
     * @param msgDataset
     * @param sigStage
     * @param signer
     * @param privateKey
     * @param privateKeyUri
     */
    private static void signEnvelopes(final Dataset msgDataset, final SigningStage sigStage, final WonSigner signer,
            final PrivateKey privateKey, final String privateKeyUri, final PublicKey publicKey) throws Exception {

        List<String> envUris = sigStage.getUnsignedEnvUrisOrderedByContainment();
        WonSignatureData wonSignatureData = null;
        String outerEnvUri = null;
        for (String envUri : sigStage.getUnsignedEnvUrisOrderedByContainment()) {
            if (wonSignatureData != null) {
                // this is the signature of the envelope we signed in the last iteration.
                // add it to the current one:
                addSignature(wonSignatureData, envUri, msgDataset, true);
            }
            wonSignatureData = signer.sign(privateKey, privateKeyUri, publicKey, envUri).get(0);
            outerEnvUri = envUri;
        }
        // this is the signature of the outermost envelopoe. put it in a new graph.
        msgDataset.addNamedModel(wonSignatureData.getSignatureUri(), ModelFactory.createDefaultModel());
        addSignature(wonSignatureData, wonSignatureData.getSignatureUri(), msgDataset, false);
    }

    /**
     * Adds the signature to the specified graph.
     * 
     * @param sigData
     * @param graphUri
     * @param msgDataset
     * @param graphIsEnvelope
     *            if true, a msg:containsSignature property is added to the graph URI
     */
    public static void addSignature(final WonSignatureData sigData, final String graphUri, final Dataset msgDataset,
            boolean graphIsEnvelope) {

        Model envelopeGraph = msgDataset.getNamedModel(graphUri);
        Resource envelopeResource = envelopeGraph.createResource(graphUri);
        Resource sigNode = envelopeGraph.createResource(sigData.getSignatureUri());
        if (graphIsEnvelope) {
            // only connect envelope to signature. pure signature graphs are not connected this way.
            envelopeResource.addProperty(WONMSG.CONTAINS_SIGNATURE_PROPERTY, sigNode);
        }
        WonRdfUtils.SignatureUtils.addSignature(sigNode, sigData);
    }

    /**
     * If the provided signing stage has unsigned content graphs, sign them. This adds the signature triples to the
     * graph, add signature graphs to the dataset, and add signature references of those signatures into the envelope
     * graph that has has content property referencing signed by that signature content graph
     * 
     * @param msgDataset
     * @param sigStage
     * @param signer
     * @param privateKey
     * @param privateKeyUri
     */
    private static void signContents(final Dataset msgDataset, final SigningStage sigStage, final WonSigner signer,
            final PrivateKey privateKey, final String privateKeyUri, final PublicKey publicKey) throws Exception {
        List<WonSignatureData> sigRefs = signer.sign(privateKey, privateKeyUri, publicKey,
                sigStage.getUnsignedContentUris());
        for (WonSignatureData sigRef : sigRefs) {
            String envUri = sigStage.getEnvelopeUriContainingContent(sigRef.getSignedGraphUri());
            addSignature(sigRef, envUri, msgDataset, true);
        }
    }

    /**
     * If the provided signing stage has signature graphs that are not referenced from any envelope graphs, they should
     * be moved to the innermost not-signed envelope graph. The signature graph is to be deleted.
     * 
     * @param msgDataset
     * @param sigStage
     */
    private static void addUnreferencedSigReferences(final Dataset msgDataset, final SigningStage sigStage) {

        String innemostUnsignedEnvUri = null;
        List<String> envUris = sigStage.getUnsignedEnvUrisOrderedByContainment();
        if (envUris.isEmpty()) {
            return;
        } else {
            innemostUnsignedEnvUri = envUris.get(0);
        }
        WonSignatureData sigRef = sigStage.getOutermostSignature();
        if (sigRef != null) {
            addSignature(sigRef, innemostUnsignedEnvUri, msgDataset, true);
            msgDataset.removeNamedModel(sigRef.getSignatureUri());
        }
    }

    public static SignatureVerificationState verify(Map<String, PublicKey> keys, WonMessage message) throws Exception {
        Dataset dataset = message.getCompleteDataset();
        WonVerifier verifier = new WonVerifier(dataset);
        verifier.verify(keys);
        return verifier.getVerificationResult();
    }

}

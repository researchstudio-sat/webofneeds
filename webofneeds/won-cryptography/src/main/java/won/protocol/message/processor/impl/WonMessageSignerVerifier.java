package won.protocol.message.processor.impl;

import java.net.URI;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;

import won.cryptography.rdfsign.SignatureVerificationState;
import won.cryptography.rdfsign.SigningStage;
import won.cryptography.rdfsign.WonHasher;
import won.cryptography.rdfsign.WonSigner;
import won.cryptography.rdfsign.WonVerifier;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageUtils;
import won.protocol.message.WonSignatureData;
import won.protocol.util.RdfUtils;
import won.protocol.util.WonMessageUriHelper;
import won.protocol.util.WonRdfUtils;
import won.protocol.vocabulary.WONMSG;

/**
 * User: ypanchenko Date: 08.04.2015
 */
public class WonMessageSignerVerifier {
    public static WonMessage seal(WonMessage message) throws Exception {
        Dataset ds = message.getCompleteDataset();
        calculateMessageUriForContent(ds);
        return WonMessage.of(ds);
    }

    public static WonMessage signAndSeal(PrivateKey privateKey, PublicKey publicKey, String privateKeyUri,
                    WonMessage message)
                    throws Exception {
        Dataset msgDataset = message.getCompleteDataset();
        SigningStage sigStage = new SigningStage(message);
        WonSigner signer = new WonSigner(msgDataset);
        if (message.getMessageTypeRequired().isContentSignedSeparately()) {
            signContents(msgDataset, sigStage, signer, privateKey, privateKeyUri, publicKey);
            signEnvelope(msgDataset, sigStage, signer, privateKey, privateKeyUri, publicKey,
                            message.getMessageURIRequired());
        } else {
            signWholeMessage(msgDataset, sigStage, signer, privateKey, privateKeyUri, publicKey,
                            message.getMessageURIRequired());
        }
        calculateMessageUriForContent(msgDataset);
        return WonMessage.of(msgDataset);
    }

    public static SignatureVerificationState verify(Map<String, PublicKey> keys, WonMessage message) throws Exception {
        Dataset ds = message.getCompleteDataset();
        RdfUtils.renameResourceWithPrefix(ds, message.getMessageURIRequired().toString(),
                        WonMessageUriHelper.getSelfUri().toString());
        WonVerifier verifier = new WonVerifier(WonMessage.of(ds));
        verifier.verify(keys);
        return verifier.getVerificationResult();
    }

    /**
     * If the provided signing stage has unsigned content graphs, sign them, add
     * signature graphs to the dataset, and add signatures to the envelope graph
     * that has contains envelope property referencing signed by that signature
     * envelope graph
     * 
     * @param msgDataset
     * @param sigStage
     * @param signer
     * @param privateKey
     * @param privateKeyUri
     */
    private static void signEnvelope(final Dataset msgDataset, final SigningStage sigStage, final WonSigner signer,
                    final PrivateKey privateKey, final String privateKeyUri, final PublicKey publicKey, URI messageURI)
                    throws Exception {
        WonSignatureData wonSignatureData = null;
        // String outerEnvUri = null;
        String envUri = sigStage.getEnvelopeUri();
        wonSignatureData = signer.signNamedGraphsSeparately(privateKey, privateKeyUri, publicKey, envUri).get(0);
        Objects.requireNonNull(wonSignatureData);
        // this is the signature of the outermost envelopoe. put it in a new graph.
        String signatureUri = messageURI.toString() + WonMessage.SIGNATURE_URI_SUFFIX;
        wonSignatureData.setSignatureUri(signatureUri);
        msgDataset.addNamedModel(signatureUri,
                        ModelFactory.createDefaultModel());
        addSignature(wonSignatureData, signatureUri, msgDataset, false);
    }

    /**
     * Signs all graphs with one signature.
     * 
     * @param msgDataset
     * @param sigStage
     * @param signer
     * @param privateKey
     * @param privateKeyUri
     * @param publicKey
     * @throws Exception
     */
    private static void signWholeMessage(final Dataset msgDataset, final SigningStage sigStage, final WonSigner signer,
                    final PrivateKey privateKey, final String privateKeyUri, final PublicKey publicKey, URI messageURI)
                    throws Exception {
        WonSignatureData wonSignatureData = null;
        // String outerEnvUri = null;
        String signatureUri = WonMessageUtils.stripFragment(messageURI).toString() + WonMessage.SIGNATURE_URI_SUFFIX;
        wonSignatureData = signer.signWholeDataset(privateKey, privateKeyUri, publicKey, signatureUri);
        Objects.requireNonNull(wonSignatureData);
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
     * @param graphIsEnvelope if true, a msg:containsSignature property is added to
     * the graph URI, otherwise a triple [msgUri] msg:signature [signatureUri] is
     * added to the graph.
     */
    private static void addSignature(final WonSignatureData sigData, final String graphUri, final Dataset msgDataset,
                    boolean graphIsEnvelope) {
        Model graph = msgDataset.getNamedModel(graphUri);
        Resource graphNode = graph.createResource(graphUri);
        Resource sigNode = graph.createResource(sigData.getSignatureUri());
        if (graphIsEnvelope) {
            // only connect envelope to signature. pure signature graphs are not connected
            // this way.
            graphNode.addProperty(WONMSG.containsSignature, sigNode);
        } else if (Objects.equals(graphNode.getURI(), sigNode.getURI())) {
            URI messageURI = WonMessageUtils.stripFragment(URI.create(graphUri));
            Resource msgNode = graph.getResource(messageURI.toString());
            graph.add(msgNode, WONMSG.signature, sigNode);
        }
        WonRdfUtils.SignatureUtils.addSignature(sigNode, sigData);
    }

    private static void calculateMessageUriForContent(Dataset msgDataset) throws Exception {
        WonHasher hasher = new WonHasher();
        String hashId = hasher.calculateHashIdForDataset(msgDataset);
        RdfUtils.renameResourceWithPrefix(msgDataset, WonMessageUriHelper.getSelfUri().toString(),
                        WonMessageUriHelper.createMessageURIForId(hashId).toString());
    }

    /**
     * If the provided signing stage has unsigned content graphs, sign them. This
     * adds the signature triples to the graph, add signature graphs to the dataset,
     * and add signature references of those signatures into the envelope graph that
     * has has content property referencing signed by that signature content graph
     * 
     * @param msgDataset
     * @param sigStage
     * @param signer
     * @param privateKey
     * @param privateKeyUri
     */
    private static void signContents(final Dataset msgDataset, final SigningStage sigStage, final WonSigner signer,
                    final PrivateKey privateKey, final String privateKeyUri, final PublicKey publicKey)
                    throws Exception {
        List<WonSignatureData> sigRefs = signer.sign(privateKey, privateKeyUri, publicKey,
                        sigStage.getUnsignedContentUris());
        for (WonSignatureData sigRef : sigRefs) {
            String envUri = sigStage.getEnvelopeUri();
            addSignature(sigRef, envUri, msgDataset, true);
        }
    }
}

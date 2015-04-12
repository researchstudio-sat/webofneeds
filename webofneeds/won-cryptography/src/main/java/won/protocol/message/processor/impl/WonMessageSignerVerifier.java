package won.protocol.message.processor.impl;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.AnonId;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import de.uni_koblenz.aggrimm.icp.crypto.sign.algorithm.algorithm.SignatureAlgorithmFisteus2010;
import won.cryptography.rdfsign.*;
import won.protocol.message.SignatureReference;
import won.protocol.message.WonMessage;
import won.protocol.vocabulary.WONMSG;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.List;
import java.util.Map;

/**
 * User: ypanchenko
 * Date: 08.04.2015
 */
public class WonMessageSignerVerifier
{

  public static WonMessage sign(PrivateKey privateKey, String privateKeyUri, WonMessage message) throws Exception {

    Dataset msgDataset = message.getCompleteDataset();
    SigningStage sigStage = new SigningStage(message);

    addUnreferencedSigReferences(msgDataset, sigStage);

    WonSigner signer = new WonSigner(msgDataset, new SignatureAlgorithmFisteus2010());
    signContents(msgDataset, sigStage, signer, privateKey, privateKeyUri);
    signEnvelopes(msgDataset, sigStage, signer, privateKey, privateKeyUri);

    return new WonMessage(msgDataset);
  }

  /**
   * If the provided signing stage has unsigned content graphs, sign them, add signature graphs
   * to the dataset, and add signature references of those signatures into the envelope graph
   * that has contains envelope property referencing signed by that signature envelope graph
   * @param msgDataset
   * @param sigStage
   * @param signer
   * @param privateKey
   * @param privateKeyUri
   */
  private static void signEnvelopes(final Dataset msgDataset, final SigningStage sigStage,
                                    final WonSigner signer, final PrivateKey privateKey,
                                    final String privateKeyUri) throws Exception {
    SignatureReference sigRef = null;
    for (String envUri : sigStage.getUnsignedEnvUrisOrderedByContainment()) {
      if (sigRef != null) {
        addSignatureReference(sigStage.getMessageUri(), sigRef, envUri, msgDataset);
      }
      sigRef = signer.sign(privateKey, privateKeyUri, envUri).get(0);
    }
  }

  private static void addSignatureReference(final String msgUri, final SignatureReference sigRef,
                                            final String envUri, final Dataset msgDataset) {

    Model envelopeGraph = msgDataset.getNamedModel(envUri);
    Resource messageEventResource = envelopeGraph.createResource(msgUri);
    Resource bnode = envelopeGraph.createResource(AnonId.create());
    messageEventResource.addProperty(
      WONMSG.REFERENCES_SIGNATURE_PROPERTY,
      bnode);
    bnode.addProperty(WONMSG.HAS_SIGNATURE_GRAPH_PROPERTY,
                      envelopeGraph.createResource(sigRef.getSignatureGraphUri()));
    bnode.addProperty(WONMSG.HAS_SIGNED_GRAPH_PROPERTY,
                      envelopeGraph.createResource(sigRef.getSignedGraphUri()));
    bnode.addProperty(WONMSG.HAS_SIGNATURE_VALUE_PROPERTY,
                      envelopeGraph.createLiteral(sigRef.getSignatureValue()));
  }

  /**
   * If the provided signing stage has unsigned content graphs, sign them, add signature graphs
   * to the dataset, and add signature references of those signatures into the envelope graph
   * that has has content property referencing signed by that signature content graph
   * @param msgDataset
   * @param sigStage
   * @param signer
   * @param privateKey
   * @param privateKeyUri
   */
  private static void signContents(final Dataset msgDataset, final SigningStage sigStage,
                                   final WonSigner signer, final PrivateKey privateKey,
                                   final String privateKeyUri) throws Exception {
    List<SignatureReference> sigRefs = signer.sign(privateKey, privateKeyUri, sigStage.getUnsignedContentUris());
    for (SignatureReference sigRef : sigRefs) {
      String envUri = sigStage.getEnvelopeUriContainingContent(sigRef.getSignedGraphUri());
      addSignatureReference(sigStage.getMessageUri(), sigRef, envUri, msgDataset);
    }
  }

  /**
   * If the provided signing stage has signature graphs that are not referenced from any envelope graphs, they
   * should be added to the innermost not-signed envelope graph of the dataset
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
    for (SignatureReference sigRef : sigStage.getNotReferencedSignaturesAsReferences()) {
      addSignatureReference(sigStage.getMessageUri(), sigRef, innemostUnsignedEnvUri, msgDataset);
    }
  }

  public static SignatureVerificationResult verify(Map<String,PublicKey> keys, WonMessage message) throws Exception {
    Dataset dataset = message.getCompleteDataset();
    WonVerifier verifier = new WonVerifier(dataset);
    verifier.verify(keys);
    return verifier.getVerificationResult();
  }

}

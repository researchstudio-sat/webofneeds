package won.cryptography.rdfsign;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.*;
import won.protocol.message.SignatureReference;
import won.protocol.message.WonMessage;
import won.protocol.util.RdfUtils;
import won.protocol.util.WonRdfUtils;
import won.protocol.vocabulary.RDFG;
import won.protocol.vocabulary.SFSIG;
import won.protocol.vocabulary.WONMSG;

import java.util.*;

/**
 * A helper class to represent the won message information such
 * as which content and envelope graphs are unsigned, signatures
 * unreferenced, envelopes hierarchical order, etc., necessary
 * for the signing component to make decisions on what parts of
 * message should be signed and referenced.
 *
 * User: ypanchenko
 * Date: 09.04.2015
 */

public class SigningStage {

  private Set<String> envUris = new HashSet<>();
  private Set<String> contentUris = new HashSet<>();
  private Map<String,String> contentUriToContainingItEnvUri = new HashMap<>();
  private Map<String,String> envUriToContainedInItEnvUri = new HashMap<>();
  private Map<String,String> graphUriToSigUri = new HashMap<>();
  private Map<String,String> sigUriToReferencingItEnvUri = new HashMap<>();
  private Map<String,SignatureReference> sigUriToSigReference = new HashMap<>();

  private List<String> envOrderedByContainment = new ArrayList<>();
  private String messageUri;
  private Map<String,String> graphUriToItsMessageUri = new HashMap<>();

  public SigningStage(WonMessage message) {
    extractData(message);
  }

  public String getMessageUri() {
    return messageUri;
  }

  public String getMessageUri(String envelopeGraphUri) {
    return graphUriToItsMessageUri.get(envelopeGraphUri);
  }

  private void extractData(final WonMessage message) {
    messageUri = message.getMessageURI().toString();
    Dataset dataset = message.getCompleteDataset();
    for (String uri : RdfUtils.getModelNames(dataset)) {
      Model model = dataset.getNamedModel(uri);
      if (message.isEnvelopeGraph(uri, model)) {
        extractEnvelopeData(uri, model, message);
      } else if (WonRdfUtils.SignatureUtils.isSignature(model)) {
        extractSignatureData(uri, model);
      } else { // should be content
        extractContentData(uri);
      }
    }
    //created ordered env list
    orderEnvelopes(message);
  }

  private void orderEnvelopes(final WonMessage message) {
    String outer = message.getOuterEnvelopeGraphURI().toString();
    envOrderedByContainment.add(outer);
    while (outer != null) {
      String inner = envUriToContainedInItEnvUri.get(outer);
      if (inner != null) {
        envOrderedByContainment.add(0, inner);
      }
      outer = inner;
    }
  }

  private void extractContentData(final String uri) {
    contentUris.add(uri);
  }

  private void extractSignatureData(final String uri, final Model model) {
    String signedGraphUri = null;
    String signatureValue = null;
    Resource resource = model.getResource(uri);
    NodeIterator ni = model.listObjectsOfProperty(resource, WONMSG.HAS_SIGNED_GRAPH_PROPERTY);
    if (ni.hasNext()) {
      signedGraphUri = ni.next().asResource().getURI();
    }
    NodeIterator ni2 = model.listObjectsOfProperty(resource, SFSIG.HAS_SIGNATURE_VALUE);
    if (ni2.hasNext()) {
      signatureValue = ni2.next().asLiteral().toString();
    }
    if (signedGraphUri != null && signatureValue != null) {
      graphUriToSigUri.put(signedGraphUri, uri);
      sigUriToSigReference.put(uri, new SignatureReference(signedGraphUri, uri, signatureValue));
    }
  }

  private void extractEnvelopeData(final String envelopeGraphUri, final Model envelopeGraph, final WonMessage message) {

    // add to envelope uris
    envUris.add(envelopeGraphUri);

    // find if it contains has_content

    //TODO this duplicates private findmessageuri method from wonmessage, refactor to avoid code repetition
    String envMessageUri = RdfUtils.findOnePropertyFromResource(envelopeGraph, envelopeGraph.getResource(envelopeGraphUri), RDFG.SUBGRAPH_OF).asResource().getURI();
    graphUriToItsMessageUri.put(envelopeGraphUri, envMessageUri);
    Resource msgEventResource = envelopeGraph.getResource(envMessageUri);
    Resource msgEnvelopeResource = envelopeGraph.getResource(envelopeGraphUri);
    StmtIterator it = msgEventResource.listProperties(WONMSG.HAS_CONTENT_PROPERTY);
    while (it.hasNext()) {
      contentUriToContainingItEnvUri.put(it.nextStatement().getObject().asResource().getURI(), envelopeGraphUri);
    }

    // find if it contains another envelopes
    it = msgEnvelopeResource.listProperties(WONMSG.CONTAINS_ENVELOPE);
    // TODO make sure that > 1 envelope is not possible in the protocol
    if (it.hasNext()) {
      envUriToContainedInItEnvUri.put(envelopeGraphUri, it.nextStatement().getObject().asResource().getURI());
    }

    // find if it contains signature references
    it = msgEventResource.listProperties(WONMSG.REFERENCES_SIGNATURE_PROPERTY);
    while (it.hasNext()) {
      Resource refObj = it.next().getObject().asResource();
      NodeIterator sigGraphIter = envelopeGraph.listObjectsOfProperty(refObj,
                                                                      WONMSG.HAS_SIGNATURE_GRAPH_PROPERTY);
      if (sigGraphIter.hasNext()) {
        sigUriToReferencingItEnvUri.put(sigGraphIter.next().asResource().getURI(), envelopeGraphUri);
      }
    }
  }

  public List<String> getUnsignedEnvUrisOrderedByContainment() {

    List<String> ordered = new ArrayList<>(envOrderedByContainment.size());
    for (String uri : envOrderedByContainment) {
      if (!graphUriToSigUri.containsKey(uri)) {
        ordered.add(uri);
      }
    }
    return ordered;
  }

  public Set<String> getUnsignedContentUris() {
    return getUnsignedUris(contentUris);
  }

  private Set<String> getUnsignedUris(final Set<String> fromUris) {
    Set<String> unsigned = new HashSet<>(fromUris.size());
    for (String uri : fromUris) {
      if (!graphUriToSigUri.containsKey(uri)) {
        unsigned.add(uri);
      }
    }
    return unsigned;
  }


  public String getEnvelopeUriContainingContent(String contentUri) {
    return contentUriToContainingItEnvUri.get(contentUri);
  }


  public List<SignatureReference> getNotReferencedSignaturesAsReferences() {
    List<SignatureReference> refs = new ArrayList<>();

    for (String sigUri : sigUriToSigReference.keySet()) {
      if (!sigUriToReferencingItEnvUri.containsKey(sigUri)) {
        refs.add(sigUriToSigReference.get(sigUri));
      }
    }
    return refs;
  }
}

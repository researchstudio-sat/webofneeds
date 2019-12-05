package won.cryptography.rdfsign;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.StmtIterator;

import won.protocol.message.WonMessage;
import won.protocol.message.WonSignatureData;
import won.protocol.util.RdfUtils;
import won.protocol.util.WonRdfUtils;
import won.protocol.vocabulary.WONMSG;

/**
 * A helper class to represent the won message information such as which content
 * and envelope graphs are unsigned, signatures unreferenced, envelopes
 * hierarchical order, etc., necessary for the signing component to make
 * decisions on what parts of message should be signed and referenced. User:
 * ypanchenko Date: 09.04.2015
 */
public class SigningStage {
    private String envUri = null;
    private final Set<String> contentUris = new HashSet<>();
    private final Map<String, String> contentUriToContainingItEnvUri = new HashMap<>();
    private final Map<String, String> envUriToContainedInItEnvUri = new HashMap<>();
    private final Map<String, String> graphUriToSigUri = new HashMap<>();
    private final Map<String, WonSignatureData> sigUriToSigReference = new HashMap<>();
    private final List<String> envOrderedByContainment = new ArrayList<>();
    private String messageUri;
    private final Map<String, String> graphUriToItsMessageUri = new HashMap<>();
    private String outermostSignatureUri = null;

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
        String envelopeURI = message.getEnvelopeURI().toString();
        Model envelope = dataset.getNamedModel(envelopeURI.toString());
        extractEnvelopeData(envelopeURI.toString(), envelope, message);
        for (String uri : RdfUtils.getModelNames(dataset)) {
            if (envelopeURI.equals(uri)) {
                continue; // we already processed this
            }
            // should be content
            extractContentData(uri);
        }
    }

    private void extractContentData(final String uri) {
        contentUris.add(uri);
    }

    private void extractSignatureData(final String sigGraphUri, final Model model) {
        WonSignatureData wonSignatureData = WonRdfUtils.SignatureUtils.extractWonSignatureData(sigGraphUri, model);
        if (wonSignatureData != null && wonSignatureData.getSignatureValue() != null) {
            wonSignatureData.getSignedGraphUris().forEach(signed -> graphUriToSigUri.put(signed, sigGraphUri));
            sigUriToSigReference.put(sigGraphUri, wonSignatureData);
        }
    }

    public String getEnvelopeUri() {
        Objects.requireNonNull(envUri);
        return envUri;
    }

    private void extractEnvelopeData(final String envelopeGraphUri, final Model envelopeGraph,
                    final WonMessage message) {
        this.envUri = envelopeGraphUri;
        String envMessageUri = message.getMessageURIRequired().toString();
        graphUriToItsMessageUri.put(envelopeGraphUri, envMessageUri);
        Resource msgEventResource = envelopeGraph.getResource(envMessageUri);
        Resource msgEnvelopeResource = envelopeGraph.getResource(envelopeGraphUri);
        StmtIterator it = msgEventResource.listProperties(WONMSG.content);
        while (it.hasNext()) {
            contentUriToContainingItEnvUri.put(it.nextStatement().getObject().asResource().getURI(), envelopeGraphUri);
        }
        // find if it contains a signature references
        it = msgEnvelopeResource.listProperties(WONMSG.containsSignature);
        while (it.hasNext()) {
            Resource refObj = it.next().getObject().asResource();
            extractSignatureData(refObj.getURI(), refObj.getModel());
        }
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

    public WonSignatureData getOutermostSignature() {
        return sigUriToSigReference.get(outermostSignatureUri);
    }
}

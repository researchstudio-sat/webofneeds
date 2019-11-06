package won.protocol.message;

import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.tdb.TDB;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

import won.protocol.exception.MissingMessagePropertyException;
import won.protocol.exception.WonMessageNotWellFormedException;
import won.protocol.util.RdfUtils;
import won.protocol.vocabulary.RDFG;
import won.protocol.vocabulary.SFSIG;
import won.protocol.vocabulary.WONMSG;

/**
 * Wraps an RDF dataset representing a WoN message.
 * <p>
 * Note: this implementation is not thread-safe.
 */
public class WonMessage implements Serializable {
    final Logger logger = LoggerFactory.getLogger(getClass());
    private Dataset messageContent;
    private Dataset completeDataset;
    // private Model messageMetadata;
    // private URI messageEventURI;
    private List<Model> envelopeGraphs;
    private List<String> envelopeGraphNames;
    private URI outerEnvelopeGraphURI;
    private Model outerEnvelopeGraph;
    private URI messageURI;
    private WonMessageType messageType; // ConnectMessage, CreateMessage, AtomStateMessage
    private WonMessageDirection envelopeType;
    private URI senderURI;
    private URI senderAtomURI;
    private URI senderNodeURI;
    private URI senderSocketURI;
    private URI recipientURI;
    private URI recipientAtomURI;
    private URI recipientNodeURI;
    private URI recipientSocketURI;
    private URI hintTargetAtomURI;
    private URI hintTargetSocketURI;
    private Double hintScore;
    private List<URI> previousMessages = null;
    private List<URI> injectIntoConnections = null;
    private URI isResponseToMessageURI;
    private URI isRemoteResponseToMessageURI;
    private List<String> contentGraphNames;
    private WonMessageType respondingToMessageType;
    private URI correspondingRemoteMessageURI;
    private List<URI> forwardedMessageURIs;
    private List<AttachmentHolder> attachmentHolders;
    private Map<String, Resource> graphSignatures;
    private Optional<WonMessage> deliveryChain = Optional.empty();
    // in the case of a single message, the headMessage is the instance. In case of
    // a multi-message (=delivery chain) the headMessage is the original message
    // (the others must be forwarded or responses).
    private WonMessage headMessage = this;
    private Map<URI, WonMessage> messages;
    private Optional<WonMessage> response = null;
    private Optional<WonMessage> remoteResponse = null;

    // private Resource msgBnode;
    // private Signature signature;
    public WonMessage(Dataset completeDataset) {
        requireOnlyOneMessage(completeDataset);
        this.completeDataset = completeDataset;
    }

    /**
     * Private constructor for use by static WonMessage.of(dataset) method.
     */
    private WonMessage() {
    }

    private void requireOnlyOneMessage(Dataset dataset) {
        Iterator<String> names = dataset.listNames();
        Optional<String> knownMessageUri = Optional.empty();
        while (names.hasNext()) {
            String name = names.next();
            String currentMessageUri = stripUriFragment(name);
            if (currentMessageUri == null || currentMessageUri.length() == 0) {
                throw new IllegalArgumentException("Found unacceptable graph name " + name);
            }
            if (knownMessageUri.isPresent()) {
                if (!knownMessageUri.get().equals(currentMessageUri)) {
                    throw new IllegalArgumentException("Dataset must contain only one message, but found graphs of "
                                    + knownMessageUri.get() + " and " + currentMessageUri
                                    + ". Use WonMessage.of(dataset) instead!");
                }
            } else {
                knownMessageUri = Optional.of(currentMessageUri);
            }
        }
    }

    /**
     * Create a WonMessage object from a dataset that may contain multiple messages.
     * 
     * @param dataset
     * @return
     */
    public static WonMessage of(Dataset dataset) {
        Objects.requireNonNull(dataset);
        Map<URI, WonMessage> messages = extractMessageMap(dataset);
        if (messages.size() == 0) {
            throw new IllegalArgumentException("No message found in dataset");
        }
        if (messages.size() == 1) {
            return messages.values().stream().findFirst().get();
        }
        WonMessage msg = new WonMessage();
        msg.messages = messages;
        msg.headMessage = messages.get(findHeadMessage(messages));
        msg.completeDataset = RdfUtils.cloneDataset(dataset);
        msg.messages.values().forEach(m -> m.deliveryChain = Optional.of(msg));
        return msg;
    }

    public static WonMessage of(Collection<WonMessage> messages) {
        return of(messages.toArray(new WonMessage[messages.size()]));
    }

    public static WonMessage of(WonMessage... messages) {
        Objects.requireNonNull(messages);
        if (messages.length == 0) {
            throw new IllegalArgumentException("No messages provided");
        }
        if (messages.length == 1) {
            return messages[0];
        }
        Map<URI, WonMessage> messageMap = extractMessageMap(messages);
        if (messageMap.size() == 0) {
            throw new IllegalArgumentException("No messages provided");
        }
        WonMessage msg = new WonMessage();
        msg.messages = messageMap;
        msg.headMessage = messageMap.get(findHeadMessage(messageMap));
        msg.completeDataset = combineDatasets(messages);
        msg.messages.values().forEach(m -> m.deliveryChain = Optional.of(msg));
        return msg;
    }

    private static URI findHeadMessage(Map<URI, WonMessage> messages) {
        if (messages.size() == 1) {
            return messages.keySet().stream().findFirst().get();
        }
        Set<URI> candidates = messages.keySet().stream().collect(Collectors.toSet());
        for (Entry<URI, WonMessage> entry : messages.entrySet()) {
            WonMessage msg = entry.getValue();
            URI msgUri = entry.getKey();
            if (msg.getMessageTypeRequired().isResponseMessage()) {
                URI respondingTo = msg.getRespondingToMessageURIRequired();
                if (candidates.contains(respondingTo)) {
                    candidates.remove(msgUri);
                }
            }
            candidates.removeAll(msg.getForwardedMessageURIs());
        }
        if (candidates.size() != 1)
            throw new WonMessageNotWellFormedException("message dataset must contain one head message");
        return candidates.stream().findFirst().get();
    }

    private static Map<URI, WonMessage> extractMessageMap(WonMessage... messages) {
        Map<URI, WonMessage> messageMap = new HashMap<>();
        for (int i = 0; i < messages.length; i++) {
            WonMessage message = messages[i];
            if (message.messages != null) {
                throw new IllegalArgumentException("No multi-messages allowed in WonMessage.of(WonMessage...)");
            }
            messageMap.put(message.getMessageURIRequired(), message);
        }
        return messageMap;
    }

    private static Dataset combineDatasets(WonMessage... messages) {
        Dataset ds = DatasetFactory.createGeneral();
        for (int i = 0; i < messages.length; i++) {
            ds = RdfUtils.addDatasetToDataset(ds, messages[i].getCompleteDataset());
        }
        return ds;
    }

    /**
     * Returns all messages contained in the dataset, each with a new copy of the
     * dataset containing only the data pertaining to the message.
     * 
     * @param dataset
     * @return
     */
    private static Map<URI, WonMessage> extractMessageMap(Dataset dataset) {
        Objects.requireNonNull(dataset);
        Map<URI, Dataset> perMessageDatasets = new HashMap<>();
        Iterator<String> names = dataset.listNames();
        while (names.hasNext()) {
            String name = names.next();
            URI msgUri = URI.create(stripUriFragment(name));
            Dataset ds = perMessageDatasets.get(msgUri);
            if (ds == null) {
                ds = DatasetFactory.createGeneral();
            }
            ds.addNamedModel(name, dataset.getNamedModel(name));
            perMessageDatasets.put(msgUri, ds);
        }
        return perMessageDatasets
                        .entrySet().stream()
                        .collect(Collectors.toMap(e -> e.getKey(), e -> new WonMessage(e.getValue())));
    }

    private static String stripUriFragment(String name) {
        String fragment = "#" + URI.create(name).getRawFragment();
        String msgUri = name.substring(0, name.length() - fragment.length());
        return msgUri;
    }

    public static WonMessage deepCopy(WonMessage original) {
        return WonMessage.of(RdfUtils.cloneDataset(original.completeDataset));
    }

    /**
     * Returns the complete dataset, even if this is a multi-message object.
     * 
     * @return
     */
    public synchronized Dataset getCompleteDataset() {
        return RdfUtils.cloneDataset(this.completeDataset);
    }

    /**
     * Return the message 'in focus'. Which one that is depends on the state of the
     * delivery chain in the dataset that this WonMessage object was created from.
     * Options are:
     * <ul>
     * <li>New message is being sent (=headMessage): no responses yet, the
     * headMessage is in focus.
     * <li>The sender's node has produced a response: the response is now 'in
     * focus'. The combined (head + response) are returned to the sender and
     * forwarded to the recipient.</li>
     * <li>The reipient's node has produced a response: two options:
     * <ul>
     * <li>The sender receives a message containing just the recipient node's
     * response (=remoteResponse). In that Message, this response is in focus.</li>
     * <li>The recipient gets a dataset containing the headMessage plus both
     * responses. The headMessage is in focus here.</li>
     * </ul>
     * </ul>
     *
     * @return
     */
    public WonMessage getFocalMessage() {
        if (this.isMessageWithResponse()) {
            return getResponse().get();
        }
        return headMessage;
    }

    /**
     * Returns all messages that are present in the dataset that was used to create
     * this WonMessage object.
     * 
     * @return
     */
    public Set<WonMessage> getAllMessages() {
        if (this.messages != null) {
            return messages.values().stream().collect(Collectors.toSet());
        }
        Set<WonMessage> ret = new HashSet<WonMessage>();
        ret.add(this);
        return ret;
    }

    /**
     * Returns all messages forwarded by this message and contained in the complete
     * dataset.
     * 
     * @return
     */
    public Set<WonMessage> getForwardedMessages() {
        if (this.messages == null) {
            return Collections.EMPTY_SET;
        }
        return this.messages.entrySet().stream()
                        .filter(e -> headMessage.getForwardedMessageURIs().contains(e.getKey()))
                        .map(e -> e.getValue())
                        .collect(Collectors.toSet());
    }

    public Optional<WonMessage> getHeadMessage() {
        if (this.messages == null) {
            return Optional.empty();
        }
        return Optional.of(headMessage);
    }

    /**
     * Returns the response to this message, if it is contained in the complete
     * dataset.
     * 
     * @return
     */
    public Optional<WonMessage> getResponse() {
        if (this.messages == null) {
            return Optional.empty();
        }
        if (this.response != null) {
            return this.response;
        }
        this.response = this.messages.values().stream()
                        .filter(m -> m.getMessageTypeRequired().isResponseMessage()
                                        && Objects.equals(m.getRespondingToMessageURI(), headMessage.getMessageURI())
                                        && Objects.equals(WonMessageUtils.getSenderAtomURI(m),
                                                        WonMessageUtils.getSenderAtomURI(headMessage)))
                        .findFirst();
        return this.response;
    }

    /**
     * Returns the remote response to this message, if it is contained in the
     * complete dataset.
     * 
     * @return
     */
    public Optional<WonMessage> getRemoteResponse() {
        if (this.messages == null) {
            return Optional.empty();
        }
        if (!this.headMessage.getMessageTypeRequired().causesOutgoingMessage()) {
            return Optional.empty();
        }
        if (this.remoteResponse != null) {
            return this.remoteResponse;
        }
        this.remoteResponse = this.messages.values().stream()
                        .filter(m -> m.getMessageTypeRequired().isResponseMessage()
                                        && Objects.equals(m.getRespondingToMessageURI(), headMessage.getMessageURI())
                                        && Objects.equals(WonMessageUtils.getSenderAtomURI(m),
                                                        WonMessageUtils.getRecipientAtomURI(headMessage)))
                        .findFirst();
        return this.remoteResponse;
    }

    public boolean isPartOfDeliveryChain() {
        return this.deliveryChain.isPresent();
    }

    public Optional<WonMessage> getDeliveryChain() {
        return this.deliveryChain;
    }

    /**
     * Adds a property to the message resource in the head message.
     *
     * @param property
     * @param value
     */
    public synchronized void addMessageProperty(Property property, RDFNode value) {
        if (logger.isDebugEnabled()) {
            logger.debug("adding property {}, value {}, to message {} in envelope {}",
                            new Object[] { property, value, getMessageURI(), getOuterEnvelopeGraphURI() });
        }
        getOuterEnvelopeGraph().getResource(getMessageURI().toString()).addProperty(property, value);
    }

    /**
     * Adds a property to the message resource in the outermost envelope.
     *
     * @param property
     * @param uri the object of the property, assumed to be an uri
     */
    public synchronized void addMessageProperty(Property property, String uri) {
        RDFNode valueAsRdfNode = getOuterEnvelopeGraph().createResource(uri);
        addMessageProperty(property, valueAsRdfNode);
    }

    /**
     * Adds a property to the message resource in the outermost envelope.
     *
     * @param property
     * @param value
     */
    public synchronized void addMessageProperty(Property property, URI value) {
        addMessageProperty(property, value.toString());
    }

    /**
     * Adds a property to the message resource in the outermost envelope.
     *
     * @param property
     * @param value
     */
    public synchronized void addMessageProperty(Property property, long value) {
        addMessageProperty(property, getOuterEnvelopeGraph().createTypedLiteral(value));
    }

    /**
     * Adds a property to the message resource in the outermost envelope.
     *
     * @param property
     * @param value
     */
    public synchronized void addMessageProperty(Property property, int value) {
        addMessageProperty(property, getOuterEnvelopeGraph().createTypedLiteral(value));
    }

    /**
     * Adds a property to the message resource in the outermost envelope.
     *
     * @param property
     * @param value
     */
    public synchronized void addMessageProperty(Property property, double value) {
        addMessageProperty(property, getOuterEnvelopeGraph().createTypedLiteral(value));
    }

    /**
     * Adds a property to the message resource in the outermost envelope.
     *
     * @param property
     * @param value
     */
    public synchronized void addMessageProperty(Property property, float value) {
        addMessageProperty(property, getOuterEnvelopeGraph().createTypedLiteral(value));
    }

    /**
     * Adds a property to the message resource in the outermost envelope.
     *
     * @param property
     * @param value
     */
    public synchronized void addMessageProperty(Property property, boolean value) {
        addMessageProperty(property, getOuterEnvelopeGraph().createTypedLiteral(value));
    }

    /**
     * Creates a new dataset containing only the content graph(s) of the message and
     * their signature (if present). Each signature is put in a separate graph whose
     * name is the URI of the signature.
     *
     * @return
     */
    public synchronized Dataset getMessageContent() {
        if (headMessage.messageContent != null) {
            return RdfUtils.cloneDataset(headMessage.messageContent);
        } else {
            Dataset newMsgContent = DatasetFactory.createGeneral();
            // Iterator<String> modelNames = headMessage.completeDataset.listNames();
            // List<String> envelopeGraphNames = getEnvelopeGraphURIs();
            List<String> contentGraphs = getContentGraphURIs();
            // add all models that are not envelope graphs or signature graphs to the
            // messageContent
            for (String modelName : contentGraphs) {
                newMsgContent.addNamedModel(modelName, headMessage.completeDataset.getNamedModel(modelName));
                if (headMessage.graphSignatures.containsKey(modelName)) {
                    Resource sig = headMessage.graphSignatures.get(modelName);
                    Model sigModel = ModelFactory.createDefaultModel();
                    sigModel.add(sig.listProperties());
                    newMsgContent.addNamedModel(sig.getURI(), sigModel);
                }
            }
            headMessage.messageContent = newMsgContent;
        }
        return RdfUtils.cloneDataset(headMessage.messageContent);
    }

    /**
     * Returns all content graphs that are attachments, including their signature
     * graphs.
     *
     * @return
     */
    public synchronized List<AttachmentHolder> getAttachments() {
        if (headMessage.attachmentHolders != null) {
            return headMessage.attachmentHolders;
        }
        final List<String> envelopeGraphUris = getEnvelopeGraphURIs();
        List<AttachmentHolder> newAttachmentHolders = new ArrayList<>();
        String queryString = "prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#>\n"
                        + "prefix xsd:   <http://www.w3.org/2001/XMLSchema#>\n"
                        + "prefix rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                        + "prefix won:   <https://w3id.org/won/core#>\n"
                        + "prefix msg:   <https://w3id.org/won/message#>\n"
                        + "prefix sig:   <http://icp.it-risk.iwvi.uni-koblenz.de/ontologies/signature.owl#>\n" + "\n"
                        + "select ?attachmentSigGraphUri ?attachmentGraphUri ?envelopeGraphUri ?attachmentDestinationUri where { \n"
                        + "graph ?attachmentSigGraphUri {?attachmentSigGraphUri " + "              a sig:Signature; \n"
                        + "              msg:signedGraph ?attachmentGraphUri.\n" + "}\n"
                        + "graph ?envelopeGraphUri {?envelopeGraphUri rdf:type msg:EnvelopeGraph.  \n"
                        + "    ?messageUri msg:hasAttachment ?attachmentData. \n"
                        + "?attachmentData msg:hasDestinationUri ?attachmentDestinationUri; \n"
                        + "                msg:hasAttachmentGraphUri ?attachmentGraphUri.\n" + "}\n" + "}";
        Query query = QueryFactory.create(queryString);
        QuerySolutionMap initialBinding = new QuerySolutionMap();
        initialBinding.add("messageUri", new ResourceImpl(getMessageURI().toString()));
        try (QueryExecution queryExecution = QueryExecutionFactory.create(query, completeDataset)) {
            queryExecution.getContext().set(TDB.symUnionDefaultGraph, true);
            ResultSet result = queryExecution.execSelect();
            while (result.hasNext()) {
                QuerySolution solution = result.nextSolution();
                String envelopeGraphUri = solution.getResource("envelopeGraphUri").getURI();
                if (!envelopeGraphUris.contains(envelopeGraphUri)) {
                    logger.warn("found resource {} of type msg:EnvelopeGraph that is not the URI of an envelope graph in message {}",
                                    envelopeGraphUri, headMessage.messageURI);
                    continue;
                }
                // String sigGraphUri = solution.getResource("attachmentSigGraphUri").getURI();
                String attachmentGraphUri = solution.getResource("attachmentGraphUri").getURI();
                String attachmentSigGraphUri = solution.getResource("attachmentSigGraphUri").getURI();
                String attachmentDestinationUri = solution.getResource("attachmentDestinationUri").getURI();
                Dataset attachmentDataset = DatasetFactory.createGeneral();
                attachmentDataset.addNamedModel(attachmentGraphUri,
                                headMessage.completeDataset.getNamedModel(attachmentGraphUri));
                attachmentDataset.addNamedModel(attachmentSigGraphUri,
                                headMessage.completeDataset.getNamedModel(attachmentSigGraphUri));
                AttachmentHolder attachmentHolder = new AttachmentHolder(URI.create(attachmentDestinationUri),
                                attachmentDataset);
                newAttachmentHolders.add(attachmentHolder);
            }
        }
        headMessage.attachmentHolders = newAttachmentHolders;
        return newAttachmentHolders;
    }

    private synchronized Model getOuterEnvelopeGraph() {
        if (headMessage.outerEnvelopeGraph != null) {
            return headMessage.outerEnvelopeGraph;
        }
        headMessage.outerEnvelopeGraph = headMessage.completeDataset
                        .getNamedModel(getOuterEnvelopeGraphURI().toString());
        return headMessage.outerEnvelopeGraph;
    }

    public synchronized URI getOuterEnvelopeGraphURI() {
        if (headMessage.outerEnvelopeGraphURI != null) {
            return headMessage.outerEnvelopeGraphURI;
        }
        getEnvelopeGraphs(); // also sets the outerEnvelopeUri
        return headMessage.outerEnvelopeGraphURI;
    }

    /**
     * Returns all envelope graphs found in the message.
     * <p>
     * Not that this method has side effects: all intermediate results are cached
     * for re-use. This concerns the envelopeGraphNames, contentGraphNames and
     * messageURI members.
     *
     * @return
     */
    public synchronized List<Model> getEnvelopeGraphs() {
        // return cached instance if we have it
        if (headMessage.envelopeGraphs != null)
            return headMessage.envelopeGraphs;
        // initialize
        List<Model> allEnvelopes = new ArrayList<>();
        headMessage.envelopeGraphNames = new ArrayList<>();
        headMessage.contentGraphNames = new ArrayList<>();
        headMessage.graphSignatures = new HashMap<>();
        headMessage.outerEnvelopeGraph = null;
        Set<String> envelopesContainedInOthers = new HashSet<>();
        Set<String> allEnvelopeGraphNames = new HashSet<>();
        // iterate over named graphs
        Iterator<String> modelUriIterator = headMessage.completeDataset.listNames();
        while (modelUriIterator.hasNext()) {
            String currentGraphUri = modelUriIterator.next();
            Model currentGraph = headMessage.completeDataset.getNamedModel(currentGraphUri);
            // check if the current named graph is an envelope graph (G rdf:type
            // wonmsg:EnvelopeGraph)
            if (isEnvelopeGraph(currentGraphUri, currentGraph)) {
                headMessage.envelopeGraphNames.add(currentGraphUri);
                allEnvelopeGraphNames.add(currentGraphUri);
                allEnvelopes.add(currentGraph);
                URI currentMessageURI = findMessageUri(currentGraph, currentGraphUri);
                if (currentMessageURI != null) {
                    for (NodeIterator it = getContentGraphReferences(currentGraph,
                                    currentGraph.getResource(currentMessageURI.toString())); it.hasNext();) {
                        RDFNode node = it.next();
                        headMessage.contentGraphNames.add(node.asResource().toString());
                    }
                }
            }
            // check if the graph contains a signature and if so, remember it
            ResIterator it = currentGraph.listSubjectsWithProperty(RDF.type, SFSIG.SIGNATURE);
            while (it.hasNext()) {
                Resource sig = it.next();
                Resource signedGraph = sig.getPropertyResourceValue(WONMSG.signedGraph);
                headMessage.graphSignatures.put(signedGraph.getURI(), sig);
            }
        }
        Set<String> candidatesForOuterEnvelope = Sets.symmetricDifference(allEnvelopeGraphNames,
                        envelopesContainedInOthers);
        // we've now visited all named graphs. We should now have exactly one candidate
        // for the outer envelope
        if (candidatesForOuterEnvelope.size() != 1) {
            throw new WonMessageNotWellFormedException(String
                            .format("Message dataset must contain exactly one envelope graph, but found %d",
                                            candidatesForOuterEnvelope.size()));
        }
        String outerEnvelopeUri = candidatesForOuterEnvelope.iterator().next();
        headMessage.outerEnvelopeGraphURI = URI.create(outerEnvelopeUri);
        headMessage.outerEnvelopeGraph = headMessage.completeDataset.getNamedModel(outerEnvelopeUri);
        headMessage.envelopeGraphs = allEnvelopes;
        return Collections.unmodifiableList(
                        allEnvelopes.stream().map(RdfUtils::cloneModel).collect(Collectors.toList()));
    }

    private synchronized URI findMessageUri(final Model model, final String modelUri) {
        RDFNode messageUriNode = RdfUtils.findOnePropertyFromResource(model, model.getResource(modelUri),
                        RDFG.SUBGRAPH_OF);
        return URI.create(messageUriNode.asResource().getURI());
    }

    private synchronized List<String> findContainedEnvelopeUris(final Model envelopeGraph,
                    final String envelopeGraphUri) {
        Resource envelopeGraphResource = envelopeGraph.getResource(envelopeGraphUri);
        StmtIterator it = envelopeGraphResource.listProperties(WONMSG.containsEnvelope);
        if (it.hasNext()) {
            List ret = new ArrayList<String>();
            while (it.hasNext()) {
                ret.add(it.nextStatement().getObject().asResource().getURI());
            }
            return ret;
        }
        return Collections.emptyList();
    }

    public boolean isEnvelopeGraph(final String modelUri, final Model model) {
        return model.contains(model.getResource(modelUri), RDF.type, WONMSG.EnvelopeGraph);
    }

    public synchronized List<String> getEnvelopeGraphURIs() {
        if (headMessage.envelopeGraphs == null) {
            getEnvelopeGraphs(); // also sets envelopeGraphNames
        }
        return Collections.unmodifiableList(headMessage.envelopeGraphNames);
    }

    public synchronized List<String> getContentGraphURIs() {
        // since there may not be any content graphs, we can't check
        // if headMessage.contentGraphNames == null. We instead have to check
        // if we ran the detection of the envelope graphs at least once.
        if (headMessage.envelopeGraphs == null) {
            getEnvelopeGraphs(); // also sets envelopeGraphNames
        }
        return Collections.unmodifiableList(headMessage.contentGraphNames);
    }

    private synchronized NodeIterator getContentGraphReferences(Model model, Resource envelopeGraphResource) {
        return model.listObjectsOfProperty(envelopeGraphResource, WONMSG.content);
    }

    public synchronized URI getMessageURI() {
        if (headMessage.messageURI == null) {
            headMessage.messageURI = findMessageUri(getOuterEnvelopeGraph(), getOuterEnvelopeGraphURI().toString());
        }
        return headMessage.messageURI;
    }

    public synchronized URI getMessageURIRequired() {
        URI ret = getMessageURI();
        if (ret == null) {
            throw new IllegalStateException("Could not determine message URI");
        }
        return ret;
    }

    public synchronized WonMessageType getMessageType() {
        if (headMessage.messageType == null) {
            URI type = getEnvelopePropertyURIValue(WONMSG.messageType);
            headMessage.messageType = WonMessageType.getWonMessageType(type);
        }
        return headMessage.messageType;
    }

    public synchronized WonMessageType getMessageTypeRequired() {
        WonMessageType ret = getMessageType();
        if (ret == null) {
            throw new MissingMessagePropertyException(WONMSG.messageType);
        }
        return ret;
    }

    public synchronized WonMessageDirection getEnvelopeType() {
        if (headMessage.envelopeType == null) {
            URI type = getEnvelopePropertyURIValue(RDF.type);
            if (type != null) {
                headMessage.envelopeType = WonMessageDirection.getWonMessageDirection(type);
            }
        }
        return headMessage.envelopeType;
    }

    public synchronized WonMessageDirection getEnvelopeTypeRequired() {
        WonMessageDirection ret = getEnvelopeType();
        if (ret == null) {
            throw new MissingMessagePropertyException(RDF.type);
        }
        return ret;
    }

    public synchronized URI getSenderURI() {
        if (headMessage.senderURI == null) {
            headMessage.senderURI = getEnvelopePropertyURIValue(WONMSG.sender);
        }
        return headMessage.senderURI;
    }

    public synchronized URI getSenderURIRequired() {
        URI ret = getSenderURI();
        if (ret == null) {
            throw new MissingMessagePropertyException(WONMSG.sender);
        }
        return ret;
    }

    public synchronized URI getSenderAtomURI() {
        if (headMessage.senderAtomURI == null) {
            headMessage.senderAtomURI = getEnvelopePropertyURIValue(WONMSG.senderAtom);
            if (headMessage.senderAtomURI == null) {
                URI socketURI = headMessage.getSenderSocketURI();
                if (socketURI != null) {
                    headMessage.senderAtomURI = WonMessageUtils.stripFragment(socketURI);
                }
            }
        }
        return headMessage.senderAtomURI;
    }

    public synchronized URI getSenderAtomURIRequired() {
        URI ret = getSenderAtomURI();
        if (ret == null) {
            throw new MissingMessagePropertyException(WONMSG.senderAtom);
        }
        return ret;
    }

    public synchronized URI getSenderNodeURI() {
        if (headMessage.senderNodeURI == null) {
            headMessage.senderNodeURI = getEnvelopePropertyURIValue(WONMSG.senderNode);
            if (headMessage.senderNodeURI == null) {
                URI atomURI = headMessage.getSenderAtomURI();
                if (atomURI != null) {
                    headMessage.senderNodeURI = WonMessageUtils.stripAtomSuffix(atomURI);
                }
            }
        }
        return headMessage.senderNodeURI;
    }

    public synchronized URI getSenderNodeURIRequired() {
        URI ret = getSenderNodeURI();
        if (ret == null) {
            throw new MissingMessagePropertyException(WONMSG.senderNode);
        }
        return ret;
    }

    public synchronized URI getSenderSocketURI() {
        if (headMessage.senderSocketURI == null) {
            headMessage.senderSocketURI = getEnvelopePropertyURIValue(WONMSG.senderSocket);
        }
        return headMessage.senderSocketURI;
    }

    public synchronized URI getSenderSocketURIRequired() {
        URI ret = getSenderSocketURI();
        if (ret == null) {
            throw new MissingMessagePropertyException(WONMSG.senderSocket);
        }
        return ret;
    }

    public synchronized URI getRecipientURI() {
        if (headMessage.recipientURI == null) {
            headMessage.recipientURI = getEnvelopePropertyURIValue(WONMSG.recipient);
        }
        return headMessage.recipientURI;
    }

    public synchronized URI getRecipientURIRequired() {
        URI ret = getRecipientURI();
        if (ret == null) {
            throw new MissingMessagePropertyException(WONMSG.recipient);
        }
        return ret;
    }

    public synchronized URI getRecipientAtomURI() {
        if (headMessage.recipientAtomURI == null) {
            headMessage.recipientAtomURI = getEnvelopePropertyURIValue(WONMSG.recipientAtom);
        }
        if (headMessage.recipientAtomURI == null) {
            URI socketURI = headMessage.getRecipientSocketURI();
            if (socketURI != null) {
                headMessage.recipientAtomURI = WonMessageUtils.stripFragment(socketURI);
            }
        }
        return headMessage.recipientAtomURI;
    }

    public synchronized URI getRecipientAtomURIRequired() {
        URI ret = getRecipientAtomURI();
        if (ret == null) {
            throw new MissingMessagePropertyException(WONMSG.recipientAtom);
        }
        return ret;
    }

    public synchronized URI getRecipientNodeURI() {
        if (headMessage.recipientNodeURI == null) {
            headMessage.recipientNodeURI = getEnvelopePropertyURIValue(WONMSG.recipientNode);
            if (headMessage.recipientNodeURI == null) {
                URI atomURI = headMessage.getRecipientAtomURI();
                if (atomURI != null) {
                    headMessage.recipientNodeURI = WonMessageUtils.stripAtomSuffix(atomURI);
                }
            }
        }
        return headMessage.recipientNodeURI;
    }

    public synchronized URI getRecipientNodeURIRequired() {
        URI ret = getRecipientNodeURI();
        if (ret == null) {
            throw new MissingMessagePropertyException(WONMSG.recipientNode);
        }
        return ret;
    }

    public synchronized URI getRecipientSocketURI() {
        if (headMessage.recipientSocketURI == null) {
            headMessage.recipientSocketURI = getEnvelopePropertyURIValue(WONMSG.recipientSocket);
        }
        return headMessage.recipientSocketURI;
    }

    public synchronized URI getRecipientSocketURIRequired() {
        URI ret = getRecipientSocketURI();
        if (ret == null) {
            throw new MissingMessagePropertyException(WONMSG.recipientSocket);
        }
        return ret;
    }

    public synchronized URI getHintTargetSocketURI() {
        if (headMessage.hintTargetSocketURI == null) {
            headMessage.hintTargetSocketURI = getEnvelopePropertyURIValue(WONMSG.hintTargetSocket);
        }
        return headMessage.hintTargetSocketURI;
    }

    public synchronized URI getHintTargetSocketURIRequired() {
        URI ret = getHintTargetSocketURI();
        if (ret == null) {
            throw new MissingMessagePropertyException(WONMSG.hintTargetSocket);
        }
        return ret;
    }

    public synchronized URI getHintTargetAtomURI() {
        if (headMessage.hintTargetAtomURI == null) {
            headMessage.hintTargetAtomURI = getEnvelopePropertyURIValue(WONMSG.hintTargetAtom);
        }
        return headMessage.hintTargetAtomURI;
    }

    public synchronized URI getHintTargetAtomURIRequired() {
        URI ret = getHintTargetAtomURI();
        if (ret == null) {
            throw new MissingMessagePropertyException(WONMSG.hintTargetAtom);
        }
        return ret;
    }

    public synchronized Double getHintScore() {
        if (headMessage.hintScore == null) {
            headMessage.hintScore = getEnvelopePropertyValue(WONMSG.hintScore,
                            x -> x.isLiteral() ? x.asLiteral().getDouble() : null);
        }
        return headMessage.hintScore;
    }

    public synchronized Double getHintScoreRequired() {
        Double ret = getHintScore();
        if (ret == null) {
            throw new MissingMessagePropertyException(WONMSG.hintScore);
        }
        return ret;
    }

    public synchronized List<URI> getInjectIntoConnectionURIs() {
        if (headMessage.injectIntoConnections == null) {
            headMessage.injectIntoConnections = getEnvelopePropertyURIValues(WONMSG.injectIntoConnection);
        }
        return headMessage.injectIntoConnections;
    }

    public synchronized List<URI> getInjectIntoConnectionURIsRequired() {
        List<URI> ret = getInjectIntoConnectionURIs();
        if (ret == null) {
            throw new MissingMessagePropertyException(WONMSG.injectIntoConnection);
        }
        return ret;
    }

    public synchronized List<URI> getPreviousMessageURIs() {
        if (headMessage.previousMessages == null) {
            headMessage.previousMessages = getEnvelopePropertyURIValues(WONMSG.previousMessage);
        }
        return headMessage.previousMessages;
    }

    public synchronized List<URI> getPreviousMessageURIsRequired() {
        List<URI> ret = getPreviousMessageURIs();
        if (ret == null) {
            throw new MissingMessagePropertyException(WONMSG.previousMessage);
        }
        return ret;
    }

    public synchronized URI getRespondingToMessageURI() {
        if (headMessage.isResponseToMessageURI == null) {
            headMessage.isResponseToMessageURI = getEnvelopePropertyURIValue(WONMSG.respondingTo);
        }
        return headMessage.isResponseToMessageURI;
    }

    public synchronized URI getRespondingToMessageURIRequired() {
        URI ret = getRespondingToMessageURI();
        if (ret == null) {
            throw new MissingMessagePropertyException(WONMSG.respondingTo);
        }
        return ret;
    }

    public synchronized List<URI> getForwardedMessageURIs() {
        if (headMessage.forwardedMessageURIs == null) {
            headMessage.forwardedMessageURIs = getEnvelopePropertyURIValues(WONMSG.forwardedMessage);
        }
        return headMessage.forwardedMessageURIs;
    }

    public synchronized List<URI> getForwardedMessageURIRequired() {
        List<URI> ret = getForwardedMessageURIs();
        if (ret == null) {
            throw new MissingMessagePropertyException(WONMSG.forwardedMessage);
        }
        return ret;
    }

    public synchronized WonMessageType getRespondingToMessageType() {
        if (headMessage.respondingToMessageType == null) {
            URI typeURI = getEnvelopePropertyURIValue(WONMSG.respondingToMessageType);
            if (typeURI != null) {
                headMessage.respondingToMessageType = WonMessageType.getWonMessageType(typeURI);
            }
        }
        return headMessage.respondingToMessageType;
    }

    public synchronized WonMessageType getRespondingToMessageTypeRequired() {
        WonMessageType ret = getRespondingToMessageType();
        if (ret == null) {
            throw new MissingMessagePropertyException(WONMSG.respondingToMessageType);
        }
        return ret;
    }

    public synchronized URI getEnvelopePropertyURIValue(URI propertyURI) {
        Property property = headMessage.completeDataset.getDefaultModel().createProperty(propertyURI.toString());
        return getEnvelopePropertyURIValue(property);
    }

    public synchronized URI getEnvelopePropertyURIValue(Property property) {
        Model currentEnvelope = getOuterEnvelopeGraph();
        URI currentEnvelopeUri = getOuterEnvelopeGraphURI();
        // TODO would make sense to order envelope graphs in order from container to
        // containee in the first place,
        // if proper done, we should avoid ending up in infinite loop if someone sends
        // us malformed envelopes that
        // contain-in-other circular...
        while (currentEnvelope != null) {
            URI currentMessageURI = findMessageUri(currentEnvelope, currentEnvelopeUri.toString());
            StmtIterator it = currentEnvelope.listStatements(currentEnvelope.getResource(currentMessageURI.toString()),
                            property, (RDFNode) null);
            if (it.hasNext()) {
                return URI.create(it.nextStatement().getObject().asResource().toString());
            }
            // move to the next envelope
            currentEnvelopeUri = RdfUtils.findFirstObjectUri(currentEnvelope, WONMSG.containsEnvelope, null, true,
                            true);
            currentEnvelope = null;
            if (currentEnvelopeUri != null) {
                currentEnvelope = headMessage.completeDataset.getNamedModel(currentEnvelopeUri.toString());
            }
        }
        return null;
    }

    public synchronized <T> T getEnvelopePropertyValue(Property property, Function<RDFNode, T> mapper) {
        Model currentEnvelope = getOuterEnvelopeGraph();
        URI currentEnvelopeUri = getOuterEnvelopeGraphURI();
        // TODO would make sense to order envelope graphs in order from container to
        // containee in the first place,
        // if proper done, we should avoid ending up in infinite loop if someone sends
        // us malformed envelopes that
        // contain-in-other circular...
        while (currentEnvelope != null) {
            URI currentMessageURI = findMessageUri(currentEnvelope, currentEnvelopeUri.toString());
            StmtIterator it = currentEnvelope.listStatements(currentEnvelope.getResource(currentMessageURI.toString()),
                            property, (RDFNode) null);
            if (it.hasNext()) {
                return mapper.apply(it.nextStatement().getObject());
            }
            // move to the next envelope
            currentEnvelopeUri = RdfUtils.findFirstObjectUri(currentEnvelope, WONMSG.containsEnvelope, null, true,
                            true);
            currentEnvelope = null;
            if (currentEnvelopeUri != null) {
                currentEnvelope = headMessage.completeDataset.getNamedModel(currentEnvelopeUri.toString());
            }
        }
        return null;
    }

    private synchronized List<URI> getEnvelopePropertyURIValues(Property property) {
        List<URI> values = new ArrayList<>();
        Model currentEnvelope = getOuterEnvelopeGraph();
        URI currentEnvelopeUri = getOuterEnvelopeGraphURI();
        // TODO would make sense to order envelope graphs in order from container to
        // containee in the first place
        while (currentEnvelope != null) {
            URI currentMessageURI = findMessageUri(currentEnvelope, currentEnvelopeUri.toString());
            StmtIterator it = currentEnvelope.listStatements(currentEnvelope.getResource(currentMessageURI.toString()),
                            property, (RDFNode) null);
            while (it.hasNext()) {
                values.add(URI.create(it.nextStatement().getObject().asResource().toString()));
            }
            currentEnvelopeUri = RdfUtils.findFirstObjectUri(currentEnvelope, WONMSG.containsEnvelope, null, true,
                            true);
            currentEnvelope = null;
            if (currentEnvelopeUri != null) {
                currentEnvelope = headMessage.completeDataset.getNamedModel(currentEnvelopeUri.toString());
            }
        }
        return values;
    }

    /**
     * Generate a String representaiton of the message for use in log statements.
     */
    public String toStringForDebug(boolean multiline) {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getClass().getSimpleName());
        sb.append("[");
        WonMessageType type = getMessageType();
        if (type.isResponseMessage()) {
            ToStringForDebugUtils.formatFields(
                            new String[] {
                                            "messageType",
                                            "direction",
                                            "messageUri",
                                            "respondingTo",
                                            "respondingToMessageType",
                                            "sender",
                                            "senderSocket",
                                            "senderAtom",
                                            "senderNode",
                                            "recipient",
                                            "recipientSocket",
                                            "recipientAtom",
                                            "recipientNode" },
                            new Object[] {
                                            getMessageType(),
                                            getEnvelopeType(),
                                            getMessageURI(),
                                            getRespondingToMessageURI(),
                                            getRespondingToMessageType(),
                                            getSenderURI(),
                                            getSenderSocketURI(),
                                            getSenderAtomURI(),
                                            getSenderNodeURI(),
                                            getRecipientURI(),
                                            getRecipientSocketURI(),
                                            getRecipientAtomURI(),
                                            getRecipientNodeURI() },
                            multiline, sb);
        } else {
            ToStringForDebugUtils.formatFields(
                            new String[] {
                                            "messageType",
                                            "direction",
                                            "messageUri",
                                            "sender",
                                            "senderSocket",
                                            "senderAtom",
                                            "senderNode",
                                            "recipient",
                                            "recipientSocket",
                                            "recipientAtom",
                                            "recipientNode" },
                            new Object[] {
                                            getMessageType(),
                                            getEnvelopeType(),
                                            getMessageURI(),
                                            getSenderURI(),
                                            getSenderSocketURI(),
                                            getSenderAtomURI(),
                                            getSenderNodeURI(),
                                            getRecipientURI(),
                                            getRecipientSocketURI(),
                                            getRecipientAtomURI(),
                                            getRecipientNodeURI() },
                            multiline, sb);
        }
        if (multiline) {
            sb.append("\n");
        }
        sb.append("]");
        return sb.toString();
    }

    private static class ToStringForDebugUtils {
        private static String indent = "    ";
        private static int INDENT_LENGTH = indent.length();

        private static void formatFields(String[] names, Object[] values, boolean multiline, StringBuilder sb) {
            if (names.length != values.length) {
                sb.append("error in toStringForDebug: names[] and values[] differ in length");
                return;
            }
            for (int i = 0; i < names.length; i++) {
                if (multiline) {
                    sb
                                    .append("\n")
                                    .append(indent)
                                    .append(names[i])
                                    .append(": ")
                                    .append(values[i]);
                } else {
                    sb
                                    .append(names[i])
                                    .append("=")
                                    .append(values[i])
                                    .append(", ");
                }
            }
            if (names.length > 0) {
                int length = sb.length();
                if (!multiline) {
                    sb.delete(length - 2, length);
                }
            }
        }
    }

    // Used to remember attachment graph uri and destination uri during the process
    // of extracting attachments.
    public class AttachmentMetaData {
        URI attachmentGraphUri;
        URI destinationUri;

        AttachmentMetaData(URI attachmentGraphUri, URI destinationUri) {
            this.attachmentGraphUri = attachmentGraphUri;
            this.destinationUri = destinationUri;
        }

        public URI getAttachmentGraphUri() {
            return attachmentGraphUri;
        }

        public URI getDestinationUri() {
            return destinationUri;
        }
    }

    public static class AttachmentHolder {
        private URI destinationUri;
        // holds the attachment graph and the signature graph
        private Dataset attachmentDataset;

        public AttachmentHolder(URI destinationUri, Dataset attachmentDataset) {
            this.destinationUri = destinationUri;
            this.attachmentDataset = attachmentDataset;
        }

        public URI getDestinationUri() {
            return destinationUri;
        }

        public Dataset getAttachmentDataset() {
            return attachmentDataset;
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getMessageURI() == null) ? 0 : getMessageURI().hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        WonMessage other = (WonMessage) obj;
        if (getMessageURI() == null) {
            if (other.getMessageURI() != null)
                return false;
        } else if (!getMessageURI().equals(other.getMessageURI()))
            return false;
        return true;
    }

    public boolean isMessageWithBothResponses() {
        return !headMessage.getMessageTypeRequired().isResponseMessage() && getResponse().isPresent()
                        && getRemoteResponse().isPresent();
    }

    public boolean isMessageWithResponse() {
        return !headMessage.getMessageTypeRequired().isResponseMessage() && getResponse().isPresent()
                        && !getRemoteResponse().isPresent();
    }

    public boolean isRemoteResponse() {
        return headMessage.getMessageTypeRequired().isResponseMessage() && messages == null;
    }
}

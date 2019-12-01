package won.protocol.message.builder;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.jboss.logging.Message;

import won.protocol.exception.WonMessageBuilderException;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageDirection;
import won.protocol.message.WonMessageType;
import won.protocol.util.CheapInsecureRandomString;
import won.protocol.util.DefaultPrefixUtils;
import won.protocol.util.RdfUtils;
import won.protocol.util.WonMessageUriHelper;
import won.protocol.util.WonRdfUtils;
import won.protocol.vocabulary.RDFG;
import won.protocol.vocabulary.WONMSG;

/**
 * Class to build a WonMessage based on the specific properties.
 *
 * @author Fabian Salcher
 */
public class WonMessageBuilder {
    public static final String CONTENT_URI_SUFFIX = "#content-";
    private static final CheapInsecureRandomString randomString = new CheapInsecureRandomString();
    private static final int RANDOM_SUFFIX_LENGTH = 5;
    private final URI messageURI;
    private URI atomURI;
    URI connectionURI;
    private URI senderSocketURI;
    private URI recipientSocketURI;
    private URI hintTargetAtomURI;
    private URI hintTargetSocketURI;
    private Double hintScore;
    private Set<URI> injectIntoConnections = new HashSet<>();
    WonMessageType wonMessageType;
    private WonMessageDirection wonMessageDirection;
    // if the message is a response message, it MUST have exactly one
    // isResponseToMessageURI set.
    private URI isResponseToMessageURI;
    // if the message is a response message, and the original message has a
    // correspondingRemoteMessage, set it here
    private WonMessageType isResponseToMessageType;
    // some message exist twice: once on the receiver's won node and once on the
    // sender's.
    // this property allows to link to the corresponding remote message
    private URI correspondingRemoteMessageURI;
    // messages can be forwarded. if they are, they are added to the message and
    // referenced:
    private Map<URI, Model> contentMap = new HashMap<>();
    private Map<URI, Model> signatureMap = new HashMap<>();
    private List<WonMessage> forwardedMessages;
    private List<URI> previousMessages;
    private Long timestamp;

    public WonMessageBuilder() {
        this.messageURI = WonMessageUriHelper.getSelfUri();
    }

    public static CloseBuilder close() {
        return new CloseBuilder(new WonMessageBuilder());
    }

    public static ConnectionMessageBuilder connectionMessage() {
        return new ConnectionMessageBuilder(new WonMessageBuilder());
    }

    public static ConnectBuilder connect() {
        return new ConnectBuilder(new WonMessageBuilder());
    }

    public static ResponseBuilder response() {
        return new ResponseBuilder(new WonMessageBuilder());
    }

    public static CreateAtomBuilder createAtom() {
        return new CreateAtomBuilder(new WonMessageBuilder());
    }

    public static ReplaceBuilder replace() {
        return new ReplaceBuilder(new WonMessageBuilder());
    }

    public static DeactivateBuilder deactivate() {
        return new DeactivateBuilder(new WonMessageBuilder());
    }

    public static ActivateBuilder activate() {
        return new ActivateBuilder(new WonMessageBuilder());
    }

    public static AtomMessageBuilder atomMessage() {
        return new AtomMessageBuilder(new WonMessageBuilder());
    }

    public static AtomCreatedNotificationBuilder atomCreatedNotification() {
        return new AtomCreatedNotificationBuilder(new WonMessageBuilder());
    }

    public static HintFeedbackMessageBuilder hintFeedbackMessage() {
        return new HintFeedbackMessageBuilder(new WonMessageBuilder());
    }

    public static DeleteBuilder delete() {
        return new DeleteBuilder(new WonMessageBuilder());
    }

    public static ChangeNotificationBuilder changeNotificatin() {
        return new ChangeNotificationBuilder(new WonMessageBuilder());
    }

    public static AtomHintBuilder atomHint() {
        return new AtomHintBuilder(new WonMessageBuilder());
    }

    public static SocketHintBuilder socketHint() {
        return new SocketHintBuilder(new WonMessageBuilder());
    }

    WonMessage build() throws WonMessageBuilderException {
        return build(DatasetFactory.createGeneral());
    }

    /**
     * Builds a WonMessage by adding data to the specified dataset. The dataset may
     * be null or empty.
     * 
     * @param dataset
     * @return
     * @throws WonMessageBuilderException
     */
    public WonMessage build(final Dataset dataset) throws WonMessageBuilderException {
        if (dataset == null) {
            throw new IllegalArgumentException(
                            "specified dataset must not be null. If a new dataset is to be created for the message, build() should be called.");
        }
        if (messageURI == null) {
            throw new WonMessageBuilderException("No messageURI specified");
        }
        Model envelopeGraph = ModelFactory.createDefaultModel();
        DefaultPrefixUtils.setDefaultPrefixes(envelopeGraph);
        // create a new envelope graph uri and add the envelope graph to the dataset
        // ... and make sure that the graph URI will be new by also checking inside the
        // wrapped message
        String envelopeGraphURI = messageURI.toString() + WonMessage.ENVELOPE_URI_SUFFIX;
        dataset.addNamedModel(envelopeGraphURI, envelopeGraph);
        // message URI
        Resource messageEventResource = envelopeGraph.createResource(messageURI.toString(),
                        this.wonMessageDirection.getResource());
        // the [envelopeGraphURI] rdf:type msg:EnvelopeGraph makes it easy to select
        // graphs by type
        Resource envelopeGraphResource = envelopeGraph.createResource(envelopeGraphURI, WONMSG.EnvelopeGraph);
        envelopeGraph.add(messageEventResource, WONMSG.envelope, envelopeGraphResource);
        // make sure the envelope type has been set
        if (this.wonMessageDirection == null) {
            throw new IllegalStateException("envelopeType must be set!");
        }
        if (wonMessageType != null) {
            messageEventResource.addProperty(WONMSG.messageType, wonMessageType.getResource());
        }
        messageEventResource.addLiteral(WONMSG.protocolVersion, envelopeGraph.createTypedLiteral("1.0"));
        // add sender
        if (atomURI != null)
            messageEventResource.addProperty(WONMSG.atom, envelopeGraph.createResource(atomURI.toString()));
        if (connectionURI != null)
            messageEventResource.addProperty(WONMSG.connection, envelopeGraph.createResource(connectionURI.toString()));
        if (senderSocketURI != null) {
            messageEventResource.addProperty(WONMSG.senderSocket,
                            envelopeGraph.createResource(senderSocketURI.toString()));
        }
        if (recipientSocketURI != null) {
            messageEventResource.addProperty(WONMSG.recipientSocket,
                            envelopeGraph.createResource(recipientSocketURI.toString()));
        }
        // add hint properties
        if (this.wonMessageType == WonMessageType.ATOM_HINT_MESSAGE) {
            if (hintTargetAtomURI == null) {
                throw new IllegalArgumentException("AtomHintMessage must have a hintTargetAtom");
            }
            if (hintScore == null) {
                throw new IllegalArgumentException("AtomHintMessage must have a hintScore");
            }
            if (hintTargetSocketURI != null) {
                throw new IllegalArgumentException("AtomHintMessage must not have a hintTargetSocket");
            }
            if (recipientSocketURI != null) {
                throw new IllegalArgumentException("AtomHintMessage must not have a recipientSocket");
            }
            messageEventResource.addProperty(WONMSG.hintTargetAtom,
                            envelopeGraph.createResource(hintTargetAtomURI.toString()));
            messageEventResource.addProperty(WONMSG.hintScore, hintScore.toString(), XSDDatatype.XSDfloat);
        } else if (this.wonMessageType == WonMessageType.SOCKET_HINT_MESSAGE) {
            if (hintTargetSocketURI == null) {
                throw new IllegalArgumentException("SocketHintMessage must have a hintTargetSocket");
            }
            if (hintScore == null) {
                throw new IllegalArgumentException("SocketHintMessage must have a hintScore");
            }
            if (hintTargetAtomURI != null) {
                throw new IllegalArgumentException("SocketHintMessage must not have a hintTargetAtom");
            }
            if (recipientSocketURI == null) {
                throw new IllegalArgumentException("SocketHintMessage must have a recipientSocket");
            }
            messageEventResource.addProperty(WONMSG.hintTargetSocket,
                            envelopeGraph.createResource(hintTargetSocketURI.toString()));
            messageEventResource.addProperty(WONMSG.hintScore, hintScore.toString(), XSDDatatype.XSDfloat);
        }
        // add forwards
        if (!injectIntoConnections.isEmpty()) {
            injectIntoConnections.forEach(receiver -> messageEventResource.addProperty(WONMSG.injectIntoConnection,
                            envelopeGraph.getResource(receiver.toString())));
        }
        if (isResponseToMessageURI != null) {
            if (wonMessageType != WonMessageType.SUCCESS_RESPONSE
                            && wonMessageType != WonMessageType.FAILURE_RESPONSE) {
                throw new IllegalArgumentException("isResponseToMessageURI can only be used for SUCCESS_RESPONSE and "
                                + "FAILURE_RESPONSE types");
            }
            if (isResponseToMessageType == null) {
                throw new IllegalArgumentException(
                                "response messages must specify the type of message they are a response to"
                                                + ". Use setIsResponseToMessageType(type)");
            }
            messageEventResource.addProperty(WONMSG.respondingTo,
                            envelopeGraph.createResource(isResponseToMessageURI.toString()));
            messageEventResource.addProperty(WONMSG.respondingToMessageType,
                            this.isResponseToMessageType.getResource());
        }
        if (forwardedMessages != null) {
            forwardedMessages.forEach(msg -> {
                messageEventResource.addProperty(WONMSG.forwardedMessage,
                                envelopeGraph.getResource(msg.getMessageURIRequired().toString()));
                RdfUtils.addDatasetToDataset(dataset, msg.getCompleteDataset());
            });
        }
        if (previousMessages != null) {
            previousMessages.forEach(msg -> {
                messageEventResource.addProperty(WONMSG.previousMessage,
                                envelopeGraph.getResource(msg.toString()));
            });
        }
        if (timestamp != null) {
            messageEventResource.addProperty(WONMSG.timestamp,
                            envelopeGraph.createTypedLiteral(this.timestamp));
        }
        for (URI contentURI : contentMap.keySet()) {
            String contentUriString = contentURI.toString();
            dataset.addNamedModel(contentUriString, contentMap.get(contentURI));
            messageEventResource.addProperty(WONMSG.content,
                            messageEventResource.getModel().createResource(contentUriString));
            Model signatureGraph = signatureMap.get(contentURI);
            if (signatureGraph != null) {
                throw new UnsupportedOperationException("signatures are not supported yet");
                /*
                 * in principle, this should work, but its untested: uniqueContentUri =
                 * RdfUtils.createNewGraphURI(contentURI.toString(), SIGNATURE_URI_SUFFIX, 5,
                 * dataset).toString(); //the signature refers to the name of the other graph.
                 * We changed that name //so we have to replace the resource referencing it,
                 * too: signatureGraph =
                 * RdfUtils.replaceResource(signatureGraph.getResource(contentURI.toString()),
                 * signatureGraph.getResource(uniqueContentUri));
                 * dataset.addNamedModel(uniqueContentUri, signatureGraph);
                 */
            }
            // now replace the content URIs
        }
        return WonMessage.of(dataset);
    }

    URI getAtom() {
        return atomURI;
    }

    WonMessageBuilder atom(URI atomURI) {
        this.atomURI = atomURI;
        return this;
    }

    URI getConnection() {
        return connectionURI;
    }

    WonMessageBuilder connection(URI connectionURI) {
        this.connectionURI = connectionURI;
        return this;
    }

    WonMessageBuilder senderSocket(URI senderSocketURI) {
        Objects.requireNonNull(senderSocketURI);
        this.senderSocketURI = senderSocketURI;
        return this;
    }

    WonMessageBuilder recipientSocket(URI recipientSocketURI) {
        Objects.requireNonNull(recipientSocketURI);
        this.recipientSocketURI = recipientSocketURI;
        return this;
    }

    WonMessageBuilder type(WonMessageType wonMessageType) {
        Objects.requireNonNull(wonMessageType);
        this.wonMessageType = wonMessageType;
        return this;
    }

    WonMessageBuilder direction(WonMessageDirection wonMessageDirection) {
        Objects.requireNonNull(wonMessageDirection);
        this.wonMessageDirection = wonMessageDirection;
        return this;
    }

    /**
     * Adds the specified content graph, and the specified signature graph, using
     * the specified contentURI as the graph name. The contentURI will be made
     * unique inside the message dataset by appending characters at the end.
     * 
     * @param content
     * @return
     */
    WonMessageBuilder content(Model content) {
        Objects.requireNonNull(content);
        addContentInternal(content);
        return this;
    }

    private URI addContentInternal(Model content) {
        URI contentGraphUri = RdfUtils.createNewGraphURI(messageURI.toString(), CONTENT_URI_SUFFIX, 4,
                        graphUri -> !contentMap.keySet().contains(URI.create(graphUri)));
        contentMap.put(contentGraphUri, content);
        return contentGraphUri;
    }

    /**
     * Adds all graphs in the specified dataset as content graphs to the message. In
     * this process, unique graph URIs are generated for all graphs in the dataset,
     * including the default graph (if present). If graphs are referenced within the
     * dataset through a triple in which the graph uri is the object, all such
     * references are changed to refer to the newly generated graph uri.
     * 
     * @param dataset
     * @return
     */
    WonMessageBuilder content(Dataset dataset) {
        Objects.requireNonNull(dataset);
        Dataset toAdd = RdfUtils.cloneDataset(dataset);
        // we can add the default model without remembering the newly
        // generated URI because there cannot be a reference
        // to the default model in the other graphs
        Model model = toAdd.getDefaultModel();
        if (model != null && model.size() > 0) {
            content(model);
        }
        // add each model, remembering a mapping between the old
        // and the new graph uri
        final Map<String, String> changedGraphUris = new HashMap<>();
        RdfUtils.toNamedModelStream(toAdd, false).forEach(namedModel -> {
            if (namedModel.getModel().size() == 0)
                return;
            URI newUri = addContentInternal(namedModel.getModel());
            changedGraphUris.put(namedModel.getName(), newUri.toString());
        });
        // replace the old graph uris with the new graph
        // uris in all graphs of our dataset
        RdfUtils.visit(toAdd, model1 -> {
            if (model1.size() == 0)
                return null;
            changedGraphUris.entrySet().stream().forEach(graphNameMapping -> {
                // in the model, get both the old and new resource, then replace
                // the old by the new. Note: This will create a resource in the
                // model if it is not in there yet.
                Resource refToOld = model1.getResource(graphNameMapping.getKey());
                Resource refToNew = model1.getResource(graphNameMapping.getValue());
                // replace resource, modifying the model (which is already in
                // the builder's content map
                RdfUtils.replaceResourceInModel(refToOld, refToNew);
            });
            return null;
        });
        return this;
    }

    /**
     * Retrieves one of the possibly multiple Models that does not have a signature
     * yet. If there is none (all are signed or none is found at all), a new model
     * is created, added to the internal contentMap and returned here.
     */
    private Model getUnsignedContentGraph() {
        if (contentMap.isEmpty()) {
            // no content graphs yet. Make one and return it.
            Model contentGraph = ModelFactory.createDefaultModel();
            RdfUtils.replaceBaseURI(contentGraph, this.messageURI.toString());
            content(contentGraph);
            return contentGraph;
        }
        // content map is not empty. find one without a signature:
        for (Map.Entry<URI, Model> entry : contentMap.entrySet()) {
            if (!signatureMap.containsKey(entry.getKey()))
                return entry.getValue();
        }
        // all content graphs are signed. add a new one.
        Model contentGraph = ModelFactory.createDefaultModel();
        RdfUtils.replaceBaseURI(contentGraph, this.messageURI.toString());
        content(contentGraph);
        return contentGraph;
    }

    WonMessageBuilder respondingToMessage(URI isResponseToMessageURI) {
        Objects.requireNonNull(isResponseToMessageURI);
        this.isResponseToMessageURI = isResponseToMessageURI;
        return this;
    }

    WonMessageBuilder respondingToMessageType(final WonMessageType isResponseToMessageType) {
        Objects.requireNonNull(isResponseToMessageType);
        this.isResponseToMessageType = isResponseToMessageType;
        return this;
    }

    WonMessageBuilder injectIntoConnections(Collection<URI> forwardToRecipientUris) {
        Objects.requireNonNull(forwardToRecipientUris);
        this.injectIntoConnections.addAll(forwardToRecipientUris);
        return this;
    }

    private WonMessageBuilder timestamp(Long timestamp) {
        Objects.requireNonNull(timestamp);
        this.timestamp = timestamp;
        return this;
    }

    WonMessageBuilder timestampNow() {
        this.timestamp = System.currentTimeMillis();
        return this;
    }

    URI getMessageURI() {
        return messageURI;
    }

    WonMessageBuilder hintScore(Double hintScore) {
        Objects.requireNonNull(hintScore);
        this.hintScore = hintScore;
        return this;
    }

    WonMessageBuilder hintTargetAtom(URI hintTargetAtomURI) {
        Objects.requireNonNull(hintTargetAtomURI);
        this.hintTargetAtomURI = hintTargetAtomURI;
        return this;
    }

    WonMessageBuilder hintTargetSocket(URI hintTargetSocketURI) {
        Objects.requireNonNull(hintTargetSocketURI);
        this.hintTargetSocketURI = hintTargetSocketURI;
        return this;
    }

    /**
     * Adds a Text message to one of the message's content graphs. If only one graph
     * is present, the text message is added to that graph. If more than one graph
     * is present, and hence we cannot decide for any one of them, a new content
     * graph is created for the text. If no content graphs are present, a new one is
     * created.
     * 
     * @param textMessage
     * @return
     */
    WonMessageBuilder textMessage(String textMessage) {
        Objects.requireNonNull(textMessage);
        Model model = getModelForAddingContent();
        RdfUtils.findOrCreateBaseResource(model);
        RdfUtils.replaceBaseResource(model, model.createResource(this.getMessageURI().toString()));
        WonRdfUtils.MessageUtils.addMessage(model, textMessage);
        return this;
    }

    /**
     * Returns a model for adding data to one of the message's content graphs. If
     * only one graph is present, the text message is added to that graph. If more
     * than one graph is present, and hence we cannot decide for any one of them, a
     * new content graph is created for the text. If no content graphs are present,
     * a new one is created.
     * 
     * @param textMessage
     * @return
     */
    Model getModelForAddingContent() {
        Model model = null;
        if (this.contentMap.size() == 1) {
            model = contentMap.values().stream().findFirst().get();
        } else {
            model = ModelFactory.createDefaultModel();
            addContentInternal(model);
        }
        return model;
    }

    WonMessageBuilder previousMessage(URI previousMessageURI) {
        if (this.previousMessages == null) {
            this.previousMessages = new ArrayList<URI>();
        }
        this.previousMessages.add(previousMessageURI);
        return this;
    }

    /**
     * Adds the complete message content to the message that will be built,
     * referencing toForward's envelope in the envelope of the new message.
     *
     * @param
     * @return
     */
    WonMessageBuilder forward(WonMessage toForward) {
        // make a copy to avoid modification in current message in case wrapped message
        // is modified externally
        if (this.forwardedMessages == null) {
            this.forwardedMessages = new ArrayList<WonMessage>();
        }
        this.forwardedMessages.add(toForward);
        return this;
    }
}

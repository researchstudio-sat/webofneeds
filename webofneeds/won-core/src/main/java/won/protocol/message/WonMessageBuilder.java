package won.protocol.message;

import java.net.URI;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;

import won.protocol.exception.WonMessageBuilderException;
import won.protocol.util.CheapInsecureRandomString;
import won.protocol.util.DefaultPrefixUtils;
import won.protocol.util.RdfUtils;
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
    public static final String SIGNATURE_URI_SUFFIX = "#signature-";
    public static final String ENVELOPE_URI_SUFFIX = "#envelope-";
    private static final CheapInsecureRandomString randomString = new CheapInsecureRandomString();
    private static final int RANDOM_SUFFIX_LENGTH = 5;
    private final URI messageURI;
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
    private Set<URI> injectIntoConnections = new HashSet<>();
    private WonMessageType wonMessageType;
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
    private URI forwardedMessageURI;
    private Map<URI, Model> contentMap = new HashMap<>();
    private Map<URI, Model> signatureMap = new HashMap<>();
    private WonMessage wrappedMessage;
    private WonMessage forwardedMessage;
    private Long timestamp;

    public WonMessageBuilder(URI messageURI) {
        Objects.requireNonNull(messageURI);
        this.messageURI = messageURI;
    }

    private WonMessageBuilder() {
        throw new UnsupportedOperationException("A messageURI must be provided when creating the WonMessageBuilder");
    }

    public WonMessage build() throws WonMessageBuilderException {
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
        String envelopeGraphURI = RdfUtils
                        .createNewGraphURI(messageURI.toString(), ENVELOPE_URI_SUFFIX, 4, graphUri -> {
                            if (dataset.containsNamedModel(graphUri))
                                return false;
                            if (wrappedMessage == null)
                                return true;
                            if (wrappedMessage.getEnvelopeGraphURIs().contains(graphUri))
                                return false;
                            return !wrappedMessage.getContentGraphURIs().contains(graphUri);
                        }).toString();
        dataset.addNamedModel(envelopeGraphURI, envelopeGraph);
        // message URI
        Resource messageEventResource = envelopeGraph.createResource(messageURI.toString(),
                        this.wonMessageDirection.getResource());
        // the [envelopeGraphURI] rdf:type msg:EnvelopeGraph makes it easy to select
        // graphs by type
        Resource envelopeGraphResource = envelopeGraph.createResource(envelopeGraphURI, WONMSG.EnvelopeGraph);
        envelopeGraphResource.addProperty(RDFG.SUBGRAPH_OF, messageEventResource);
        addWrappedOrForwardedMessage(dataset, envelopeGraph, envelopeGraphResource, messageURI);
        // make sure the envelope type has been set
        if (this.wonMessageDirection == null) {
            throw new IllegalStateException("envelopeType must be set!");
        }
        if (wonMessageType != null) {
            messageEventResource.addProperty(WONMSG.messageType, wonMessageType.getResource());
        }
        messageEventResource.addLiteral(WONMSG.PROTOCOL_VERSION, envelopeGraph.createTypedLiteral("1.0"));
        // add sender
        if (senderURI != null)
            messageEventResource.addProperty(WONMSG.sender, envelopeGraph.createResource(senderURI.toString()));
        if (senderAtomURI != null)
            messageEventResource.addProperty(WONMSG.senderAtom, envelopeGraph.createResource(senderAtomURI.toString()));
        if (senderNodeURI != null)
            messageEventResource.addProperty(WONMSG.senderNode, envelopeGraph.createResource(senderNodeURI.toString()));
        if (senderSocketURI != null) {
            messageEventResource.addProperty(WONMSG.senderSocket,
                            envelopeGraph.createResource(senderSocketURI.toString()));
        }
        // add receiver
        if (recipientURI != null)
            messageEventResource.addProperty(WONMSG.recipient, envelopeGraph.createResource(recipientURI.toString()));
        if (recipientAtomURI != null)
            messageEventResource.addProperty(WONMSG.recipientAtom,
                            envelopeGraph.createResource(recipientAtomURI.toString()));
        if (recipientNodeURI != null)
            messageEventResource.addProperty(WONMSG.recipientNode,
                            envelopeGraph.createResource(recipientNodeURI.toString()));
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
        if (forwardedMessageURI != null) {
            messageEventResource.addProperty(WONMSG.forwardedMessage,
                            envelopeGraph.createResource(forwardedMessageURI.toString()));
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
            // add the [content-graph] rdfg:subGraphOf [message-uri] triple to the envelope
            envelopeGraph.createStatement(envelopeGraph.getResource(contentURI.toString()), RDFG.SUBGRAPH_OF,
                            messageEventResource);
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

    public void addWrappedOrForwardedMessage(final Dataset dataset, final Model envelopeGraph,
                    final Resource envelopeGraphResource, URI messageURI) {
        // add wrapped message first, including all its named graphs.
        // This way, we can later avoid clashed when generating new graph URIs
        if (this.wrappedMessage != null) {
            if (this.forwardedMessage != null)
                throw new IllegalStateException("cannot wrap and forward with the same " + "builder");
            addAsContainedEnvelope(dataset, envelopeGraph, envelopeGraphResource, wrappedMessage, messageURI);
        }
        // add forwarded message next, including all its named graphs.
        // This way, we can later avoid clashed when generating new graph URIs
        if (this.forwardedMessage != null) {
            if (this.wrappedMessage != null)
                throw new IllegalStateException("cannot wrap and forward with the same " + "builder");
            addAsContainedEnvelope(dataset, envelopeGraph, envelopeGraphResource, forwardedMessage, messageURI);
        }
    }

    public void addAsContainedEnvelope(final Dataset dataset, final Model envelopeGraph,
                    final Resource envelopeGraphResource, WonMessage messageToAdd, URI messageURI) {
        String messageUriString = messageURI.toString();
        // the [wrappedMessage.envelopeGraphURI] rdf:type msg:EnvelopeGraph triple in
        // the default graph is required to
        // find the wrapped envelope graph.
        envelopeGraphResource.addProperty(WONMSG.containsEnvelope,
                        envelopeGraph.getResource(messageToAdd.getOuterEnvelopeGraphURI().toString()));
        // copy all named graphs to the new message dataset
        for (Iterator<String> names = messageToAdd.getCompleteDataset().listNames(); names.hasNext();) {
            String graphUri = names.next();
            Model modelToAdd = messageToAdd.getCompleteDataset().getNamedModel(graphUri);
            dataset.addNamedModel(graphUri, modelToAdd);
            // define that the added graph is a subgraph of the message if that is not yet
            // expressed in the graph itself
            if (!modelToAdd.contains(modelToAdd.getResource(graphUri), RDFG.SUBGRAPH_OF,
                            modelToAdd.getResource(messageUriString))) {
                envelopeGraph.createStatement(envelopeGraph.getResource(graphUri), RDFG.SUBGRAPH_OF,
                                envelopeGraph.getResource(messageUriString));
            }
        }
    }

    public static WonMessageBuilder setMessagePropertiesForClose(URI messageURI, WonMessage connectToReactTo,
                    String farewellMessage) {
        return setMessagePropertiesForClose(messageURI, connectToReactTo.getRecipientSocketURI(),
                        connectToReactTo.getRecipientURI(),
                        connectToReactTo.getRecipientAtomURI(), connectToReactTo.getRecipientNodeURI(),
                        connectToReactTo.getSenderSocketURI(), connectToReactTo.getSenderURI(),
                        connectToReactTo.getSenderAtomURI(),
                        connectToReactTo.getSenderNodeURI(), farewellMessage);
    }

    public static WonMessageBuilder setMessagePropertiesForClose(URI messageURI, URI localSocket, URI localConnection,
                    URI localAtom,
                    URI localWonNode, URI targetSocket, URI targetConnection, URI targetAtom, URI remoteWonNode,
                    String farewellMessage) {
        return setMessagePropertiesForClose(messageURI, WonMessageDirection.FROM_OWNER, localSocket, localConnection,
                        localAtom,
                        localWonNode, targetSocket, targetConnection, targetAtom, remoteWonNode, farewellMessage);
    }

    public static WonMessageBuilder setMessagePropertiesForClose(URI messageURI, WonMessageDirection direction,
                    URI localSocket,
                    URI localConnection, URI localAtom, URI localWonNode, URI targetSocket, URI targetConnection,
                    URI targetAtom,
                    URI remoteWonNode, String farewellMessage) {
        return new WonMessageBuilder(messageURI).setWonMessageDirection(direction)
                        .setWonMessageType(WonMessageType.CLOSE)
                        .setSenderSocketURI(localSocket)
                        .setSenderURI(localConnection)
                        .setSenderAtomURI(localAtom)
                        .setSenderNodeURI(localWonNode)
                        .setRecipientSocketURI(targetSocket)
                        .setRecipientURI(targetConnection)
                        .setRecipientAtomURI(targetAtom)
                        .setRecipientNodeURI(remoteWonNode)
                        .setTextMessage(farewellMessage)
                        .setTimestampToNow();
    }

    public static WonMessageBuilder setMessagePropertiesForClose(URI messageURI, URI localSocket, URI localConnection,
                    URI localAtom,
                    URI localWonNode, String farewellMessage) {
        return setMessagePropertiesForClose(messageURI, WonMessageDirection.FROM_OWNER, localSocket, localConnection,
                        localAtom,
                        localWonNode, farewellMessage);
    }

    public static WonMessageBuilder setMessagePropertiesForClose(URI messageURI, WonMessageDirection direction,
                    URI localSocket, URI localConnection, URI localAtom, URI localWonNode, String farewellMessage) {
        return setMessagePropertiesForClose(messageURI, WonMessageDirection.FROM_OWNER, localSocket, localConnection,
                        localAtom,
                        localWonNode, localSocket, localConnection, localAtom, localWonNode, farewellMessage);
    }

    /**
     * Sets the MessageProperties for Closing open connections (happens when the
     * atom is closed and the system is closing all the corresponding connections
     * when no connection is present from the targetAtom
     * 
     * @param messageURI
     * @param localConnection
     * @param localAtom
     * @param localWonNode
     * @return
     */
    public static WonMessageBuilder setMessagePropertiesForLocalOnlyClose(URI messageURI, URI localConnection,
                    URI localAtom, URI localWonNode) {
        return new WonMessageBuilder(messageURI).setWonMessageDirection(WonMessageDirection.FROM_SYSTEM)
                        .setWonMessageType(WonMessageType.CLOSE).setSenderURI(localConnection)
                        .setSenderAtomURI(localAtom).setSenderNodeURI(localWonNode).setTimestampToNow();
    }

    public static WonMessageBuilder setMessagePropertiesForSystemMessageToTargetAtom(URI messageURI,
                    URI localConnection, URI localAtom, URI localWonNode, URI targetConnection, URI targetAtom,
                    URI remoteNode, String textMessage) {
        return new WonMessageBuilder(messageURI).setWonMessageDirection(WonMessageDirection.FROM_SYSTEM)
                        .setWonMessageType(WonMessageType.CONNECTION_MESSAGE).setSenderURI(localConnection)
                        .setSenderAtomURI(localAtom).setSenderNodeURI(localWonNode).setRecipientURI(targetConnection)
                        .setRecipientAtomURI(targetAtom).setRecipientNodeURI(remoteNode).setTextMessage(textMessage)
                        .setTimestampToNow();
    }

    public static WonMessageBuilder setMessagePropertiesForSystemChangeNotificationMessageToTargetAtom(URI messageURI,
                    URI localConnection, URI localAtom, URI localWonNode, URI targetConnection, URI targetAtom,
                    URI remoteNode, String textMessage) {
        return new WonMessageBuilder(messageURI).setWonMessageDirection(WonMessageDirection.FROM_SYSTEM)
                        .setWonMessageType(WonMessageType.CHANGE_NOTIFICATION).setSenderURI(localConnection)
                        .setSenderAtomURI(localAtom).setSenderNodeURI(localWonNode).setRecipientURI(targetConnection)
                        .setRecipientAtomURI(targetAtom).setRecipientNodeURI(remoteNode).setTextMessage(textMessage)
                        .setTimestampToNow();
    }

    public static WonMessageBuilder setMessagePropertiesForSystemChangeNotificationMessageToTargetAtom(URI messageURI,
                    URI localConnection, URI localAtom, URI localWonNode, URI targetConnection, URI targetAtom,
                    URI remoteNode) {
        return setMessagePropertiesForSystemChangeNotificationMessageToTargetAtom(messageURI, localConnection,
                        localAtom, localWonNode, targetConnection, targetAtom, remoteNode, null);
    }

    public static WonMessageBuilder setMessagePropertiesForDeactivateFromOwner(URI messageURI, URI localAtom,
                    URI localWonNode) {
        return new WonMessageBuilder(messageURI).setWonMessageDirection(WonMessageDirection.FROM_OWNER)
                        .setWonMessageType(WonMessageType.DEACTIVATE).setSenderAtomURI(localAtom)
                        .setSenderNodeURI(localWonNode).setRecipientAtomURI(localAtom).setRecipientNodeURI(localWonNode)
                        .setTimestampToNow();
    }

    public static WonMessageBuilder setMessagePropertiesForActivateFromOwner(URI messageURI, URI localAtom,
                    URI localWonNode) {
        return new WonMessageBuilder(messageURI).setWonMessageDirection(WonMessageDirection.FROM_OWNER)
                        .setWonMessageType(WonMessageType.ACTIVATE).setSenderAtomURI(localAtom)
                        .setSenderNodeURI(localWonNode).setRecipientAtomURI(localAtom).setRecipientNodeURI(localWonNode)
                        .setTimestampToNow();
    }

    public static WonMessageBuilder setMessagePropertiesForDeleteFromOwner(URI messageURI, URI localAtom,
                    URI localWonNode) {
        return new WonMessageBuilder(messageURI).setWonMessageDirection(WonMessageDirection.FROM_OWNER)
                        .setWonMessageType(WonMessageType.DELETE).setSenderAtomURI(localAtom)
                        .setSenderNodeURI(localWonNode).setRecipientAtomURI(localAtom).setRecipientNodeURI(localWonNode)
                        .setTimestampToNow();
    }

    public static WonMessageBuilder setMessagePropertiesForDeactivateFromSystem(URI messageURI, URI localAtom,
                    URI localWonNode) {
        return new WonMessageBuilder(messageURI).setWonMessageDirection(WonMessageDirection.FROM_SYSTEM)
                        .setWonMessageType(WonMessageType.DEACTIVATE).setSenderAtomURI(localAtom)
                        .setSenderNodeURI(localWonNode).setRecipientAtomURI(localAtom).setRecipientNodeURI(localWonNode)
                        .setTimestampToNow();
    }

    /**
     * Sets message properties for sending a 'atom message' from System to Owner,
     * i.e. a notification from the node to the owner. This message will have no
     * effect on atom or connection states and it is expected that a payload (e.g.
     * via setTextMessage()) is added to the message builder prior to calling the
     * build() method.
     * 
     * @param messageURI
     * @param localAtom
     * @param localWonNode
     * @return
     */
    public static WonMessageBuilder setMessagePropertiesForAtomMessageFromSystem(URI messageURI, URI localAtom,
                    URI localWonNode) {
        return new WonMessageBuilder(messageURI).setWonMessageDirection(WonMessageDirection.FROM_SYSTEM)
                        .setWonMessageType(WonMessageType.ATOM_MESSAGE).setSenderAtomURI(localAtom)
                        .setSenderNodeURI(localWonNode).setRecipientAtomURI(localAtom).setRecipientNodeURI(localWonNode)
                        .setTimestampToNow();
    }

    public static WonMessageBuilder setMessagePropertiesForConnect(URI messageURI,
                    WonMessage connectToReactTo,
                    String welcomeMessage) {
        return setMessagePropertiesForRespondingConnect(messageURI,
                        connectToReactTo.getRecipientSocketURIRequired(),
                        connectToReactTo.getRecipientAtomURIRequired(),
                        connectToReactTo.getRecipientNodeURIRequired(),
                        connectToReactTo.getSenderSocketURIRequired(),
                        connectToReactTo.getSenderAtomURIRequired(),
                        connectToReactTo.getSenderNodeURIRequired(),
                        welcomeMessage);
    }

    public static WonMessageBuilder setMessagePropertiesForRespondingConnect(URI messageURI,
                    URI localSocket,
                    URI localAtom, URI localWonNode, URI targetSocket, URI targetAtom,
                    URI remoteWonNode,
                    String welcomeMessage) {
        // create content model
        Model model = ModelFactory.createDefaultModel();
        RdfUtils.findOrCreateBaseResource(model);
        RdfUtils.replaceBaseResource(model, model.createResource(messageURI.toString()));
        if (welcomeMessage != null) {
            WonRdfUtils.MessageUtils.addMessage(model, welcomeMessage);
        }
        WonMessageBuilder builder = new WonMessageBuilder(messageURI)
                        .setWonMessageDirection(WonMessageDirection.FROM_OWNER)
                        .setWonMessageType(WonMessageType.CONNECT).setSenderAtomURI(localAtom)
                        .setSenderNodeURI(localWonNode).setRecipientAtomURI(targetAtom)
                        .setRecipientNodeURI(remoteWonNode)
                        .setSenderSocketURI(localSocket)
                        .setRecipientSocketURI(targetSocket);
        return builder.addContent(model).setTimestampToNow();
    }

    public static WonMessageBuilder setMessagePropertiesForConnect(URI messageURI, URI localSocket,
                    URI localAtom, URI localWonNode, URI targetSocket, URI targetAtom, URI remoteWonNode,
                    String welcomeMessage) {
        // create content model
        Model model = ModelFactory.createDefaultModel();
        RdfUtils.findOrCreateBaseResource(model);
        RdfUtils.replaceBaseResource(model, model.createResource(messageURI.toString()));
        if (welcomeMessage != null) {
            WonRdfUtils.MessageUtils.addMessage(model, welcomeMessage);
        }
        WonMessageBuilder builder = new WonMessageBuilder(messageURI)
                        .setWonMessageDirection(WonMessageDirection.FROM_OWNER)
                        .setWonMessageType(WonMessageType.CONNECT).setSenderAtomURI(localAtom)
                        .setSenderNodeURI(localWonNode).setRecipientAtomURI(targetAtom)
                        .setRecipientNodeURI(remoteWonNode)
                        .setSenderSocketURI(localSocket)
                        .setRecipientSocketURI(targetSocket);
        return builder.addContent(model).setTimestampToNow();
    }

    public static WonMessageBuilder setMessagePropertiesForCreate(URI messageURI, URI atomURI, URI wonNodeURI) {
        return new WonMessageBuilder(messageURI).setWonMessageDirection(WonMessageDirection.FROM_OWNER)
                        .setWonMessageType(WonMessageType.CREATE_ATOM).setSenderAtomURI(atomURI)
                        .setSenderNodeURI(wonNodeURI).setRecipientNodeURI(wonNodeURI).setTimestampToNow();
    }

    public static WonMessageBuilder setMessagePropertiesForReplace(URI messageURI, URI atomURI, URI wonNodeURI) {
        return new WonMessageBuilder(messageURI).setWonMessageDirection(WonMessageDirection.FROM_OWNER)
                        .setWonMessageType(WonMessageType.REPLACE).setSenderAtomURI(atomURI)
                        .setSenderNodeURI(wonNodeURI).setRecipientAtomURI(atomURI).setRecipientNodeURI(wonNodeURI)
                        .setTimestampToNow();
    }

    public static WonMessageBuilder setMessagePropertiesForHintToAtom(URI messageURI, URI atomURI, URI wonNodeURI,
                    URI otherAtomURI, URI matcherURI, double score) {
        WonMessageBuilder builder = new WonMessageBuilder(messageURI)
                        .setWonMessageDirection(WonMessageDirection.FROM_EXTERNAL)
                        .setWonMessageType(WonMessageType.ATOM_HINT_MESSAGE)
                        .setSenderNodeURI(matcherURI)
                        .setRecipientAtomURI(atomURI)
                        .setRecipientNodeURI(wonNodeURI)
                        .setHintTargetAtomURI(otherAtomURI)
                        .setHintScore(score);
        return builder.setTimestampToNow();
    }

    public static WonMessageBuilder setMessagePropertiesForHintToSocket(URI messageURI, URI recipientAtomURI,
                    URI recipientSocketURI,
                    URI wonNodeURI, URI targetSocketURI, URI matcherURI, double score) {
        WonMessageBuilder builder = new WonMessageBuilder(messageURI)
                        .setWonMessageDirection(WonMessageDirection.FROM_EXTERNAL)
                        .setWonMessageType(WonMessageType.SOCKET_HINT_MESSAGE)
                        .setSenderNodeURI(matcherURI)
                        .setRecipientAtomURI(recipientAtomURI)
                        .setRecipientSocketURI(recipientSocketURI)
                        .setRecipientNodeURI(wonNodeURI)
                        .setHintTargetSocketURI(targetSocketURI)
                        .setHintScore(score);
        return builder.setTimestampToNow();
    }

    public static WonMessageBuilder setMessagePropertiesForHintFeedback(URI messageURI, URI connectionURI, URI atomURI,
                    URI wonNodeURI, boolean booleanFeedbackValue) {
        Model contentModel = WonRdfUtils.MessageUtils.binaryFeedbackMessage(connectionURI, booleanFeedbackValue);
        Resource msgResource = contentModel.createResource(messageURI.toString());
        RdfUtils.replaceBaseResource(contentModel, msgResource);
        return new WonMessageBuilder(messageURI).setWonMessageDirection(WonMessageDirection.FROM_OWNER)
                        .setWonMessageType(WonMessageType.HINT_FEEDBACK_MESSAGE).setRecipientNodeURI(wonNodeURI)
                        .setRecipientURI(connectionURI).setRecipientAtomURI(atomURI).setSenderNodeURI(wonNodeURI)
                        .setSenderAtomURI(atomURI).setSenderURI(connectionURI).setTimestampToNow()
                        .addContent(contentModel);
    }

    @Deprecated
    public static WonMessageBuilder setMessagePropertiesForConnectionMessage(URI messageURI, URI localConnection,
                    URI localAtom, URI localWonNode, URI targetConnection, URI targetAtom, URI remoteWonNode,
                    Model content) {
        return new WonMessageBuilder(messageURI).setWonMessageDirection(WonMessageDirection.FROM_OWNER)
                        .setWonMessageType(WonMessageType.CONNECTION_MESSAGE).setSenderURI(localConnection)
                        .setSenderAtomURI(localAtom).setSenderNodeURI(localWonNode).setRecipientURI(targetConnection)
                        .setRecipientAtomURI(targetAtom).setRecipientNodeURI(remoteWonNode).addContent(content)
                        .setTimestampToNow();
    }

    public static WonMessageBuilder setMessagePropertiesForConnectionMessage(URI messageURI, URI senderSocket,
                    URI recpientSocket,
                    Model content) {
        return new WonMessageBuilder(messageURI).setWonMessageDirection(WonMessageDirection.FROM_OWNER)
                        .setWonMessageType(WonMessageType.CONNECTION_MESSAGE).setSenderSocketURI(senderSocket)
                        .setRecipientSocketURI(recpientSocket).addContent(content)
                        .setTimestampToNow();
    }

    public static WonMessageBuilder setMessagePropertiesForConnectionMessage(URI messageURI, URI senderSocket,
                    URI recpientSocket,
                    String textMessage) {
        return new WonMessageBuilder(messageURI).setWonMessageDirection(WonMessageDirection.FROM_OWNER)
                        .setWonMessageType(WonMessageType.CONNECTION_MESSAGE).setSenderSocketURI(senderSocket)
                        .setRecipientSocketURI(recpientSocket)
                        .addContent(WonRdfUtils.MessageUtils.textMessage(textMessage))
                        .setTimestampToNow();
    }

    public static WonMessageBuilder setMessagePropertiesForConnectionMessage(URI messageURI, URI localSocket,
                    URI localConnection,
                    URI localAtom, URI localWonNode, URI targetSocket, URI targetConnection, URI targetAtom,
                    URI remoteWonNode,
                    String textMessage) {
        return new WonMessageBuilder(messageURI)
                        .setWonMessageDirection(WonMessageDirection.FROM_OWNER)
                        .setWonMessageType(WonMessageType.CONNECTION_MESSAGE)
                        .setSenderSocketURI(localSocket)
                        .setSenderURI(localConnection)
                        .setSenderAtomURI(localAtom)
                        .setSenderNodeURI(localWonNode)
                        .setRecipientSocketURI(targetSocket)
                        .setRecipientURI(targetConnection)
                        .setRecipientAtomURI(targetAtom)
                        .setRecipientNodeURI(remoteWonNode)
                        .setTextMessage(textMessage)
                        .setTimestampToNow();
    }

    public static WonMessageBuilder setMessagePropertiesForAtomCreatedNotification(URI messageURI, URI localAtom,
                    URI localWonNode) {
        return new WonMessageBuilder(messageURI).setWonMessageType(WonMessageType.ATOM_CREATED_NOTIFICATION)
                        .setWonMessageDirection(WonMessageDirection.FROM_EXTERNAL).setSenderAtomURI(localAtom)
                        .setSenderNodeURI(localWonNode).setTimestampToNow();
    }

    public static WonMessageBuilder setPropertiesForPassingMessageToRemoteNode(final WonMessage ownerOrSystemMsg,
                    URI newMessageUri) {
        return setPropertiesForPassingMessageToRemoteNodeAndCopyLocalMessage(ownerOrSystemMsg, newMessageUri);
    }

    /**
     * Adds the complete message content to the message that will be built,
     * referencing toForward's envelope in the envelope of the new message.
     *
     * @param
     * @return
     */
    private WonMessageBuilder forward(WonMessage toForward) {
        // make a copy to avoid modification in current message in case wrapped message
        // is modified externally
        this.forwardedMessage = WonRdfUtils.MessageUtils.copyByDatasetSerialization(toForward);
        return this;
    }

    /**
     * The message to remote node will contain envelope with sentTimestamp,
     * remoteMessageUri, direction, as well as exact copy of the local message
     * envelopes and contents.
     *
     * @param ownerOrSystemMsg
     * @param newMessageUri
     * @return
     */
    private static WonMessageBuilder setPropertiesForPassingMessageToRemoteNodeAndCopyLocalMessage(
                    final WonMessage ownerOrSystemMsg, URI newMessageUri) {
        return new WonMessageBuilder(newMessageUri).setTimestamp(new Date().getTime()).forward(ownerOrSystemMsg) // copy
                        .setCorrespondingRemoteMessageURI(ownerOrSystemMsg.getMessageURI())
                        .setWonMessageDirection(WonMessageDirection.FROM_EXTERNAL);
    }

    /**
     * Creates a node response
     * 
     * @param originalMessage
     * @param isSuccess
     * @param messageURI
     * @param backupOriginalSender use that if we cannot determine it from the
     * original message
     * @param backupOriginalRecipient use that if we cannot determine it from the
     * original message
     * @return
     */
    public static WonMessageBuilder setPropertiesForNodeResponse(WonMessage originalMessage, boolean isSuccess,
                    URI messageURI, Optional<URI> parent, WonMessageDirection direction) {
        WonMessageBuilder messageBuilder = new WonMessageBuilder(messageURI).setWonMessageType(
                        isSuccess ? WonMessageType.SUCCESS_RESPONSE : WonMessageType.FAILURE_RESPONSE);
        Optional<URI> senderAtomURI = Optional.ofNullable(originalMessage.getSenderAtomURI());
        Optional<URI> recipientAtomURI = Optional.ofNullable(originalMessage.getRecipientAtomURI());
        Optional<URI> senderSocketURI = Optional.ofNullable(originalMessage.getSenderSocketURI());
        Optional<URI> recipientSocketURI = Optional.ofNullable(originalMessage.getRecipientSocketURI());
        if (WonMessageDirection.FROM_EXTERNAL == direction) {
            // if the message is an external message, the original receiver becomes
            // the sender of the response.
            messageBuilder.setSenderNodeURI(originalMessage.getRecipientNodeURI());
            recipientAtomURI.ifPresent(messageBuilder::setSenderAtomURI);
            recipientSocketURI.ifPresent(messageBuilder::setSenderSocketURI);
        } else if (WonMessageDirection.FROM_OWNER == direction
                        || WonMessageDirection.FROM_SYSTEM == direction) {
            // if the message comes from the owner, the original sender is also
            // the sender of the response
            messageBuilder.setSenderNodeURI(originalMessage.getSenderNodeURI());
            senderAtomURI.ifPresent(messageBuilder::setSenderAtomURI);
            senderSocketURI.ifPresent(messageBuilder::setSenderSocketURI);
        }
        messageBuilder
                        .setRecipientNodeURI(originalMessage.getSenderNodeURI())
                        .setIsResponseToMessageURI(originalMessage.getMessageURIRequired())
                        .setIsResponseToMessageType(originalMessage.getMessageTypeRequired())
                        .setWonMessageDirection(WonMessageDirection.FROM_SYSTEM);
        parent.ifPresent(messageBuilder::setSenderURI);
        senderAtomURI.ifPresent(messageBuilder::setRecipientAtomURI);
        senderSocketURI.ifPresent(messageBuilder::setRecipientSocketURI);
        messageBuilder.setTimestampToNow();
        return messageBuilder;
    }

    public WonMessageBuilder setSenderURI(URI senderURI) {
        Objects.requireNonNull(senderURI);
        this.senderURI = senderURI;
        return this;
    }

    public WonMessageBuilder setSenderAtomURI(URI senderAtomURI) {
        Objects.requireNonNull(senderAtomURI);
        this.senderAtomURI = senderAtomURI;
        return this;
    }

    public WonMessageBuilder setSenderNodeURI(URI senderNodeURI) {
        Objects.requireNonNull(senderNodeURI);
        this.senderNodeURI = senderNodeURI;
        return this;
    }

    public WonMessageBuilder setSenderSocketURI(URI senderSocketURI) {
        Objects.requireNonNull(senderSocketURI);
        this.senderSocketURI = senderSocketURI;
        return this;
    }

    public WonMessageBuilder setRecipientURI(URI recipientURI) {
        Objects.requireNonNull(recipientURI);
        this.recipientURI = recipientURI;
        return this;
    }

    public WonMessageBuilder setRecipientAtomURI(URI recipientAtomURI) {
        Objects.requireNonNull(recipientAtomURI);
        this.recipientAtomURI = recipientAtomURI;
        return this;
    }

    public WonMessageBuilder setRecipientNodeURI(URI recipientNodeURI) {
        Objects.requireNonNull(recipientNodeURI);
        this.recipientNodeURI = recipientNodeURI;
        return this;
    }

    public WonMessageBuilder setRecipientSocketURI(URI recipientSocketURI) {
        Objects.requireNonNull(recipientSocketURI);
        this.recipientSocketURI = recipientSocketURI;
        return this;
    }

    public WonMessageBuilder setWonMessageType(WonMessageType wonMessageType) {
        Objects.requireNonNull(wonMessageType);
        this.wonMessageType = wonMessageType;
        return this;
    }

    public WonMessageBuilder setWonMessageDirection(WonMessageDirection wonMessageDirection) {
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
    public WonMessageBuilder addContent(Model content) {
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
    public WonMessageBuilder addContent(Dataset dataset) {
        Objects.requireNonNull(dataset);
        Dataset toAdd = RdfUtils.cloneDataset(dataset);
        // we can add the default model without remembering the newly
        // generated URI because there cannot be a reference
        // to the default model in the other graphs
        Model model = toAdd.getDefaultModel();
        if (model != null && model.size() > 0) {
            addContent(model);
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
    public Model getUnsignedContentGraph() {
        if (contentMap.isEmpty()) {
            // no content graphs yet. Make one and return it.
            Model contentGraph = ModelFactory.createDefaultModel();
            RdfUtils.replaceBaseURI(contentGraph, this.messageURI.toString());
            addContent(contentGraph);
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
        addContent(contentGraph);
        return contentGraph;
    }

    public WonMessageBuilder setIsResponseToMessageURI(URI isResponseToMessageURI) {
        Objects.requireNonNull(isResponseToMessageURI);
        this.isResponseToMessageURI = isResponseToMessageURI;
        return this;
    }

    public WonMessageBuilder setIsResponseToMessageType(final WonMessageType isResponseToMessageType) {
        Objects.requireNonNull(isResponseToMessageType);
        this.isResponseToMessageType = isResponseToMessageType;
        return this;
    }

    public WonMessageBuilder setCorrespondingRemoteMessageURI(URI correspondingRemoteMessageURI) {
        Objects.requireNonNull(correspondingRemoteMessageURI);
        this.correspondingRemoteMessageURI = correspondingRemoteMessageURI;
        return this;
    }

    public WonMessageBuilder setForwardedMessageURI(URI forwardedMessageURI) {
        Objects.requireNonNull(forwardedMessageURI);
        this.forwardedMessageURI = forwardedMessageURI;
        return this;
    }

    public WonMessageBuilder setInjectIntoConnections(Collection<URI> forwardToRecipientUris) {
        Objects.requireNonNull(forwardToRecipientUris);
        this.injectIntoConnections.addAll(forwardToRecipientUris);
        return this;
    }

    public WonMessageBuilder setTimestamp(Long timestamp) {
        Objects.requireNonNull(timestamp);
        this.timestamp = timestamp;
        return this;
    }

    public WonMessageBuilder setTimestampToNow() {
        this.timestamp = System.currentTimeMillis();
        return this;
    }

    public WonMessageBuilder setHintScore(Double hintScore) {
        Objects.requireNonNull(hintScore);
        this.hintScore = hintScore;
        return this;
    }

    public WonMessageBuilder setHintTargetAtomURI(URI hintTargetAtomURI) {
        Objects.requireNonNull(hintTargetAtomURI);
        this.hintTargetAtomURI = hintTargetAtomURI;
        return this;
    }

    public WonMessageBuilder setHintTargetSocketURI(URI hintTargetSocketURI) {
        Objects.requireNonNull(hintTargetSocketURI);
        this.hintTargetSocketURI = hintTargetSocketURI;
        return this;
    }

    /**
     * Adds a con:text triple to one of the unsigned content graphs in this builder.
     * Creates a new unsigned content graph if none is found.
     * 
     * @param textMessage may be null in which case the builder is not modified
     * @return
     */
    public WonMessageBuilder setTextMessage(String textMessage) {
        Objects.requireNonNull(textMessage);
        WonRdfUtils.MessageUtils.addMessage(getUnsignedContentGraph(), textMessage);
        return this;
    }

    /**
     * Copies the envelope properties from the specified message to this message.
     * Note that this does not copy the original envelope graph, only the standard
     * envelope properties.
     *
     * @param wonMessage
     * @return
     */
    public static WonMessageBuilder copyEnvelopeFromWonMessage(final WonMessage wonMessage) {
        WonMessageBuilder builder = new WonMessageBuilder(wonMessage.getMessageURI())
                        .setWonMessageType(wonMessage.getMessageType()).setRecipientURI(wonMessage.getRecipientURI())
                        .setRecipientAtomURI(wonMessage.getRecipientAtomURI())
                        .setRecipientNodeURI(wonMessage.getRecipientNodeURI()).setSenderURI(wonMessage.getSenderURI())
                        .setSenderAtomURI(wonMessage.getSenderAtomURI())
                        .setSenderNodeURI(wonMessage.getSenderNodeURI());
        if (wonMessage.getRespondingToMessageType() != null) {
            builder.setIsResponseToMessageType(wonMessage.getRespondingToMessageType());
        }
        if (wonMessage.getRespondingToMessageURI() != null) {
            builder.setIsResponseToMessageURI(wonMessage.getRespondingToMessageURI());
        }
        return builder;
    }

    public static WonMessage forwardReceivedNodeToNodeMessageAsNodeToNodeMessage(final URI newMessageUri,
                    final WonMessage wonMessage, final URI connectionURI, final URI atomURI, final URI wonNodeUri,
                    final URI targetConnectionURI, final URI targetAtomURI, final URI remoteWonNodeUri) {
        WonMessageBuilder builder = new WonMessageBuilder(newMessageUri).setWonMessageType(wonMessage.getMessageType())
                        .forward(wonMessage).setForwardedMessageURI(wonMessage.getMessageURI())
                        .setSenderAtomURI(atomURI).setSenderURI(connectionURI).setSenderNodeURI(wonNodeUri)
                        .setTimestamp(System.currentTimeMillis()).setRecipientURI(targetConnectionURI)
                        .setRecipientAtomURI(targetAtomURI).setRecipientNodeURI(remoteWonNodeUri)
                        .setIsResponseToMessageURI(wonMessage.getRespondingToMessageURI())
                        .setIsResponseToMessageType(wonMessage.getRespondingToMessageType())
                        .setWonMessageDirection(WonMessageDirection.FROM_SYSTEM);
        return builder.build();
    }

    public static Make make(URI messageURI) {
        WonMessageBuilder builder = new WonMessageBuilder(messageURI);
        return new Make(builder);
    }

    public static class BuilderBase {
        protected WonMessageBuilder builder;

        public BuilderBase(WonMessageBuilder builder) {
            this.builder = builder;
        }
    }

    public static class Make extends BuilderBase {
        private Make(WonMessageBuilder builder) {
            super(builder);
        }

        public ConnectSenderSocket connect() {
            return new ConnectSenderSocket(builder);
        }
    }

    public static class ConnectSenderSocket extends BuilderBase {
        public ConnectSenderSocket(WonMessageBuilder builder) {
            super(builder);
            builder
                            .setTimestampToNow()
                            .setWonMessageDirection(WonMessageDirection.FROM_OWNER)
                            .setWonMessageType(WonMessageType.CONNECT);
        }

        public ConnectSenderAtom senderSocket(URI socketURI) {
            builder.setSenderSocketURI(socketURI);
            return new ConnectSenderAtom(builder);
        }
    }

    public static class ConnectSenderAtom extends BuilderBase {
        public ConnectSenderAtom(WonMessageBuilder builder) {
            super(builder);
        }

        public ConnectSenderNode senderAtom(URI atomURI) {
            builder.setSenderAtomURI(atomURI);
            return new ConnectSenderNode(builder);
        }
    }

    public static class ConnectSenderNode extends BuilderBase {
        public ConnectSenderNode(WonMessageBuilder builder) {
            super(builder);
        }

        public ConnectRecipientSocket senderNode(URI nodeURI) {
            builder.setSenderNodeURI(nodeURI);
            return new ConnectRecipientSocket(builder);
        }
    }

    public static class ConnectRecipientSocket extends BuilderBase {
        public ConnectRecipientSocket(WonMessageBuilder builder) {
            super(builder);
        }

        public ConnectRecipientAtom recipientSocket(URI atomURI) {
            builder.setRecipientSocketURI(atomURI);
            return new ConnectRecipientAtom(builder);
        }
    }

    public static class ConnectRecipientAtom extends BuilderBase {
        public ConnectRecipientAtom(WonMessageBuilder builder) {
            super(builder);
        }

        public ConnectRecipientNode recipientAtom(URI atomURI) {
            builder.setRecipientAtomURI(atomURI);
            return new ConnectRecipientNode(builder);
        }
    }

    public static class ConnectRecipientNode extends BuilderBase {
        public ConnectRecipientNode(WonMessageBuilder builder) {
            super(builder);
        }

        public ConnectWelcomeMessage recipientNode(URI atomURI) {
            builder.setRecipientNodeURI(atomURI);
            return new ConnectWelcomeMessage(builder);
        }
    }

    public static class ConnectWelcomeMessage extends BuilderBase {
        public ConnectWelcomeMessage(WonMessageBuilder builder) {
            super(builder);
        }

        public WonMessageBuilder welcomeMessage(String welcomeMessage) {
            Objects.requireNonNull(welcomeMessage);
            Model model = ModelFactory.createDefaultModel();
            RdfUtils.findOrCreateBaseResource(model);
            RdfUtils.replaceBaseResource(model, model.createResource(builder.getMessageURI().toString()));
            WonRdfUtils.MessageUtils.addMessage(model, welcomeMessage);
            builder.addContent(model);
            return builder;
        }

        public WonMessageBuilder noMessage() {
            return builder;
        }
    }

    private URI getMessageURI() {
        return messageURI;
    }
}

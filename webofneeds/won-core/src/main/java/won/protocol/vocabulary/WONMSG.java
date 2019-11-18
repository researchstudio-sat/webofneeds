package won.protocol.vocabulary;

import java.net.URI;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

/**
 * User: ypanchenko Date: 04.08.2014
 */
public class WONMSG {
    // TODO check with existing code how they do it, do they have ontology objects
    // and
    // access the vocabulary from there? If yes, change to that all the enum classes
    /**
     * prefix of message URIs. Message IDs are to be added after adding 1 slash.
     * e.g. wm:/1234
     */
    public static final String MESSAGE_URI_PREFIX = "wm:/";
    /**
     * URI used as message URI when creating a message, before its permanent URI is
     * calculated. Likewise, this URI is used for verifying message
     * signatures/contents: before verification, its permanent message URI is
     * replaced with this one.
     */
    public static final String MESSAGE_SELF = "wm:/SELF";
    public static final String BASE_URI = "https://w3id.org/won/message#";
    public static final String DEFAULT_PREFIX = "msg";
    private static Model m = ModelFactory.createDefaultModel();
    public static final String protocolVersionString = BASE_URI + "protocolVersion";
    public static final Property protocolVersion = m.createProperty(protocolVersionString);
    public static final String FromOwnerString = BASE_URI + "FromOwner";
    public static final String FromSystemString = BASE_URI + "FromSystem";
    public static final String FromExternalString = BASE_URI + "FromExternal";
    public static final Resource FromOwner = m.createResource(BASE_URI + "FromOwner");
    public static final Resource FromSystem = m.createResource(BASE_URI + "FromSystem");
    public static final Resource FromExternal = m.createResource(BASE_URI + "FromExternal");
    public static final String CreateMessageString = BASE_URI + "CreateMessage";
    public static final String ReplaceMessageString = BASE_URI + "ReplaceMessage";
    public static final String ChangeNotificationMessageString = BASE_URI + "ChangeNotificationMessage";
    public static final String ConnectMessageString = BASE_URI + "ConnectMessage";
    public static final String DeactivateMessageString = BASE_URI + "DeactivateMessage";
    public static final String ActivateMessageString = BASE_URI + "ActivateMessage";
    public static final String CloseMessageString = BASE_URI + "CloseMessage";
    public static final String DeleteMessageString = BASE_URI + "DeleteMessage";
    public static final String ConnectionMessageString = BASE_URI + "ConnectionMessage";
    public static final String AtomMessageString = BASE_URI + "AtomMessage";
    public static final String SocketHintMessageString = BASE_URI + "SocketHintMessage";
    public static final String AtomHintMessageString = BASE_URI + "AtomHintMessage";
    public static final String HintFeedbackMessageString = BASE_URI + "HintFeedbackMessage";
    public static final String FailureResponseString = BASE_URI + "FailureResponse";
    public static final String SuccessResponseString = BASE_URI + "SuccessResponse";
    // main types
    public static final Resource CreateMessage = m.createResource(CreateMessageString);
    public static final Resource ReplaceMessage = m.createResource(ReplaceMessageString);
    public static final Resource ChangeNotificationMessage = m.createResource(ChangeNotificationMessageString);
    public static final Resource ConnectMessage = m.createResource(ConnectMessageString);
    public static final Resource DeactivateMessage = m.createResource(DeactivateMessageString);
    public static final Resource ActivateMessage = m.createResource(ActivateMessageString);
    public static final Resource CloseMessage = m.createResource(CloseMessageString);
    public static final Resource DeleteMessage = m.createResource(DeleteMessageString);
    public static final Resource ConnectionMessage = m.createResource(ConnectionMessageString);
    public static final Resource AtomMessage = m.createResource(AtomMessageString);
    public static final Resource SocketHintMessage = m.createResource(SocketHintMessageString);
    public static final Resource AtomHintMessage = m.createResource(AtomHintMessageString);
    public static final Resource HintFeedbackMessage = m.createResource(HintFeedbackMessageString);
    public static final Resource SuccessResponse = m.createResource(SuccessResponseString);
    public static final Resource FailureResponse = m.createResource(FailureResponseString);
    // notification types
    // TODO: delete if not needed
    public static final Resource HintNotificationMessage = m.createResource(BASE_URI + "HintNotificationMessage");
    // TODO: delete if not needed
    public static final Resource AtomCreatedNotificationMessage = m
                    .createResource(BASE_URI + "AtomCreatedNotificationMessage");
    // response states
    public static final Resource SuccessResponseState = m.createResource(BASE_URI + "SuccessResponse");
    public static final Resource FailureResponseState = m.createResource(BASE_URI + "FailureResponse");
    // TODO: delete if not needed
    public static final Resource DuplicateAtomIdResponseState = m.createResource(BASE_URI + "DuplicateAtomIdResponse");
    // TODO: delete if not needed
    public static final Resource DuplicateConnectionIdResponseState = m
                    .createResource(BASE_URI + "DuplicateConnectionIdResponse");
    // TODO: delete if not needed
    public static final Resource DuplicateMessageIdResponseState = m
                    .createResource(BASE_URI + "DuplicateMessageIdResponse");
    // TODO: delete if not needed
    public static final Property responseStateProperty = m.createProperty(BASE_URI + "responseStateProperty"); // TODO
                                                                                                               // rename!
    // public static final String CreateMessage = BASE_URI +
    // "CreateMessage";
    // public static final String ConnectMessage = BASE_URI +
    // "ConnectMessage";
    // public static final String AtomStateMessage = BASE_URI +
    // "AtomStateMessage";
    public static final Resource EnvelopeGraph = m.createResource(BASE_URI + "EnvelopeGraph");
    public static final Resource ForwardedEnvelopeGraph = m.createResource(BASE_URI + "ForwardedEnvelopeGraph");
    // used to wrap an envelope inside another for forwarding and adding the
    // server-side envelope to a
    // client-generated message
    public static final Property messageType = m.createProperty(BASE_URI, "messageType");
    public static final Property content = m.createProperty(BASE_URI, "content");
    public static final Property respondingTo = m.createProperty(BASE_URI, "respondingTo");
    public static final Property respondingToMessageType = m.createProperty(BASE_URI, "respondingToMessageType");;
    public static final Property forwardedMessage = m.createProperty(BASE_URI, "forwardedMessage");
    public static final Property injectIntoConnection = m.createProperty(BASE_URI, "injectIntoConnection");
    public static final Property previousMessage = m.createProperty(BASE_URI + "previousMessage");
    public static final Property timestamp = m.createProperty(BASE_URI, "timestamp");
    // added to support referencing signatures from the envelope
    public static final Property containsSignature = m.createProperty(BASE_URI, "containsSignature");
    // TODO maybe the three properties below could better belong to a separate
    // ontology
    public static final Property signedGraph = m.createProperty(BASE_URI, "signedGraph");
    public static final Property signatureValue = m.createProperty(BASE_URI, "signatureValue");
    public static final Property hash = m.createProperty(BASE_URI, "hash");
    public static final Property signer = m.createProperty(BASE_URI, "signer");
    public static final Property Signature = m.createProperty(BASE_URI, "Signature");
    public static final Property signatureGraph = m.createProperty(BASE_URI, "signatureGraph");
    public static final Property publicKeyFingerprint = m.createProperty(BASE_URI, "publicKeyFingerprint");
    public static final Property recipientSocket = m.createProperty(BASE_URI, "recipientSocket");
    public static final Property senderSocket = m.createProperty(BASE_URI, "senderSocket");
    public static final Property contentType = m.createProperty(BASE_URI, "contentType");
    public static final Property connection = m.createProperty(BASE_URI, "connection");
    public static final Property atom = m.createProperty(BASE_URI, "atom");
    public static final Property hintScore = m.createProperty(BASE_URI, "hintScore");
    public static final Property hintTargetAtom = m.createProperty(BASE_URI, "hintTargetAtom");
    public static final Property hintTargetSocket = m.createProperty(BASE_URI, "hintTargetSocket");
    public static final String GRAPH_URI_FRAGMENT = "data";

    /**
     * Returns the base URI for this schema.
     *
     * @return the URI for this schema
     */
    public static String getURI() {
        return BASE_URI;
    }

    public static Resource toResource(URI uri) {
        return m.getResource(uri.toString());
    }
}

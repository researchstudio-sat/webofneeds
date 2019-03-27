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
    public static final String BASE_URI = "http://purl.org/webofneeds/message#";
    public static final String DEFAULT_PREFIX = "msg";;
    private static Model m = ModelFactory.createDefaultModel();
    public static final String PROTOCOL_VERSION_STRING = BASE_URI + "protocolVersion";
    public static final Property PROTOCOL_VERSION = m.createProperty(PROTOCOL_VERSION_STRING);
    public static final String TYPE_FROM_OWNER_STRING = BASE_URI + "FromOwner";
    public static final String TYPE_FROM_SYSTEM_STRING = BASE_URI + "FromSystem";
    public static final String TYPE_FROM_EXTERNAL_STRING = BASE_URI + "FromExternal";
    public static final Resource TYPE_FROM_OWNER = m.createResource(BASE_URI + "FromOwner");
    public static final Resource TYPE_FROM_SYSTEM = m.createResource(BASE_URI + "FromSystem");
    public static final Resource TYPE_FROM_EXTERNAL = m.createResource(BASE_URI + "FromExternal");
    public static final String TYPE_CREATE_STRING = BASE_URI + "CreateMessage";
    public static final String TYPE_CONNECT_STRING = BASE_URI + "ConnectMessage";
    public static final String TYPE_DEACTIVATE_STRING = BASE_URI + "DeactivateMessage";
    public static final String TYPE_ACTIVATE_STRING = BASE_URI + "ActivateMessage";
    public static final String TYPE_OPEN_STRING = BASE_URI + "OpenMessage";
    public static final String TYPE_CLOSE_STRING = BASE_URI + "CloseMessage";
    public static final String TYPE_DELETE_STRING = BASE_URI + "DeleteMessage";
    public static final String TYPE_CONNECTION_MESSAGE_STRING = BASE_URI + "ConnectionMessage";
    public static final String TYPE_NEED_MESSAGE_STRING = BASE_URI + "NeedMessage";
    public static final String TYPE_HINT_STRING = BASE_URI + "HintMessage";
    public static final String TYPE_HINT_FEEDBACK_STRING = BASE_URI + "HintFeedbackMessage";
    public static final String TYPE_FAILURE_RESPONSE_STRING = BASE_URI + "FailureResponse";
    public static final String TYPE_SUCCESS_RESPONSE_STRING = BASE_URI + "SuccessResponse";
    // main types
    public static final Resource TYPE_CREATE = m.createResource(TYPE_CREATE_STRING);
    public static final Resource TYPE_CONNECT = m.createResource(TYPE_CONNECT_STRING);
    public static final Resource TYPE_DEACTIVATE = m.createResource(TYPE_DEACTIVATE_STRING);
    public static final Resource TYPE_ACTIVATE = m.createResource(TYPE_ACTIVATE_STRING);
    public static final Resource TYPE_OPEN = m.createResource(TYPE_OPEN_STRING);
    public static final Resource TYPE_CLOSE = m.createResource(TYPE_CLOSE_STRING);
    public static final Resource TYPE_DELETE = m.createResource(TYPE_DELETE_STRING);
    public static final Resource TYPE_CONNECTION_MESSAGE = m.createResource(TYPE_CONNECTION_MESSAGE_STRING);
    public static final Resource TYPE_NEED_MESSAGE = m.createResource(TYPE_NEED_MESSAGE_STRING);
    public static final Resource TYPE_HINT = m.createResource(TYPE_HINT_STRING);
    public static final Resource TYPE_HINT_FEEDBACK = m.createResource(TYPE_HINT_FEEDBACK_STRING);
    public static final Resource TYPE_SUCCESS_RESPONSE = m.createResource(TYPE_SUCCESS_RESPONSE_STRING);
    public static final Resource TYPE_FAILURE_RESPONSE = m.createResource(TYPE_FAILURE_RESPONSE_STRING);
    // notification types
    // TODO: delete if not needed
    public static final Resource TYPE_HINT_NOTIFICATION = m.createResource(BASE_URI + "HintNotificationMessage");
    // TODO: delete if not needed
    public static final Resource TYPE_NEED_CREATED_NOTIFICATION = m
                    .createResource(BASE_URI + "NeedCreatedNotificationMessage");
    // response types
    // TODO: delete if not needed
    public static final Resource TYPE_CREATE_RESPONSE = m.createResource(BASE_URI + "CreateResponseMessage");
    // TODO: delete if not needed
    public static final Resource TYPE_CONNECT_RESPONSE = m.createResource(BASE_URI + "ConnectResponseMessage");
    // TODO: delete if not needed
    public static final Resource TYPE_NEED_STATE_RESPONSE = m.createResource(BASE_URI + "NeedStateResponseMessage");
    // TODO: delete if not needed
    public static final Resource TYPE_CLOSE_RESPONSE = m.createResource(BASE_URI + "CloseResponseMessage");
    // TODO: delete if not needed
    public static final Resource TYPE_OPEN_RESPONSE = m.createResource(BASE_URI + "OpenResponseMessage");
    // TODO: delete if not needed
    public static final Resource TYPE_CONNECTION_MESSAGE_RESPONSE = m
                    .createResource(BASE_URI + "ConnectionMessageResponseMessage");

    // TODO: delete if not needed
    public static boolean isResponseMessageType(Resource resource) {
        if (resource.equals(TYPE_CREATE_RESPONSE) || resource.equals(TYPE_CONNECT_RESPONSE)
                        || resource.equals(TYPE_NEED_STATE_RESPONSE) || resource.equals(TYPE_CLOSE_RESPONSE)
                        || resource.equals(TYPE_OPEN_RESPONSE) || resource.equals(TYPE_CONNECTION_MESSAGE_RESPONSE))
            return true;
        else
            return false;
    }

    // response states
    public static final Resource TYPE_RESPONSE_STATE_SUCCESS = m.createResource(BASE_URI + "SuccessResponse");
    public static final Resource TYPE_RESPONSE_STATE_FAILURE = m.createResource(BASE_URI + "FailureResponse");
    // TODO: delete if not needed
    public static final Resource TYPE_RESPONSE_STATE_DUPLICATE_NEED_ID = m
                    .createResource(BASE_URI + "DuplicateNeedIdResponse");
    // TODO: delete if not needed
    public static final Resource TYPE_RESPONSE_STATE_DUPLICATE_CONNECTION_ID = m
                    .createResource(BASE_URI + "DuplicateConnectionIdResponse");
    // TODO: delete if not needed
    public static final Resource TYPE_RESPONSE_STATE_DUPLICATE_MESSAGE_ID = m
                    .createResource(BASE_URI + "DuplicateMessageIdResponse");
    // TODO: delete if not needed
    public static final Property HAS_RESPONSE_STATE_PROPERTY = m.createProperty(BASE_URI + "hasResponseStateProperty"); // TODO
                                                                                                                        // rename!
    // public static final String MESSAGE_TYPE_CREATE_RESOURCE = BASE_URI +
    // "CreateMessage";
    // public static final String MESSAGE_TYPE_CONNECT_RESOURCE = BASE_URI +
    // "ConnectMessage";
    // public static final String MESSAGE_TYPE_NEED_STATE_RESOURCE = BASE_URI +
    // "NeedStateMessage";
    public static final Resource ENVELOPE_GRAPH = m.createResource(BASE_URI + "EnvelopeGraph");
    public static final Resource FORWARDED_ENVELOPE_GRAPH = m.createResource(BASE_URI + "ForwardedEnvelopeGraph");
    // used to wrap an envelope inside another for forwarding and adding the
    // server-side envelope to a
    // client-generated message
    public static final Property CONTAINS_ENVELOPE = m.createProperty(BASE_URI, "containsEnvelope");
    public static final Property RECEIVER_PROPERTY = m.createProperty(BASE_URI, "hasReceiver");
    public static final Property RECEIVER_NEED_PROPERTY = m.createProperty(BASE_URI, "hasReceiverNeed");
    public static final Property RECEIVER_NODE_PROPERTY = m.createProperty(BASE_URI, "hasReceiverNode");
    public static final Property SENDER_PROPERTY = m.createProperty(BASE_URI, "hasSender");
    public static final Property SENDER_NEED_PROPERTY = m.createProperty(BASE_URI, "hasSenderNeed");
    public static final Property SENDER_NODE_PROPERTY = m.createProperty(BASE_URI, "hasSenderNode");
    public static final Property HAS_MESSAGE_TYPE_PROPERTY = m.createProperty(BASE_URI, "hasMessageType");
    public static final Property HAS_CONTENT_PROPERTY = m.createProperty(BASE_URI, "hasContent");
    public static final Property REFERS_TO_PROPERTY = m.createProperty(BASE_URI, "refersTo");
    public static final Property IS_RESPONSE_TO = m.createProperty(BASE_URI, "isResponseTo");
    public static final Property IS_REMOTE_RESPONSE_TO = m.createProperty(BASE_URI, "isRemoteResponseTo");
    public static final Property IS_RESPONSE_TO_MESSAGE_TYPE = m.createProperty(BASE_URI, "isResponseToMessageType");;
    public static final Property HAS_CORRESPONDING_REMOTE_MESSAGE = m.createProperty(BASE_URI,
                    "hasCorrespondingRemoteMessage");
    public static final Property HAS_FORWARDED_MESSAGE = m.createProperty(BASE_URI, "hasForwardedMessage");
    public static final Property HAS_INJECT_INTO_CONNECTION = m.createProperty(BASE_URI, "hasInjectIntoConnection");
    public static final Property HAS_PREVIOUS_MESSAGE_PROPERTY = m.createProperty(BASE_URI + "hasPreviousMessage");
    public static final Property NEW_NEED_STATE_PROPERTY = m.createProperty(BASE_URI, "newNeedState");
    public static final Property HAS_SENT_TIMESTAMP = m.createProperty(BASE_URI, "hasSentTimestamp");
    public static final Property HAS_RECEIVED_TIMESTAMP = m.createProperty(BASE_URI, "hasReceivedTimestamp");
    // public static final String MESSAGE_HAS_CONTENT_PROPERTY = "hasContent";
    // public static final String MESSAGE_REFERS_TO_PROPERTY = "refersTo";
    // public static final String MESSAGE_NEW_NEED_STATE_PROPERTY = "newNeedState";
    // added to support referencing signatures from the envelope
    public static final Property CONTAINS_SIGNATURE_PROPERTY = m.createProperty(BASE_URI, "containsSignature");
    // TODO maybe the three properties below could better belong to a separate
    // ontology
    public static final Property HAS_SIGNED_GRAPH_PROPERTY = m.createProperty(BASE_URI, "hasSignedGraph");
    public static final Property HAS_SIGNATURE_VALUE_PROPERTY = m.createProperty(BASE_URI, "hasSignatureValue");
    public static final Property HAS_HASH_PROPERTY = m.createProperty(BASE_URI, "hasHash");
    public static final Property HAS_SIGNATURE_GRAPH_PROPERTY = m.createProperty(BASE_URI, "hasSignatureGraph");
    public static final Property HAS_PUBLIC_KEY_FINGERPRINT_PROPERTY = m.createProperty(BASE_URI,
                    "hasPublicKeyFingerprint");
    public static final Property HAS_RECEIVER_FACET = m.createProperty(BASE_URI, "hasReceiverFacet");
    public static final Property HAS_SENDER_FACET = m.createProperty(BASE_URI, "hasSenderFacet");
    public static final Property CONTENT_TYPE = m.createProperty(BASE_URI, "contentType");
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

package won.protocol.message;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;

import won.protocol.vocabulary.WONMSG;

/**
 * User: ypanchenko Date: 13.08.2014
 */
public enum WonMessageType {
    // main messages
    CREATE_ATOM(WONMSG.CreateMessage, null,
                    RDF.type,
                    WONMSG.messageType,
                    WONMSG.timestamp,
                    WONMSG.atom,
                    WONMSG.content,
                    WONMSG.protocolVersion),
    REPLACE(WONMSG.ReplaceMessage, null,
                    RDF.type,
                    WONMSG.messageType,
                    WONMSG.timestamp,
                    WONMSG.atom,
                    WONMSG.content,
                    WONMSG.protocolVersion),
    CONNECT(WONMSG.ConnectMessage, new Property[] { WONMSG.content },
                    RDF.type,
                    WONMSG.messageType,
                    WONMSG.timestamp,
                    WONMSG.senderSocket,
                    WONMSG.recipientSocket,
                    WONMSG.protocolVersion),
    DEACTIVATE(WONMSG.DeactivateMessage, null,
                    RDF.type,
                    WONMSG.messageType,
                    WONMSG.timestamp,
                    WONMSG.atom,
                    WONMSG.protocolVersion),
    ACTIVATE(WONMSG.ActivateMessage, null,
                    WONMSG.protocolVersion,
                    RDF.type,
                    WONMSG.messageType,
                    WONMSG.timestamp,
                    WONMSG.atom),
    CLOSE(WONMSG.CloseMessage, new Property[] { WONMSG.content },
                    WONMSG.protocolVersion,
                    RDF.type,
                    WONMSG.messageType,
                    WONMSG.timestamp,
                    WONMSG.senderSocket,
                    WONMSG.recipientSocket),
    DELETE(WONMSG.DeleteMessage, null,
                    WONMSG.protocolVersion,
                    RDF.type,
                    WONMSG.messageType,
                    WONMSG.timestamp,
                    WONMSG.atom),
    CONNECTION_MESSAGE(WONMSG.ConnectionMessage, new Property[] { WONMSG.content, WONMSG.forwardedMessage },
                    WONMSG.protocolVersion,
                    RDF.type,
                    WONMSG.messageType,
                    WONMSG.timestamp,
                    WONMSG.senderSocket,
                    WONMSG.recipientSocket),
    ATOM_HINT_MESSAGE(WONMSG.AtomHintMessage, null,
                    RDF.type,
                    WONMSG.messageType,
                    WONMSG.timestamp,
                    WONMSG.atom,
                    WONMSG.hintTargetAtom,
                    WONMSG.hintScore,
                    WONMSG.protocolVersion),
    SOCKET_HINT_MESSAGE(WONMSG.SocketHintMessage, null,
                    WONMSG.protocolVersion,
                    RDF.type,
                    WONMSG.messageType,
                    WONMSG.timestamp,
                    WONMSG.recipientSocket,
                    WONMSG.hintTargetSocket,
                    WONMSG.hintScore),
    HINT_FEEDBACK_MESSAGE(WONMSG.HintFeedbackMessage, null,
                    WONMSG.protocolVersion,
                    RDF.type,
                    WONMSG.messageType,
                    WONMSG.timestamp,
                    WONMSG.connection,
                    WONMSG.content),
    // response messages
    SUCCESS_RESPONSE(WONMSG.SuccessResponse,
                    new Property[] {
                                    WONMSG.atom,
                                    WONMSG.connection,
                                    WONMSG.senderSocket,
                                    WONMSG.recipientSocket
                    },
                    WONMSG.protocolVersion,
                    RDF.type,
                    WONMSG.messageType,
                    WONMSG.timestamp,
                    WONMSG.respondingTo,
                    WONMSG.respondingToMessageType),
    FAILURE_RESPONSE(WONMSG.FailureResponse,
                    new Property[] {
                                    WONMSG.atom,
                                    WONMSG.connection,
                                    WONMSG.senderSocket,
                                    WONMSG.recipientSocket
                    },
                    WONMSG.protocolVersion,
                    RDF.type,
                    WONMSG.messageType,
                    WONMSG.timestamp,
                    WONMSG.respondingTo,
                    WONMSG.respondingToMessageType),
    ATOM_MESSAGE(WONMSG.AtomMessage, null,
                    WONMSG.protocolVersion,
                    RDF.type,
                    WONMSG.messageType,
                    WONMSG.timestamp,
                    WONMSG.content),
    ATOM_CREATED_NOTIFICATION(WONMSG.AtomCreatedNotificationMessage, null,
                    WONMSG.protocolVersion,
                    RDF.type,
                    WONMSG.messageType,
                    WONMSG.timestamp,
                    WONMSG.atom),
    CHANGE_NOTIFICATION(WONMSG.ChangeNotificationMessage, new Property[] { WONMSG.content, WONMSG.forwardedMessage },
                    WONMSG.protocolVersion,
                    RDF.type,
                    WONMSG.messageType,
                    WONMSG.timestamp,
                    WONMSG.senderSocket,
                    WONMSG.recipientSocket);
    private Resource resource;
    private Set<Property> requiredEnvelopeProperties;
    private Set<Property> optionalEnvelopeProperties;

    WonMessageType(Resource resource, Property[] optional, Property... required) {
        this.resource = resource;
        this.requiredEnvelopeProperties = required == null ? Collections.EMPTY_SET
                        : Arrays.asList(required).stream().collect(Collectors.toSet());
        this.optionalEnvelopeProperties = optional == null ? Collections.EMPTY_SET
                        : Arrays.asList(optional).stream().collect(Collectors.toSet());
    }

    public Resource getResource() {
        return resource;
    }

    public URI getURI() {
        return URI.create(getResource().getURI());
    }

    public static WonMessageType getWonMessageType(URI uri) {
        return getWonMessageType(WONMSG.toResource(uri));
    }

    public boolean isIdentifiedBy(URI uri) {
        if (uri == null)
            return false;
        return getResource().getURI().equals(uri.toString());
    }

    public boolean causesConnectionStateChange() {
        return this == CLOSE || this == CONNECT;
    }

    public boolean causesAtomStateChange() {
        return this == ACTIVATE || this == DEACTIVATE || this == REPLACE || this == DELETE;
    }

    public boolean isAtomSpecificMessage() {
        return this == CREATE_ATOM || this == ACTIVATE || this == DEACTIVATE || this == REPLACE || this == DELETE
                        || this == ATOM_MESSAGE
                        || this == ATOM_HINT_MESSAGE;
    }

    public boolean isConnectionSpecificMessage() {
        return this == CONNECT || this == CONNECTION_MESSAGE || this == CLOSE || this == CHANGE_NOTIFICATION
                        || this == SOCKET_HINT_MESSAGE || this == HINT_FEEDBACK_MESSAGE;
    }

    public boolean causesNewConnection() {
        return this == CONNECT || this == SOCKET_HINT_MESSAGE;
    }

    public boolean isHintMessage() {
        return this == ATOM_HINT_MESSAGE || this == SOCKET_HINT_MESSAGE;
    }

    public boolean isResponseMessage() {
        return this == SUCCESS_RESPONSE || this == FAILURE_RESPONSE;
    }

    public boolean causesOutgoingMessage() {
        return this == CLOSE
                        || this == CONNECT
                        || this == CONNECTION_MESSAGE
                        || this == FAILURE_RESPONSE
                        || this == SUCCESS_RESPONSE
                        || this == CHANGE_NOTIFICATION;
    }

    public static WonMessageType getWonMessageType(Resource resource) {
        if (WONMSG.CreateMessage.equals(resource))
            return CREATE_ATOM;
        if (WONMSG.ReplaceMessage.equals(resource))
            return REPLACE;
        if (WONMSG.ConnectMessage.equals(resource))
            return CONNECT;
        if (WONMSG.DeactivateMessage.equals(resource))
            return DEACTIVATE;
        if (WONMSG.ActivateMessage.equals(resource))
            return ACTIVATE;
        if (WONMSG.CloseMessage.equals(resource))
            return CLOSE;
        if (WONMSG.DeleteMessage.equals(resource))
            return DELETE;
        if (WONMSG.ConnectionMessage.equals(resource))
            return CONNECTION_MESSAGE;
        if (WONMSG.AtomMessage.equals(resource))
            return ATOM_MESSAGE;
        if (WONMSG.AtomHintMessage.equals(resource))
            return ATOM_HINT_MESSAGE;
        if (WONMSG.SocketHintMessage.equals(resource))
            return SOCKET_HINT_MESSAGE;
        if (WONMSG.HintFeedbackMessage.equals(resource))
            return HINT_FEEDBACK_MESSAGE;
        // response classes
        if (WONMSG.SuccessResponse.equals(resource))
            return SUCCESS_RESPONSE;
        if (WONMSG.FailureResponse.equals(resource))
            return FAILURE_RESPONSE;
        // notification classes
        if (WONMSG.ChangeNotificationMessage.equals(resource))
            return CHANGE_NOTIFICATION;
        if (WONMSG.AtomCreatedNotificationMessage.equals(resource))
            return ATOM_CREATED_NOTIFICATION;
        return null;
    }

    public void requireType(WonMessageType expectedType) {
        if (this != expectedType) {
            throw new WrongMessageTypeException(expectedType, this);
        }
    }

    public boolean isCreateAtom() {
        return this == CREATE_ATOM;
    }

    public boolean isReplace() {
        return this == REPLACE;
    }

    public boolean isConnect() {
        return this == CONNECT;
    }

    public boolean isDeactivate() {
        return this == DEACTIVATE;
    }

    public boolean isActivate() {
        return this == ACTIVATE;
    }

    public boolean isClose() {
        return this == CLOSE;
    }

    public boolean isDelete() {
        return this == DELETE;
    }

    public boolean isConnectionMessage() {
        return this == CONNECTION_MESSAGE;
    }

    public boolean isAtomMessage() {
        return this == ATOM_MESSAGE;
    }

    public boolean isAtomHintMessage() {
        return this == ATOM_HINT_MESSAGE;
    }

    public boolean isSocketHintMessage() {
        return this == SOCKET_HINT_MESSAGE;
    }

    public boolean isHintFeedbackMessage() {
        return this == HINT_FEEDBACK_MESSAGE;
    }

    public boolean isAtomCreatedNotification() {
        return this == WonMessageType.ATOM_CREATED_NOTIFICATION;
    }

    public boolean isSuccessResponse() {
        return this == SUCCESS_RESPONSE;
    }

    public boolean isFailureResponse() {
        return this == FAILURE_RESPONSE;
    }

    public boolean isChangeNotification() {
        return this == CHANGE_NOTIFICATION;
    }

    public Set<Property> getRequiredEnvelopeProperties() {
        return Collections.unmodifiableSet(this.requiredEnvelopeProperties);
    }

    public Set<Property> getOptionalEnvelopeProperties() {
        return Collections.unmodifiableSet(this.optionalEnvelopeProperties);
    }
}

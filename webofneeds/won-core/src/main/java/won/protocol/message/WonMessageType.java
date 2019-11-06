package won.protocol.message;

import java.net.URI;

import org.apache.jena.rdf.model.Resource;

import won.protocol.vocabulary.WONMSG;

/**
 * User: ypanchenko Date: 13.08.2014
 */
public enum WonMessageType {
    // main messages
    CREATE_ATOM(WONMSG.CreateMessage),
    REPLACE(WONMSG.ReplaceMessage),
    CONNECT(WONMSG.ConnectMessage),
    DEACTIVATE(WONMSG.DeactivateMessage),
    ACTIVATE(WONMSG.ActivateMessage),
    CLOSE(WONMSG.CloseMessage),
    DELETE(WONMSG.DeleteMessage),
    CONNECTION_MESSAGE(WONMSG.ConnectionMessage),
    ATOM_MESSAGE(WONMSG.AtomMessage),
    ATOM_HINT_MESSAGE(WONMSG.AtomHintMessage),
    SOCKET_HINT_MESSAGE(WONMSG.SocketHintMessage),
    HINT_FEEDBACK_MESSAGE(WONMSG.HintFeedbackMessage),
    // notification messages
    HINT_NOTIFICATION(WONMSG.HintNotificationMessage),
    ATOM_CREATED_NOTIFICATION(WONMSG.AtomCreatedNotificationMessage),
    // response messages
    SUCCESS_RESPONSE(WONMSG.SuccessResponse), FAILURE_RESPONSE(WONMSG.FailureResponse),
    CHANGE_NOTIFICATION(WONMSG.ChangeNotificationMessage);
    private Resource resource;

    WonMessageType(Resource resource) {
        this.resource = resource;
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
        return this == ACTIVATE || this == DEACTIVATE || this == REPLACE || this == DELETE || this == ATOM_MESSAGE
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
        if (WONMSG.ChangeNotificationMessage.equals(resource))
            return CHANGE_NOTIFICATION;
        if (WONMSG.FailureResponse.equals(resource))
            return FAILURE_RESPONSE;
        // notification classes
        if (WONMSG.HintNotificationMessage.equals(resource))
            return HINT_NOTIFICATION;
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

    public boolean isHintNotification() {
        return this == HINT_NOTIFICATION;
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
}

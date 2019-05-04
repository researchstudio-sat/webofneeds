package won.protocol.message;

import java.net.URI;

import org.apache.jena.rdf.model.Resource;

import won.protocol.vocabulary.WONMSG;

/**
 * User: ypanchenko Date: 13.08.2014
 */
public enum WonMessageType {
    // main messages
    CREATE_ATOM(WONMSG.CreateMessage), REPLACE(WONMSG.ReplaceMessage), CONNECT(WONMSG.ConnectMessage),
    DEACTIVATE(WONMSG.DeactivateMessage), ACTIVATE(WONMSG.ActivateMessage), CLOSE(WONMSG.CloseMessage),
    DELETE(WONMSG.DeleteMessage), OPEN(WONMSG.OpenMessage), CONNECTION_MESSAGE(WONMSG.ConnectionMessage),
    ATOM_MESSAGE(WONMSG.AtomMessage), HINT_MESSAGE(WONMSG.HintMessage),
    HINT_FEEDBACK_MESSAGE(WONMSG.HintFeedbackMessage),
    // notification messages
    HINT_NOTIFICATION(WONMSG.HintNotificationMessage), ATOM_CREATED_NOTIFICATION(WONMSG.AtomCreatedNotificationMessage),
    // response messages
    SUCCESS_RESPONSE(WONMSG.SuccessResponse), FAILURE_RESPONSE(WONMSG.FailureResponse),
    CHANGE_NOTIFICATION(WONMSG.ChangeNotificationMessage);
    private Resource resource;

    private WonMessageType(Resource resource) {
        this.resource = resource;
    }

    public Resource getResource() {
        return resource;
    }

    public URI getURI() {
        return URI.create(getResource().getURI().toString());
    }

    public static WonMessageType getWonMessageType(URI uri) {
        return getWonMessageType(WONMSG.toResource(uri));
    }

    public boolean isIdentifiedBy(URI uri) {
        if (uri == null)
            return false;
        return getResource().getURI().toString().equals(uri.toString());
    }

    public boolean causesConnectionStateChange() {
        return this == CLOSE || this == CONNECT || this == OPEN;
    }

    public boolean causesAtomStateChange() {
        return this == ACTIVATE || this == DEACTIVATE || this == REPLACE || this == DELETE;
    }

    public boolean causesNewConnection() {
        return this == CONNECT || this == HINT_MESSAGE;
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
        if (WONMSG.OpenMessage.equals(resource))
            return OPEN;
        if (WONMSG.CloseMessage.equals(resource))
            return CLOSE;
        if (WONMSG.DeleteMessage.equals(resource))
            return DELETE;
        if (WONMSG.ConnectionMessage.equals(resource))
            return CONNECTION_MESSAGE;
        if (WONMSG.AtomMessage.equals(resource))
            return ATOM_MESSAGE;
        if (WONMSG.HintMessage.equals(resource))
            return HINT_MESSAGE;
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
}

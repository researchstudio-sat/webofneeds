package won.owner.pojo;

/**
 * Used for the server side connect action (describes post payload) fromSocket:
 * String representation of senderSocketUri toSocket: String representation of
 * targetSocketUri fromPending: if atom to connect from is currently in creation
 * (e.g. socket is still pending/not available) toPending: if atom to connect to
 * is currently in creation (e.g. socket is still pending/not available)
 * message: message that is used within the connect message (see default value)
 * autoOpen: if set to true, a successful connect from the one side will be
 * answered with another connect (only possible if both atoms belong to the
 * user)
 */
public class ServerSideConnectPayload {
    String fromSocket;
    String toSocket;
    boolean fromPending = false;
    boolean toPending = false;
    boolean autoOpen = false;
    String message = "Connect message automatically sent by a server-side action";

    public ServerSideConnectPayload() {
    }

    public String getFromSocket() {
        return fromSocket;
    }

    public void setFromSocket(String fromSocket) {
        this.fromSocket = fromSocket;
    }

    public String getToSocket() {
        return toSocket;
    }

    public void setToSocket(String toSocket) {
        this.toSocket = toSocket;
    }

    public boolean isFromPending() {
        return fromPending;
    }

    public void setFromPending(boolean fromPending) {
        this.fromPending = fromPending;
    }

    public boolean isToPending() {
        return toPending;
    }

    public void setToPending(boolean toPending) {
        this.toPending = toPending;
    }

    public boolean isAutoOpen() {
        return autoOpen;
    }

    public void setAutoOpen(boolean autoOpen) {
        this.autoOpen = autoOpen;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

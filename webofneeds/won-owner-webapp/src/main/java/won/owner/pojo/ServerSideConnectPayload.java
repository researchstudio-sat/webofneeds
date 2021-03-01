package won.owner.pojo;

/**
 * Data transfer object for the server side connect action.
 */
public class ServerSideConnectPayload {
    /**
     * String representation of senderSocketUri.
     */
    String fromSocket;
    /**
     * String representation of targetSocketUri.
     */
    String toSocket;
    /**
     * Set to <code>true</code> if the atom to connect <i>from</i> is currently in
     * creation (e.g. socket is still pending/not available).
     */
    boolean fromPending = false;
    /**
     * Set to <code>true</code> if the atom to connect <i>to</i> is currently in
     * creation (e.g. socket is still pending/not available).
     */
    boolean toPending = false;
    /**
     * If set to <code>true</code>, a successful connect from the one side will be
     * answered with another connect (only possible if both atoms belong to the
     * user).
     */
    boolean autoOpen = false;
    /**
     * Message to use as the connect message's <code>textMessage</code> (see default
     * value).
     */
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

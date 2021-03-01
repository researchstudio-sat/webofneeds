package won.owner.pojo;

public class SocketToConnect {
    String socket;
    String message = "Connect message automatically sent by a server-side action";
    boolean pending = false;
    boolean nonOwned = false;

    public SocketToConnect() {
    }

    public String getSocket() {
        return socket;
    }

    public void setSocket(String socket) {
        this.socket = socket;
    }

    public boolean isPending() {
        return pending;
    }

    public void setPending(boolean pending) {
        this.pending = pending;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isNonOwned() {
        return nonOwned;
    }

    public void setNonOwned(boolean nonOwned) {
        this.nonOwned = nonOwned;
    }
}

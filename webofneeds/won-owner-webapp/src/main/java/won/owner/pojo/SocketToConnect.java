package won.owner.pojo;

public class SocketToConnect {
    String socket;
    boolean pending = false;

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
}

package won.protocol.exception;

public class SocketCapacityException extends WonProtocolException {
    public SocketCapacityException() {
    }

    public SocketCapacityException(String message) {
        super(message);
    }

    public SocketCapacityException(Throwable cause) {
        super(cause);
    }

    public SocketCapacityException(String message, Throwable cause) {
        super(message, cause);
    }
}

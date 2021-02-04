package won.protocol.exception;

public class IllegalMessageSignerException extends WonProtocolException {
    public IllegalMessageSignerException() {
    }

    public IllegalMessageSignerException(String message) {
        super(message);
    }

    public IllegalMessageSignerException(String message, Throwable cause) {
        super(message, cause);
    }

    public IllegalMessageSignerException(Throwable cause) {
        super(cause);
    }
}

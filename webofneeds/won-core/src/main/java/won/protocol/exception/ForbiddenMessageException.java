package won.protocol.exception;

public class ForbiddenMessageException extends WonProtocolException {
    public ForbiddenMessageException() {
    }

    public ForbiddenMessageException(String message) {
        super(message);
    }

    public ForbiddenMessageException(String message, Throwable cause) {
        super(message, cause);
    }

    public ForbiddenMessageException(Throwable cause) {
        super(cause);
    }
}

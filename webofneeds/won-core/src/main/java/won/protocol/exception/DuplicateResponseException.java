package won.protocol.exception;

public class DuplicateResponseException extends WonProtocolException {
    private static final long serialVersionUID = -3834992485733353559L;

    public DuplicateResponseException() {
        super();
    }

    public DuplicateResponseException(String message, Throwable cause) {
        super(message, cause);
    }

    public DuplicateResponseException(String message) {
        super(message);
    }

    public DuplicateResponseException(Throwable cause) {
        super(cause);
    }
}

package won.protocol.exception;

/**
 * Created with IntelliJ IDEA.
 * User: gabriel
 * Date: 15.02.13
 * Time: 15:40
 * To change this template use File | Settings | File Templates.
 */
public class RDFStorageException extends RuntimeException {
    public RDFStorageException() {
    }

    public RDFStorageException(String message) {
        super(message);
    }

    public RDFStorageException(String message, Throwable cause) {
        super(message, cause);
    }

    public RDFStorageException(Throwable cause) {
        super(cause);
    }

    public RDFStorageException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}

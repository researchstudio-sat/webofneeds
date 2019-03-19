package won.protocol.message.processor.exception;

/**
 * Indicates that the uri (of event or need) is malformed from the point of view of the node that is supposed to store
 * it. (e.g. the domain if the message event uri does not correspond to the node domain, etc.)
 *
 * User: ypanchenko Date: 24.04.2015
 */
public class UriNodePathException extends WonMessageProcessingException {
    public UriNodePathException() {
    }

    public UriNodePathException(String message) {
        super(message);
    }

    public UriNodePathException(String message, Throwable cause) {
        super(message, cause);
    }

    public UriNodePathException(Throwable cause) {
        super(cause);
    }

    public UriNodePathException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}

package won.protocol.message.processor.exception;

/**
 * Indicates that the uri (of event or need) is already in use.
 *
 * User: ypanchenko Date: 24.04.2015
 */
public class UriAlreadyInUseException extends WonMessageProcessingException {
    public UriAlreadyInUseException() {
    }

    public UriAlreadyInUseException(final String uri) {
        super(uri);
    }

    public UriAlreadyInUseException(final String uri, final Throwable cause) {
        super(uri, cause);
    }

    public UriAlreadyInUseException(final Throwable cause) {
        super(cause);
    }

    public UriAlreadyInUseException(final String uri, final Throwable cause, final boolean enableSuppression,
            final boolean writableStackTrace) {
        super(uri, cause, enableSuppression, writableStackTrace);
    }
}

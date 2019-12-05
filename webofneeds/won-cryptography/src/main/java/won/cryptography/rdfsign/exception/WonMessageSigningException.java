package won.cryptography.rdfsign.exception;

import won.protocol.exception.WonMessageProcessingException;
import won.protocol.message.WonMessage;

public class WonMessageSigningException extends WonMessageProcessingException {
    public WonMessageSigningException() {
    }

    public WonMessageSigningException(WonMessage wonMessage) {
        super(wonMessage);
    }

    public WonMessageSigningException(String message) {
        super(message);
    }

    public WonMessageSigningException(String message, WonMessage wonMessage) {
        super(message, wonMessage);
    }

    public WonMessageSigningException(String message, Throwable cause) {
        super(message, cause);
    }

    public WonMessageSigningException(Throwable cause) {
        super(cause);
    }

    public WonMessageSigningException(String message, Throwable cause, boolean enableSuppression,
                    boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}

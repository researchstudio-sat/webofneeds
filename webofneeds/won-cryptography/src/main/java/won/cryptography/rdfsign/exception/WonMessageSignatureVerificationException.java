package won.cryptography.rdfsign.exception;

import won.protocol.exception.WonMessageProcessingException;
import won.protocol.message.WonMessage;

public class WonMessageSignatureVerificationException extends WonMessageProcessingException {
    public WonMessageSignatureVerificationException() {
    }

    public WonMessageSignatureVerificationException(WonMessage wonMessage) {
        super(wonMessage);
    }

    public WonMessageSignatureVerificationException(String message) {
        super(message);
    }

    public WonMessageSignatureVerificationException(String message, WonMessage wonMessage) {
        super(message, wonMessage);
    }

    public WonMessageSignatureVerificationException(String message, Throwable cause) {
        super(message, cause);
    }

    public WonMessageSignatureVerificationException(Throwable cause) {
        super(cause);
    }

    public WonMessageSignatureVerificationException(String message, Throwable cause, boolean enableSuppression,
                    boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}

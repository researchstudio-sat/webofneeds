package won.cryptography.rdfsign.exception;

import won.protocol.exception.WonMessageProcessingException;
import won.protocol.message.WonMessage;

public class WonMessageHashingException extends WonMessageProcessingException {
    public WonMessageHashingException() {
    }

    public WonMessageHashingException(WonMessage wonMessage) {
        super(wonMessage);
    }

    public WonMessageHashingException(String message) {
        super(message);
    }

    public WonMessageHashingException(String message, WonMessage wonMessage) {
        super(message, wonMessage);
    }

    public WonMessageHashingException(String message, Throwable cause) {
        super(message, cause);
    }

    public WonMessageHashingException(Throwable cause) {
        super(cause);
    }

    public WonMessageHashingException(String message, Throwable cause, boolean enableSuppression,
                    boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}

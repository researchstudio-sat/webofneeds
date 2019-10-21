package won.protocol.message;

import java.text.MessageFormat;

public class WrongMessageTypeException extends RuntimeException {
    /**
     * 
     */
    private static final long serialVersionUID = -413489974525256663L;
    private WonMessageType expectedType;
    private WonMessageType actualType;

    public WrongMessageTypeException(WonMessageType expectedType, WonMessageType actualType) {
        super(MessageFormat.format("Message has the wrong message type, expected: {0}, actual: {1}", expectedType,
                        actualType));
        this.expectedType = expectedType;
        this.actualType = actualType;
    }

    public WonMessageType getActualType() {
        return actualType;
    }

    public WonMessageType getExpectedType() {
        return expectedType;
    }
}

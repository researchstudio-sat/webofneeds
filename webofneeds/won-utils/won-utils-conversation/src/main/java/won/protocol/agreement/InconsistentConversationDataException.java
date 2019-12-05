package won.protocol.agreement;

public class InconsistentConversationDataException extends ConversationDataAnalysisException {
    public InconsistentConversationDataException() {
    }

    public InconsistentConversationDataException(String message) {
        super(message);
    }

    public InconsistentConversationDataException(Throwable cause) {
        super(cause);
    }

    public InconsistentConversationDataException(String message, Throwable cause) {
        super(message, cause);
    }

    public InconsistentConversationDataException(String message, Throwable cause, boolean enableSuppression,
                    boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}

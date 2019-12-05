package won.protocol.agreement;

public class ConversationDataAnalysisException extends RuntimeException {
    public ConversationDataAnalysisException() {
    }

    public ConversationDataAnalysisException(String message) {
        super(message);
    }

    public ConversationDataAnalysisException(Throwable cause) {
        super(cause);
    }

    public ConversationDataAnalysisException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConversationDataAnalysisException(String message, Throwable cause, boolean enableSuppression,
                    boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}

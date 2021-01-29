package won.auth.check;

public class WonAclEvaluationException extends RuntimeException {
    public WonAclEvaluationException() {
    }

    public WonAclEvaluationException(String message) {
        super(message);
    }

    public WonAclEvaluationException(String message, Throwable cause) {
        super(message, cause);
    }

    public WonAclEvaluationException(Throwable cause) {
        super(cause);
    }

    public WonAclEvaluationException(String message, Throwable cause, boolean enableSuppression,
                    boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}

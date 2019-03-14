package won.owner.service.impl;

public class WrongOldPasswordException extends Exception {

    public WrongOldPasswordException() {
        super();
    }

    public WrongOldPasswordException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public WrongOldPasswordException(String message, Throwable cause) {
        super(message, cause);
    }

    public WrongOldPasswordException(String message) {
        super(message);
    }

    public WrongOldPasswordException(Throwable cause) {
        super(cause);
    }


}

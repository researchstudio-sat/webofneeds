package won.bot.exception;

public class NoBotResponsibleException extends IllegalStateException {
    public NoBotResponsibleException() {
    }

    public NoBotResponsibleException(String s) {
        super(s);
    }

    public NoBotResponsibleException(String message, Throwable cause) {
        super(message, cause);
    }

    public NoBotResponsibleException(Throwable cause) {
        super(cause);
    }
}

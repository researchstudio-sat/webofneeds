package won.owner.service.impl;

public class IncorrectPasswordException extends Exception {

  public IncorrectPasswordException() {
    super();
  }

  public IncorrectPasswordException(String message, Throwable cause, boolean enableSuppression,
      boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }

  public IncorrectPasswordException(String message, Throwable cause) {
    super(message, cause);
  }

  public IncorrectPasswordException(String message) {
    super(message);
  }

  public IncorrectPasswordException(Throwable cause) {
    super(cause);
  }

}

package won.protocol.exception;

/**
 * User: fsalcher
 * Date: 27.08.2014
 */
public class WonMessageBuilderException extends RuntimeException
{

  public WonMessageBuilderException() {
  }

  public WonMessageBuilderException(final String message) {
    super(message);
  }

  public WonMessageBuilderException(final String message, final Throwable cause) {
    super(message, cause);
  }

  public WonMessageBuilderException(final Throwable cause) {
    super(cause);
  }

  public WonMessageBuilderException(final String message, final Throwable cause, final boolean enableSuppression, final boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}

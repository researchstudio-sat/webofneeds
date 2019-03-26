package won.protocol.message.processor.exception;

/**
 * Indicates that the message event has already been received and processed.
 *
 * User: ypanchenko Date: 24.04.2015
 */
public class MessageAlreadyProcessedException extends WonMessageProcessingException {
  public MessageAlreadyProcessedException() {
  }

  public MessageAlreadyProcessedException(final String uri) {
    super(uri);
  }

  public MessageAlreadyProcessedException(final String uri, final Throwable cause) {
    super(uri, cause);
  }

  public MessageAlreadyProcessedException(final String uri, final Throwable cause, final boolean enableSuppression,
      final boolean writableStackTrace) {
    super(uri, cause, enableSuppression, writableStackTrace);
  }
}

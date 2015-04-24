package crawler.exception;

import crawler.message.UriStatusMessage;

/**
 * Used to wrap an exception occurred during crawling to pass the breaking message additionally.
 *
 * User: hfriedrich
 * Date: 15.04.2015
 */
public class CrawlingWrapperException extends RuntimeException
{
  private Exception exception;
  private UriStatusMessage breakingMessage;

  public CrawlingWrapperException(Exception e, UriStatusMessage msg) {
    super(e.getMessage());
    exception = e;
    breakingMessage = new UriStatusMessage(msg.getUri(), msg.getBaseUri(), UriStatusMessage.STATUS.FAILED);
  }

  public Exception getException() {
    return exception;
  }

  public UriStatusMessage getBreakingMessage() {
    return breakingMessage;
  }

}

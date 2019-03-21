package won.matcher.service.crawler.exception;

import won.matcher.service.crawler.msg.CrawlUriMessage;

/**
 * Used to wrap an exception occurred during crawling to pass the breaking message additionally.
 * <p>
 * User: hfriedrich
 * Date: 15.04.2015
 */
public class CrawlWrapperException extends RuntimeException {
  private Exception exception;
  private CrawlUriMessage breakingMessage;

  public CrawlWrapperException(Exception e, CrawlUriMessage msg) {
    super(e.getMessage());
    exception = e;
    breakingMessage = new CrawlUriMessage(msg.getUri(), msg.getBaseUri(), msg.getWonNodeUri(),
        CrawlUriMessage.STATUS.FAILED, msg.getCrawlDate(), msg.getResourceETagHeaderValues());
  }

  public Exception getException() {
    return exception;
  }

  public CrawlUriMessage getBreakingMessage() {
    return breakingMessage;
  }

}

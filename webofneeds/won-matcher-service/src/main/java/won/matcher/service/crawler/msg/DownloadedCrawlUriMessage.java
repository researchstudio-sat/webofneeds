package won.matcher.service.crawler.msg;

/**
 * Created by hfriedrich on 11.10.2016.
 */
public class DownloadedCrawlUriMessage extends CrawlUriMessage
{
  public DownloadedCrawlUriMessage(final String uri, final String baseUri, final String wonNodeUri, final STATUS status) {
    super(uri, baseUri, wonNodeUri, status);
  }
}

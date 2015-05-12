package crawler.msg;

/**
 * Message class used to crawl URIs relative to base URIs from certain won nodes
 *
 * User: hfriedrich
 * Date: 30.03.2015
 */
public class CrawlUriMessage
{

  private String uri;
  private String baseUri;
  private String wonNodeUri;
  private STATUS status;

  /**
   * Constructor
   *
   * @param uri URI that should be or is already crawled
   * @param baseUri base URI that is used with property paths to extract further URIs
   * @param wonNodeUri URI of the corresponding won node
   * @param status describes what to with the URI
   */
  public CrawlUriMessage(final String uri, final String baseUri, String wonNodeUri, final STATUS status) {
    this.uri = uri;
    this.baseUri = baseUri;
    this.status = status;
    this.wonNodeUri = wonNodeUri;
  }

  public CrawlUriMessage(final String uri, final String baseUri, final STATUS status) {
    this.uri = uri;
    this.baseUri = baseUri;
    this.status = status;
    this.wonNodeUri = null;
  }

  public String getUri() {
    return uri;
  }

  public STATUS getStatus() {
    return status;
  }

  public String getBaseUri() {
    return baseUri;
  }

  public String getWonNodeUri() {
    return wonNodeUri;
  }

  @Override
  public String toString() {
    return "[" + uri + "," + baseUri + "," + wonNodeUri + "," + status + "]";
  }

  @Override
  public CrawlUriMessage clone() {
    return new CrawlUriMessage(uri, baseUri, wonNodeUri, status);
  }

  @Override
  public boolean equals(Object obj) {

    if (obj instanceof CrawlUriMessage) {
      CrawlUriMessage msg = (CrawlUriMessage) obj;
      return uri.equals(msg.getUri()) && baseUri.equals(msg.getBaseUri()) &&
        status.equals(msg.getStatus()) && wonNodeUri.equals(msg.getWonNodeUri());
    }

    return false;
  }

  @Override
  public int hashCode() {
    return (uri + baseUri + wonNodeUri + status.toString()).hashCode();
  }

  public static enum STATUS
  {
    PROCESS, FAILED, DONE, SKIP
  }
}

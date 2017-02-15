package won.matcher.service.crawler.msg;


import java.io.Serializable;

/**
 * Message class used to crawl URIs relative to base URIs from certain won nodes
 *
 * User: hfriedrich
 * Date: 30.03.2015
 */
public class CrawlUriMessage implements Serializable
{
  public static enum STATUS
  {
    PROCESS,  // resource is currently in the crawling process (e.g. downloading, link extraction, saving to rdf store)
    FAILED,   // crawler failed to process the resource correctly in the last execution
    DONE,     // resource was successfully crawled and saved and extracted links from that resource are processed too
    SAVE,     // resource has been saved to the rdf store but links are not extracted. That means crawling of linked
              // resources is not happening right now. In contrast to "DONE" the "SAVE" status is set if the resource
              // was received by event subscription from the wonnode and not via the crawling process.
  }

  private String uri;
  private String baseUri;
  private String wonNodeUri;
  private STATUS status;
  private long crawlDate;

  /**
   * Constructor
   *
   * @param uri URI that should be or is already crawled
   * @param baseUri base URI that is used with property paths to extract further URIs
   * @param wonNodeUri URI of the corresponding won node
   * @param status describes what to with the URI
   */

  /**
   *
   * @param uri URI that should be or is already crawled
   * @param baseUri base URI that is used with property paths to extract further URIs
   * @param wonNodeUri URI of the corresponding won node
   * @param status describes what to with the URI
   * @param crawlDate timestamp in milli seconds when crawling message was generated
   */
  public CrawlUriMessage(final String uri, final String baseUri, String wonNodeUri, final STATUS status, long crawlDate) {
    this.uri = uri;
    this.baseUri = baseUri;
    this.status = status;
    this.wonNodeUri = wonNodeUri;
    this.crawlDate = crawlDate;
  }

  /**
   *
   * @param uri URI that should be or is already crawled
   * @param baseUri base URI that is used with property paths to extract further URIs
   * @param status describes what to with the URI
   * @param crawlDate timestamp in milli seconds when crawling message was generated
   */
  public CrawlUriMessage(final String uri, final String baseUri, final STATUS status, long crawlDate) {
    this.uri = uri;
    this.baseUri = baseUri;
    this.status = status;
    this.wonNodeUri = null;
    this.crawlDate = crawlDate;
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

  public long getCrawlDate() {
    return crawlDate;
  }

  @Override
  public String toString() {
    return "[" + uri + "," + baseUri + "," + wonNodeUri + "," + status + "," + crawlDate + "]";
  }

  @Override
  public CrawlUriMessage clone() {
    return new CrawlUriMessage(uri, baseUri, wonNodeUri, status, crawlDate);
  }

  @Override
  public boolean equals(Object obj) {

    if (obj instanceof CrawlUriMessage) {
      CrawlUriMessage msg = (CrawlUriMessage) obj;
      if (uri.equals(msg.getUri()) && baseUri.equals(msg.getBaseUri()) &&
        status.equals(msg.getStatus()) && crawlDate == msg.getCrawlDate()) {
        return (wonNodeUri == null) ? msg.getWonNodeUri() == null : wonNodeUri.equals(msg.getWonNodeUri());
      }
    }

    return false;
  }

  @Override
  public int hashCode() {
    return (uri + baseUri + wonNodeUri + status.toString() + crawlDate).hashCode();
  }
}

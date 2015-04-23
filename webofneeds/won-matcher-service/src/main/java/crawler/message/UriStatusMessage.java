package crawler.message;

/**
 * Message class used to exchange URIs with status to determine according actions.
 *
 * User: hfriedrich
 * Date: 30.03.2015
 */
public class UriStatusMessage
{

  public static enum STATUS
  {
    PROCESS, FAILED, DONE
  }

  /**
   * Constructor
   * @param uri URI that should be or is already crawled
   * @param baseUri base URI that is used with property paths to extract further URIs
   * @param action describes what to with the URI
   */
  public UriStatusMessage(final String uri, final String baseUri, final STATUS action) {
    this.uri = uri;
    this.baseUri = baseUri;
    this.status = action;
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

  @Override
  public String toString() {
    return "[" + uri + "," + baseUri + "," + status + "]";
  }

  private String uri;
  private String baseUri;
  private STATUS status;
}

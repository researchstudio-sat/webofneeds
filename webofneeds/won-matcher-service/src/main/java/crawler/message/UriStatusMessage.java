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
   * @param status describes what to with the URI
   */
  public UriStatusMessage(final String uri, final String baseUri, final STATUS status) {
    this.uri = uri;
    this.baseUri = baseUri;
    this.status = status;
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

  @Override
  public UriStatusMessage clone() {
    return new UriStatusMessage(uri, baseUri, status);
  }

  @Override
  public boolean equals(Object obj) {

    if (obj instanceof UriStatusMessage) {
      UriStatusMessage msg = (UriStatusMessage) obj;
      return uri.equals(msg.getUri()) && baseUri.equals(msg.getBaseUri()) &&
        status.equals(msg.getStatus());
    }

    return false;
  }

  @Override
  public int hashCode() {
    return (uri + baseUri + status.toString()).hashCode();
  }

  private String uri;
  private String baseUri;
  private STATUS status;
}

package messages;

/**
 * Message class used to exchange URIs and specify actions.
 * User: hfriedrich
 * Date: 30.03.2015
 */
public class UriActionMessage
{

  public static enum ACTION {
    PROCESS,
    REMOVE
  }

  /**
   * Constructor
   * @param uri URI that should be or is already crawled
   * @param baseUri base URI that is used with property paths to extract further URIs
   * @param action describes what to with the URI
   */
  public UriActionMessage(final String uri, final String baseUri, final ACTION action) {
    this.uri = uri;
    this.baseUri = baseUri;
    this.action = action;
  }

  public String getUri() {
    return uri;
  }

  public ACTION getAction() {
    return action;
  }

  public String getBaseUri() {
    return baseUri;
  }

  @Override
  public String toString() {
    return "[" + uri + "," + baseUri + "," + action + "]";
  }

  private String uri;
  private String baseUri;
  private ACTION action;
}

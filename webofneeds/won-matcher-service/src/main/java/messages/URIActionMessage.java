package messages;

/**
 * Message class used to exchange URIs and specify actions what to do with them.
 * User: hfriedrich
 * Date: 30.03.2015
 */
public class URIActionMessage
{
  public static enum ACTION {
    PROCESS,
    REMOVE
  }

  /**
   * @param uri URI that should be or is already crawled
   * @param action describes what to with the URI
   */
  public URIActionMessage(final String uri, ACTION action) {
    this.uri = uri;
    this.action = action;
  }

  public String getUri() {
    return uri;
  }

  public ACTION getAction() {
    return action;
  }

  private String uri;
  private ACTION action;
}

package won.protocol.service;

import java.net.URI;

/**
 * User: fsalcher
 * Date: 17.09.2014
 */
public interface WonNodeInformationService
{

  public class WonNodeInformation
  {

    public WonNodeInformation(
      String needMessageEventURIPattern,
      String connectionURIPattern,
      String needURIPattern,
      String idPlaceholder) {

      this.needMessageEventURIPattern = needMessageEventURIPattern;
      this.connectionURIPattern = connectionURIPattern;
      this.needURIPattern = needURIPattern;
      this.idPlaceholder = idPlaceholder;
    }

    private String needMessageEventURIPattern;
    private String connectionURIPattern;
    private String needURIPattern;

    private String idPlaceholder;

    public String getNeedMessageEventURIPattern() {
      return needMessageEventURIPattern;
    }

    public String getConnectionURIPattern() {
      return connectionURIPattern;
    }

    public String getNeedURIPattern() {
      return needURIPattern;
    }

    public String getIdPlaceholder() {
      return idPlaceholder;
    }

  }

  public WonNodeInformation getWonNodeInformation(URI wonNodeURI);

  public URI generateMessageEventURI(URI needURI, URI wonNodeURI);

  public URI generateMessageEventURI(URI wonNodeURI);

  public URI generateMessageEventURI();

  public URI generateConnectionURI(URI needURI, URI wonNodeURI);

  public URI generateNeedURI(URI wonNodeURI);

  public URI getDefaultWonNode();

}

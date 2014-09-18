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
      String connectionMessageEventURIPattern,
      String connectionURIPattern,
      String needURIPattern,
      String needURIPlaceholder,
      String connectionURIPlaceholder,
      String idPlaceholder) {

      this.needMessageEventURIPattern = needMessageEventURIPattern;
      this.connectionMessageEventURIPattern = connectionMessageEventURIPattern;
      this.connectionURIPattern = connectionURIPattern;
      this.needURIPattern = needURIPattern;
      this.needURIPlaceholder = needURIPlaceholder;
      this.connectionURIPlaceholder = connectionURIPlaceholder;
      this.idPlaceholder = idPlaceholder;
    }

    private String needMessageEventURIPattern;
    private String connectionMessageEventURIPattern;
    private String connectionURIPattern;
    private String needURIPattern;

    private String needURIPlaceholder;
    private String connectionURIPlaceholder;
    private String idPlaceholder;

    public String getNeedMessageEventURIPattern() {
      return needMessageEventURIPattern;
    }

    public String getConnectionMessageEventURIPattern() {
      return connectionMessageEventURIPattern;
    }

    public String getConnectionURIPattern() {
      return connectionURIPattern;
    }

    public String getNeedURIPattern() {
      return needURIPattern;
    }

    public String getNeedURIPlaceholder() {
      return needURIPlaceholder;
    }

    public String getConnectionURIPlaceholder() {
      return connectionURIPlaceholder;
    }

    public String getIdPlaceholder() {
      return idPlaceholder;
    }

  }

  public WonNodeInformation getWonNodeInformation(URI wonNodeURI);

  public URI generateNeedMessageEventURI(URI needURI, URI wonNodeURI);

  public URI generateConnectionMessageEventURI(URI connectionURI, URI wonNodeURI);

  public URI generateConnectionURI(URI needURI, URI wonNodeURI);

  public URI generateNeedURI(URI wonNodeURI);

  public URI getDefaultWonNode();

}

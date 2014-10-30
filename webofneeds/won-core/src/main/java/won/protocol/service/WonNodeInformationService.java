package won.protocol.service;

import java.net.URI;

/**
 * Service for managing won node related information and for
 * generating URIs required for messaging and creation of resources.
 */
public interface WonNodeInformationService
{

  public WonNodeInfo getWonNodeInformation(URI wonNodeURI);

  /**
   * Generates a random event URI according to the URI pattern of the
   * default won node.
   *
   * @return
   */
  public URI generateEventURI();

  /**
   * Generates a random event URI according to the URI pattern of the
   * specified won node.
   *
   * @param wonNodeURI
   * @return
   */
  public URI generateEventURI(URI wonNodeURI);

  /**
   * Generates a random connection URI according to the URI pattern of the
   * default won node.
   *
   * @return
   */
  public URI generateConnectionURI();

  /**
   * Generates a random connection URI according to the URI pattern of the
   * specified won node.
   *
   * @param wonNodeURI
   * @return
   */
  public URI generateConnectionURI(URI wonNodeURI);

  /**
   * Generates a random need URI according to the URI pattern of the
   * default won node.
   *
   * @return
   */
  public URI generateNeedURI();

  /**
   * Generates a random need URI according to the URI pattern of the
   * specified won node.
   *
   * @param wonNodeURI
   * @return
   */
  public URI generateNeedURI(URI wonNodeURI);

  public URI getDefaultWonNodeURI();

  /**
   * Obtains the won node uri associated with the specified need or
   * connection resource.
   *
   * @param resourceURI
   * @return the won node URI or null if none is found.
   */
  public URI getWonNodeUri(URI resourceURI);
}

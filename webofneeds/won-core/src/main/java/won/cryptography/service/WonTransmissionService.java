package won.cryptography.service;

import won.cryptography.ssl.MessagingContext;
import won.protocol.rest.LinkedDataRestBridge;
import won.protocol.rest.LinkedDataRestClient;

/**
 * User: ypanchenko
 * Date: 20.10.2015
 */
public interface WonTransmissionService
{

  public LinkedDataRestClient getLinkedDataClient();

  /**
   * The returned class is for use on the owner-app-server side to access linked data resource from the node
   * and than the accessed data response can be sent to the owner-app-client side (javascript side). This class
   * is necessary as long as we store keys of the needs on the owner-app-server side. As long as it changes, the
   * owner-app-client side can access the linked data on the node directly.
   * @return
   */
  public LinkedDataRestBridge getLinkedDataRestBridge();

  public RegistrationClient getRegistrationClient();
  public RegistrationServer getRegistrationServer();

  // TODO ideally, it would be more consistent to return smth like MessagingClient here (e.g. Camel Component)
  public MessagingContext getClientMessagingContext();

  public CryptographyService getClientCryptographyService();

}

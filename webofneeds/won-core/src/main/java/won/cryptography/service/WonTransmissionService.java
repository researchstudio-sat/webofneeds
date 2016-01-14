package won.cryptography.service;

import won.cryptography.ssl.MessagingContext;
import won.protocol.rest.LinkedDataRestClient;

/**
 * User: ypanchenko
 * Date: 20.10.2015
 */
public interface WonTransmissionService
{

  public LinkedDataRestClient getLinkedDataClient();

  public RegistrationClient getRegistrationClient();
  public RegistrationServer getRegistrationServer();

  // TODO ideally, it would be more consistent to return smth like MessagingClient here (e.g. Camel Component)
  public MessagingContext getClientMessagingContext();

  public CryptographyService getClientCryptographyService();

}

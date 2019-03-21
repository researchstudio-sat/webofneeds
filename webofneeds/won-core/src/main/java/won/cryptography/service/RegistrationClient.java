package won.cryptography.service;

import java.io.IOException;

/**
 * User: ypanchenko
 * Date: 15.10.2015
 */
public interface RegistrationClient
{

  public String register(final String remoteNodeUri) throws IOException;
}

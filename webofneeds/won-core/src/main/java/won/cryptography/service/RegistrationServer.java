package won.cryptography.service;

import won.protocol.exception.WonProtocolException;

/**
 * User: ypanchenko Date: 08.10.2015
 */
public interface RegistrationServer {

  public String registerOwner(final Object credentials) throws WonProtocolException;

  public String registerNode(final Object credentials) throws WonProtocolException;

}

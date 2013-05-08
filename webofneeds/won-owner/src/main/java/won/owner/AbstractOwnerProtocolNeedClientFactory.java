package won.owner;

import won.protocol.exception.NoSuchConnectionException;
import won.protocol.exception.NoSuchNeedException;
import won.protocol.ws.OwnerProtocolNeedWebServiceEndpoint;

import java.net.MalformedURLException;
import java.net.URI;

/**
 * User: atus
 * Date: 08.05.13
 */
public interface AbstractOwnerProtocolNeedClientFactory
{
  public OwnerProtocolNeedWebServiceEndpoint getOwnerProtocolEndpoint(URI wonNodeURI) throws NoSuchNeedException, MalformedURLException;

  public OwnerProtocolNeedWebServiceEndpoint getOwnerProtocolEndpointForNeed(URI needURI) throws NoSuchNeedException, MalformedURLException;

  public OwnerProtocolNeedWebServiceEndpoint getOwnerProtocolEndpointForConnection(URI connectionURI) throws NoSuchConnectionException, MalformedURLException;

}

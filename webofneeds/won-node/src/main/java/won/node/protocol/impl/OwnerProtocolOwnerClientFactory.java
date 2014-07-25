package won.node.protocol.impl;

import org.springframework.beans.factory.annotation.Autowired;
import won.node.ws.OwnerProtocolOwnerWebServiceClient;
import won.protocol.exception.NoSuchConnectionException;
import won.protocol.exception.NoSuchNeedException;
import won.protocol.model.Connection;
import won.protocol.model.Need;
import won.protocol.repository.ConnectionRepository;
import won.protocol.repository.NeedRepository;
import won.protocol.util.DataAccessUtils;
import won.protocol.ws.AbstractClientFactory;
import won.protocol.ws.OwnerProtocolOwnerWebServiceEndpoint;

import java.net.MalformedURLException;
import java.net.URI;

/**
 * User: atus
 * Date: 14.05.13
 */
public class OwnerProtocolOwnerClientFactory extends AbstractClientFactory<OwnerProtocolOwnerWebServiceClient>
{
  @Autowired
  private NeedRepository needRepository;

  @Autowired
  private ConnectionRepository connectionRepository;

  public void setNeedRepository(NeedRepository needRepository)
  {
    this.needRepository = needRepository;
  }

  public void setConnectionRepository(ConnectionRepository connectionRepository)
  {
    this.connectionRepository = connectionRepository;
  }

  public OwnerProtocolOwnerWebServiceEndpoint getOwnerProtocolEndpointForNeed(URI needURI) throws NoSuchNeedException, MalformedURLException
  {
    Need owner = DataAccessUtils.loadNeed(needRepository, needURI);
    URI ownerWsdlUri = URI.create(owner.getOwnerURI().toString() + "?wsdl");

    OwnerProtocolOwnerWebServiceClient client = getCachedClient(ownerWsdlUri);

    if (client == null) {
      client = new OwnerProtocolOwnerWebServiceClient(ownerWsdlUri.toURL());
      cacheClient(ownerWsdlUri, client);
    }

    return client.getOwnerProtocolOwnerWebServiceEndpointPort();
  }

  public OwnerProtocolOwnerWebServiceEndpoint getOwnerProtocolEndpointForConnection(URI connectionURI) throws NoSuchConnectionException, MalformedURLException,
      NoSuchNeedException
  {
    Connection connection = DataAccessUtils.loadConnection(connectionRepository, connectionURI);
    URI needUri = connection.getNeedURI();

    return getOwnerProtocolEndpointForNeed(needUri);
  }

  public URI getOwnerProtocolOwnerURI(URI needURI){
      Need owner = needRepository.findByNeedURI(needURI).get(0);
      return  URI.create(owner.getOwnerURI().toString());

  }
}

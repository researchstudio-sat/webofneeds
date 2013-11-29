package won.owner.ws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import won.owner.service.impl.URIService;
import won.protocol.exception.NoSuchConnectionException;
import won.protocol.exception.NoSuchNeedException;
import won.protocol.rest.LinkedDataRestClient;
import won.protocol.vocabulary.WON;
import won.protocol.ws.AbstractClientFactory;
import won.protocol.ws.OwnerProtocolNeedWebServiceEndpoint;

import java.net.MalformedURLException;
import java.net.URI;

/**
 * User: atus
 * Date: 08.05.13
 */
public class OwnerProtocolNeedClientFactory extends AbstractClientFactory<OwnerProtocolNeedWebServiceClient>
{
  /* default wsdl location */
  private static final String WSDL_LOCATION = "?wsdl";

  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Autowired
  private LinkedDataRestClient linkedDataRestClient;

  @Autowired
  private URIService uriService;

  public void setLinkedDataRestClient(LinkedDataRestClient linkedDataRestClient)
  {
    this.linkedDataRestClient = linkedDataRestClient;
  }

  public void setUriService(final URIService uriService)
  {
    this.uriService = uriService;
  }

  public OwnerProtocolNeedWebServiceEndpoint getOwnerProtocolEndpoint(URI wonNodeURI) throws NoSuchNeedException, MalformedURLException
  {
    if (wonNodeURI == null)
      wonNodeURI = uriService.getDefaultOwnerProtocolNeedServiceEndpointURI();

    OwnerProtocolNeedWebServiceClient client = getCachedClient(wonNodeURI);

    if (client == null) {
      client = new OwnerProtocolNeedWebServiceClient(URI.create(wonNodeURI.toURL() + WSDL_LOCATION).toURL());
      cacheClient(wonNodeURI, client);
    }

    return client.getOwnerProtocolOwnerWebServiceEndpointPort();
  }

  public OwnerProtocolNeedWebServiceEndpoint getOwnerProtocolEndpointForNeed(URI needURI) throws NoSuchNeedException, MalformedURLException
  {
    URI needProtocolEndpoint = linkedDataRestClient.getURIPropertyForResource(needURI, WON.HAS_OWNER_PROTOCOL_ENDPOINT);
    if (needProtocolEndpoint == null) throw new NoSuchNeedException(needURI);

    logger.debug("need protocol endpoint of need {} is {}", needURI.toString(), needProtocolEndpoint.toString());

    OwnerProtocolNeedWebServiceClient client = getCachedClient(needProtocolEndpoint);
    if (client == null) {
      URI wsdlURI = URI.create(needProtocolEndpoint.toString() + WSDL_LOCATION);
      client = new OwnerProtocolNeedWebServiceClient(wsdlURI.toURL());
      cacheClient(needProtocolEndpoint, client);
    }

    return client.getOwnerProtocolOwnerWebServiceEndpointPort();
  }

  public OwnerProtocolNeedWebServiceEndpoint getOwnerProtocolEndpointForConnection(URI connectionURI) throws NoSuchConnectionException, MalformedURLException
  {
    URI needProtocolEndpoint = linkedDataRestClient.getURIPropertyForResource(connectionURI, WON.HAS_OWNER_PROTOCOL_ENDPOINT);
    if (needProtocolEndpoint == null) throw new NoSuchConnectionException(connectionURI);

    logger.debug("need protocol endpoint of connection {} is {}", connectionURI.toString(), needProtocolEndpoint.toString());

    OwnerProtocolNeedWebServiceClient client = getCachedClient(connectionURI);
    if (client == null) {
      URI wsdlURI = URI.create(needProtocolEndpoint.toString() + WSDL_LOCATION);
      client = new OwnerProtocolNeedWebServiceClient(wsdlURI.toURL());
      cacheClient(wsdlURI, client);
    }

    return client.getOwnerProtocolOwnerWebServiceEndpointPort();
  }

}

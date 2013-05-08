package won.owner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.owner.service.impl.URIService;
import won.owner.ws.OwnerProtocolNeedWebServiceClient;
import won.protocol.exception.NoSuchConnectionException;
import won.protocol.exception.NoSuchNeedException;
import won.protocol.rest.LinkedDataRestClient;
import won.protocol.vocabulary.WON;
import won.protocol.ws.OwnerProtocolNeedWebServiceEndpoint;

import java.net.MalformedURLException;
import java.net.URI;

/**
 * User: atus
 * Date: 08.05.13
 */
public class OwnerProtocolNeedWebServiceClientFactory implements AbstractOwnerProtocolNeedClientFactory
{
  /* default wsdl location */
  private static final String WSDL_LOCATION = "?wsdl";

  final Logger logger = LoggerFactory.getLogger(getClass());

  private LinkedDataRestClient linkedDataRestClient;
  private URIService uriService;

  public void setLinkedDataRestClient(LinkedDataRestClient linkedDataRestClient)
  {
    this.linkedDataRestClient = linkedDataRestClient;
  }

  public void setUriService(final URIService uriService)
  {
    this.uriService = uriService;
  }

  //TODO: workaround until we can work with multiple WON nodes: protocol URI is hard-coded in spring properties
  public OwnerProtocolNeedWebServiceEndpoint getOwnerProtocolEndpoint(URI wonNodeURI) throws NoSuchNeedException, MalformedURLException
  {
    if(wonNodeURI == null)
     wonNodeURI = uriService.getDefaultOwnerProtocolNeedServiceEndpointURI();

    //TODO: fetch endpoint information for the need and store in db?
    OwnerProtocolNeedWebServiceClient client = new OwnerProtocolNeedWebServiceClient(URI.create(wonNodeURI.toURL() + WSDL_LOCATION).toURL());
    return client.getOwnerProtocolOwnerWebServiceEndpointPort();
  }

  @Override
  public OwnerProtocolNeedWebServiceEndpoint getOwnerProtocolEndpointForNeed(URI needURI) throws NoSuchNeedException, MalformedURLException
  {
    //TODO: fetch endpoint information for the need and store in db?
    URI needProtocolEndpoint = linkedDataRestClient.getURIPropertyForResource(needURI, WON.OWNER_PROTOCOL_ENDPOINT);
    if (needProtocolEndpoint == null) throw new NoSuchNeedException(needURI);

    logger.debug("need protocol endpoint of need {} is {}", needURI.toString(), needProtocolEndpoint.toString());

    URI wsdlURI = URI.create(needProtocolEndpoint.toString() + WSDL_LOCATION);
    OwnerProtocolNeedWebServiceClient client = new OwnerProtocolNeedWebServiceClient(wsdlURI.toURL());

    return client.getOwnerProtocolOwnerWebServiceEndpointPort();
  }

  @Override
  public OwnerProtocolNeedWebServiceEndpoint getOwnerProtocolEndpointForConnection(URI connectionURI) throws NoSuchConnectionException, MalformedURLException
  {
    //TODO: fetch endpoint information for the need and store in db?
    URI needProtocolEndpoint = linkedDataRestClient.getURIPropertyForResource(connectionURI, WON.OWNER_PROTOCOL_ENDPOINT);
    if (needProtocolEndpoint == null) throw new NoSuchConnectionException(connectionURI);

    logger.debug("need protocol endpoint of connection {} is {}", connectionURI.toString(), needProtocolEndpoint.toString());

    URI wsdlURI = URI.create(needProtocolEndpoint.toString() + WSDL_LOCATION);
    OwnerProtocolNeedWebServiceClient client = new OwnerProtocolNeedWebServiceClient(wsdlURI.toURL());

    return client.getOwnerProtocolOwnerWebServiceEndpointPort();
  }
}

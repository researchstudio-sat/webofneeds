package won.node.protocol.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import won.node.ws.NeedProtocolNeedWebServiceClient;
import won.protocol.ws.NeedProtocolNeedWebServiceEndpoint;
import won.protocol.exception.NoSuchConnectionException;
import won.protocol.exception.NoSuchNeedException;
import won.protocol.rest.LinkedDataRestClient;
import won.protocol.vocabulary.WON;
import won.protocol.ws.AbstractClientFactory;

import java.net.MalformedURLException;
import java.net.URI;

/**
 * User: atus
 * Date: 14.05.13
 */
public class NeedProtocolNeedClientFactory extends AbstractClientFactory<NeedProtocolNeedWebServiceClient>
{
  final Logger logger = LoggerFactory.getLogger(getClass());

  @Autowired
  private LinkedDataRestClient linkedDataRestClient;

  public void setLinkedDataRestClient(final LinkedDataRestClient linkedDataRestClient)
  {
    this.linkedDataRestClient = linkedDataRestClient;
  }

  //TODO: switch from linkedDataRestClient to need and connection repositories?
  public NeedProtocolNeedWebServiceEndpoint getNeedProtocolEndpointForNeed(URI needURI) throws NoSuchNeedException, MalformedURLException
  {
    URI needProtocolEndpoint = linkedDataRestClient.getURIPropertyForResource(needURI, WON.NEED_PROTOCOL_ENDPOINT);
    logger.info("need protocol endpoint of need {} is {}", needURI.toString(), needProtocolEndpoint.toString());

    if (needProtocolEndpoint == null) throw new NoSuchNeedException(needURI);

    URI needWsdl = URI.create(needProtocolEndpoint.toString() + "?wsdl");
    NeedProtocolNeedWebServiceClient client = getCachedClient(needWsdl);

    if (client == null) {
      client = new NeedProtocolNeedWebServiceClient(needWsdl.toURL());
      cacheClient(needWsdl, client);
    }

    return client.getNeedProtocolNeedWebServiceEndpointPort();
  }

  public NeedProtocolNeedWebServiceEndpoint getNeedProtocolEndpointForConnection(URI connectionURI) throws NoSuchConnectionException, MalformedURLException
  {
    URI needProtocolEndpoint = linkedDataRestClient.getURIPropertyForResource(connectionURI, WON.NEED_PROTOCOL_ENDPOINT);
    logger.info("need protocol endpoint of connection {} is {}", connectionURI.toString(), needProtocolEndpoint.toString());

    if (needProtocolEndpoint == null) throw new NoSuchConnectionException(connectionURI);

    URI needWsdl = URI.create(needProtocolEndpoint.toString() + "?wsdl");
    NeedProtocolNeedWebServiceClient client = getCachedClient(needWsdl);

    if(client == null) {
      client = new NeedProtocolNeedWebServiceClient(needWsdl.toURL());
      cacheClient(needWsdl, client);
    }

    return client.getNeedProtocolNeedWebServiceEndpointPort();
  }
}
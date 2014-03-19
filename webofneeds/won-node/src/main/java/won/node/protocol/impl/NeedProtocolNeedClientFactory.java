package won.node.protocol.impl;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import won.node.ws.NeedProtocolNeedWebServiceClient;
import won.protocol.exception.NoSuchConnectionException;
import won.protocol.exception.NoSuchNeedException;
import won.protocol.repository.NeedRepository;
import won.protocol.util.RdfUtils;
import won.protocol.util.linkeddata.LinkedDataSource;
import won.protocol.vocabulary.WON;
import won.protocol.ws.AbstractClientFactory;
import won.protocol.ws.NeedProtocolNeedWebServiceEndpoint;

import javax.ws.rs.core.Response;
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
  private LinkedDataSource linkedDataSource;
  private NeedRepository needRepository;

  public void setLinkedDataSource(final LinkedDataSource linkedDataSource)
  {
    this.linkedDataSource = linkedDataSource;
  }

  //TODO: switch from linkedDataRestClient to need and connection repositories?
  public NeedProtocolNeedWebServiceEndpoint getNeedProtocolEndpointForNeed(URI needURI) throws NoSuchNeedException, MalformedURLException
  {
    URI needProtocolEndpoint = getNeedProtocolEndpointURI(needURI);
    if (needProtocolEndpoint == null) throw new NoSuchNeedException(needURI);
    logger.info("need protocol endpoint of need {} is {}", needURI.toString(), needProtocolEndpoint.toString());

    URI needWsdl = URI.create(needProtocolEndpoint.toString() + "?wsdl");
    NeedProtocolNeedWebServiceClient client = getCachedClient(needWsdl);

    if (client == null) {
      client = new NeedProtocolNeedWebServiceClient(needWsdl.toURL());
      cacheClient(needWsdl, client);
    }

    return client.getNeedProtocolNeedWebServiceEndpointPort();
  }

  private URI getNeedProtocolEndpointURI(final URI needURI)
  {
    URI needProtocolEndpoint = null;
    try{
      needProtocolEndpoint = RdfUtils.getURIPropertyForResource(
          linkedDataSource.getModelForResource(needURI),
          needURI,
          WON.HAS_NEED_PROTOCOL_ENDPOINT);
    } catch (UniformInterfaceException e){
      ClientResponse response = e.getResponse();
      if (response.getStatus() == Response.Status.NOT_FOUND.getStatusCode()){
        return null;
      }
      else throw e;
    }

    return needProtocolEndpoint;
  }

  public NeedProtocolNeedWebServiceEndpoint getNeedProtocolEndpointForConnection(URI connectionURI) throws NoSuchConnectionException, MalformedURLException
  {
    URI needProtocolEndpoint = getNeedProtocolEndpointURI(connectionURI);
    if (needProtocolEndpoint == null) throw new NoSuchConnectionException(connectionURI);
    logger.info("need protocol endpoint of need {} is {}", connectionURI.toString(), needProtocolEndpoint.toString());

    URI needWsdl = URI.create(needProtocolEndpoint.toString() + "?wsdl");
    NeedProtocolNeedWebServiceClient client = getCachedClient(needWsdl);

    if(client == null) {
      client = new NeedProtocolNeedWebServiceClient(needWsdl.toURL());
      cacheClient(needWsdl, client);
    }

    return client.getNeedProtocolNeedWebServiceEndpointPort();
  }
    //TODO: change this method so that it gets the URI from RDF triples.




}
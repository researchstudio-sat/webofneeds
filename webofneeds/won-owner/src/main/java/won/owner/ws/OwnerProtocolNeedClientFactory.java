package won.owner.ws;

import com.hp.hpl.jena.shared.impl.PrefixMappingImpl;
import com.hp.hpl.jena.sparql.path.Path;
import com.hp.hpl.jena.sparql.path.PathParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import won.owner.service.impl.URIService;
import won.protocol.exception.NoSuchConnectionException;
import won.protocol.exception.NoSuchNeedException;
import won.protocol.util.RdfUtils;
import won.protocol.util.linkeddata.LinkedDataSource;
import won.protocol.util.linkeddata.WonLinkedDataUtils;
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
  private final String PATH_OWNER_PROTOCOL_ENDPOINT = "<"+WON.SUPPORTS_WON_PROTOCOL_IMPL+">/<"+WON
    .HAS_OWNER_PROTOCOL_ENDPOINT+">";

  @Autowired
  private LinkedDataSource linkedDataSource;

  @Autowired
  private URIService uriService;

  public void setLinkedDataSource(final LinkedDataSource linkedDataSource)
  {
    this.linkedDataSource = linkedDataSource;
  }

  public void setUriService(final URIService uriService)
  {
    this.uriService = uriService;
  }

  public OwnerProtocolNeedWebServiceEndpoint getOwnerProtocolEndpoint(URI wonNodeURI) throws NoSuchNeedException, MalformedURLException
  {
    if (wonNodeURI == null)
      wonNodeURI = uriService.getDefaultOwnerProtocolNeedServiceEndpointURI();
    Path propertyPath =  PathParser.parse(PATH_OWNER_PROTOCOL_ENDPOINT, new PrefixMappingImpl());
    OwnerProtocolNeedWebServiceClient client = getCachedClient(wonNodeURI);

    URI protocolEndpoint = RdfUtils.toURI(WonLinkedDataUtils.getPropertyForURI(wonNodeURI, propertyPath,
      linkedDataSource));

    if (client == null) {
      client = new OwnerProtocolNeedWebServiceClient(URI.create(protocolEndpoint.toURL() + WSDL_LOCATION).toURL());
      cacheClient(wonNodeURI, client);
    }

    return client.getOwnerProtocolOwnerWebServiceEndpointPort();
  }

  public OwnerProtocolNeedWebServiceEndpoint getOwnerProtocolEndpointForNeed(URI needURI) throws NoSuchNeedException, MalformedURLException
  {
    OwnerProtocolNeedWebServiceEndpoint endpoint = getOwnerProtocolEndpointForNeedOrConnection(needURI);
    if (endpoint == null) throw new NoSuchNeedException(needURI);
    return endpoint;
  }

  private OwnerProtocolNeedWebServiceEndpoint getOwnerProtocolEndpointForNeedOrConnection(final URI needURI)
    throws MalformedURLException {
    Path propertyPath =  PathParser.parse(PATH_OWNER_PROTOCOL_ENDPOINT, new PrefixMappingImpl());
    URI protocolEndpoint = RdfUtils.toURI(WonLinkedDataUtils.getWonNodePropertyForNeedOrConnectionURI(
      needURI,
      propertyPath, linkedDataSource
    ));
    if (protocolEndpoint == null) return null;
    logger.debug("need protocol endpoint of need {} is {}", needURI, protocolEndpoint);

    OwnerProtocolNeedWebServiceClient client = getCachedClient(protocolEndpoint);
    if (client == null) {
      URI wsdlURI = URI.create(protocolEndpoint.toString() + WSDL_LOCATION);
      client = new OwnerProtocolNeedWebServiceClient(wsdlURI.toURL());
      cacheClient(protocolEndpoint, client);
    }

    return client.getOwnerProtocolOwnerWebServiceEndpointPort();
  }

  public OwnerProtocolNeedWebServiceEndpoint getOwnerProtocolEndpointForConnection(URI connectionURI) throws NoSuchConnectionException, MalformedURLException
  {
    OwnerProtocolNeedWebServiceEndpoint endpoint = getOwnerProtocolEndpointForNeedOrConnection(connectionURI);
    if (endpoint == null) throw new NoSuchConnectionException(connectionURI);
    return endpoint;
  }

}

package won.matcher.protocol.impl;

import com.hp.hpl.jena.shared.impl.PrefixMappingImpl;
import com.hp.hpl.jena.sparql.path.Path;
import com.hp.hpl.jena.sparql.path.PathParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.matcher.ws.MatcherProtocolNeedWebServiceClient;
import won.protocol.exception.NoSuchNeedException;
import won.protocol.util.RdfUtils;
import won.protocol.util.linkeddata.LinkedDataSource;
import won.protocol.util.linkeddata.WonLinkedDataUtils;
import won.protocol.vocabulary.WON;
import won.protocol.ws.AbstractClientFactory;
import won.protocol.ws.MatcherProtocolNeedWebServiceEndpoint;

import java.net.MalformedURLException;
import java.net.URI;

/**
 * User: atus
 * Date: 22.05.13
 */
public class MatcherProtocolNeedClientFactory extends AbstractClientFactory<MatcherProtocolNeedWebServiceClient>
{
  private final Logger logger = LoggerFactory.getLogger(getClass());
  private final String PATH_MATCHER_PROTOCOL_ENDPOINT = "<"+WON.SUPPORTS_WON_PROTOCOL_IMPL+">/<"+WON
    .HAS_MATCHER_PROTOCOL_ENDPOINT+">";

  private LinkedDataSource linkedDataSource;

  public MatcherProtocolNeedWebServiceEndpoint getMatcherProtocolEndpointForNeed(URI needURI) throws NoSuchNeedException, MalformedURLException
  {
    Path propertyPath =  PathParser.parse(PATH_MATCHER_PROTOCOL_ENDPOINT,new PrefixMappingImpl());
    URI protocolEndpoint = RdfUtils.toURI(WonLinkedDataUtils.getWonNodePropertyForNeedOrConnectionURI(
      needURI,
      propertyPath, linkedDataSource
    ));
    if (protocolEndpoint == null) throw new NoSuchNeedException(needURI);
    logger.debug("need won.matcher.protocol endpoint of need {} is {}", needURI.toString(),
      protocolEndpoint.toString());

    URI needProtocolEndpointUri = URI.create(protocolEndpoint.toString() + "?wsdl");

    MatcherProtocolNeedWebServiceClient client = getCachedClient(needProtocolEndpointUri);
    if (client == null) {
      client = new MatcherProtocolNeedWebServiceClient(needProtocolEndpointUri.toURL());
      cacheClient(needProtocolEndpointUri, client);
    }

    return client.getOwnerProtocolOwnerWebServiceEndpointPort();
  }

  public void setLinkedDataSource(final LinkedDataSource linkedDataSource)
  {
    this.linkedDataSource = linkedDataSource;
  }

}

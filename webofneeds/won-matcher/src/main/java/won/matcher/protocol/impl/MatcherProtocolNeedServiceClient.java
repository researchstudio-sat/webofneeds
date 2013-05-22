package won.matcher.protocol.impl;

import com.hp.hpl.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import won.matcher.ws.MatcherProtocolNeedWebServiceClient;
import won.protocol.exception.IllegalMessageForNeedStateException;
import won.protocol.exception.NoSuchNeedException;
import won.protocol.matcher.MatcherProtocolNeedService;
import won.protocol.rest.LinkedDataRestClient;
import won.protocol.util.RdfUtils;
import won.protocol.vocabulary.WON;
import won.protocol.ws.MatcherProtocolNeedWebServiceEndpoint;

import java.net.MalformedURLException;
import java.net.URI;
import java.text.MessageFormat;

/**
 * User: gabriel
 * Date: 12.02.13
 * Time: 17:26
 */
public class MatcherProtocolNeedServiceClient implements MatcherProtocolNeedService
{
  final Logger logger = LoggerFactory.getLogger(getClass());

  private LinkedDataRestClient linkedDataRestClient;

  @Autowired
  private RdfUtils rdfUtils;

  public void setLinkedDataRestClient(LinkedDataRestClient linkedDataRestClient)
  {
    this.linkedDataRestClient = linkedDataRestClient;
  }

  @Override
  public void hint(URI needURI, URI otherNeed, double score, URI originator, Model content)
      throws NoSuchNeedException, IllegalMessageForNeedStateException
  {
    logger.info(MessageFormat.format("need-facing: HINT called for needURI {0} and otherNeed {1} " +
        "with score {2} from originator {3}.", needURI, otherNeed, score, originator));
    try {
      MatcherProtocolNeedWebServiceEndpoint proxy = getMatcherProtocolEndpointForNeed(needURI);

      proxy.hint(needURI, otherNeed, score, originator, rdfUtils.toString(content));
    } catch (MalformedURLException e) {
      e.printStackTrace();
    }
  }

  private MatcherProtocolNeedWebServiceEndpoint getMatcherProtocolEndpointForNeed(URI needURI) throws NoSuchNeedException, MalformedURLException
  {
    //TODO: fetch endpoint information for the need and store in db?
    URI needProtocolEndpoint = linkedDataRestClient.getURIPropertyForResource(needURI, WON.MATCHER_PROTOCOL_ENDPOINT);
    if (needProtocolEndpoint == null) throw new NoSuchNeedException(needURI);
    logger.info("need won.matcher.protocol endpoint of need {} is {}", needURI.toString(), needProtocolEndpoint.toString());
    MatcherProtocolNeedWebServiceClient client = new MatcherProtocolNeedWebServiceClient(URI.create(needProtocolEndpoint.toString() + "?wsdl").toURL());
    return client.getOwnerProtocolOwnerWebServiceEndpointPort();
  }
}

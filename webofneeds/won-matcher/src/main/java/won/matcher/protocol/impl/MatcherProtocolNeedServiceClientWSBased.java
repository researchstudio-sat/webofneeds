package won.matcher.protocol.impl;

import com.hp.hpl.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import won.protocol.exception.IllegalMessageForNeedStateException;
import won.protocol.exception.NoSuchNeedException;
import won.protocol.matcher.MatcherProtocolNeedServiceClientSide;
import won.protocol.message.WonMessage;
import won.protocol.util.RdfUtils;
import won.protocol.ws.MatcherProtocolNeedWebServiceEndpoint;
import won.protocol.ws.fault.IllegalMessageForNeedStateFault;
import won.protocol.ws.fault.NoSuchNeedFault;

import java.net.MalformedURLException;
import java.net.URI;

/**
 * User: gabriel
 * Date: 12.02.13
 * Time: 17:26
 */
public class MatcherProtocolNeedServiceClientWSBased implements MatcherProtocolNeedServiceClientSide
{
  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Autowired
  private MatcherProtocolNeedClientFactory clientFactory;

  @Override
  public void hint(URI needURI, URI otherNeed, double score, URI originator, Model content, WonMessage wonMessage)
            throws NoSuchNeedException, IllegalMessageForNeedStateException
  {
        logger.info("need-facing: HINT called for needURI {} and otherNeed {} " +
                "with score {} from originator {}.", new Object[]{needURI, otherNeed, score, originator});
    try {
      MatcherProtocolNeedWebServiceEndpoint proxy = clientFactory.getMatcherProtocolEndpointForNeed(needURI);

      proxy.hint(needURI, otherNeed, score, originator, RdfUtils.toString(content));
    } catch (MalformedURLException e) {
      logger.warn("caught MalformedURLException:", e);
    } catch (NoSuchNeedFault noSuchNeedFault) {
      throw NoSuchNeedFault.toException(noSuchNeedFault);
    } catch (IllegalMessageForNeedStateFault illegalMessageForNeedStateFault) {
      throw IllegalMessageForNeedStateFault.toException(illegalMessageForNeedStateFault);
    }
  }

  public void setClientFactory(final MatcherProtocolNeedClientFactory clientFactory)
  {
    this.clientFactory = clientFactory;
  }

  public void initializeDefault() {
  }

}

package won.matcher.protocol.impl;

import java.net.URI;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import won.protocol.matcher.MatcherProtocolNeedServiceClientSide;
import won.protocol.message.WonMessage;

/**
 * User: gabriel Date: 12.02.13 Time: 17:26
 */
public class MatcherProtocolNeedServiceClient implements MatcherProtocolNeedServiceClientSide {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  MatcherProtocolNeedServiceClientSide delegate;

  public void hint(URI needURI, URI otherNeed, double score, URI originator, Model content, WonMessage wonMessage)
      throws Exception {
    logger.info("need-facing: HINT called for needURI {} and otherNeed {} " + "with score {} from originator {}.",
        new Object[] { needURI, otherNeed, score, originator });
    Model facetModel = ModelFactory.createDefaultModel();
    delegate.hint(needURI, otherNeed, score, originator, facetModel, wonMessage);
  }

  public void initializeDefault() {
    // delegate = new MatcherProtocolNeedServiceClientJMSBased();
    delegate.initializeDefault();
  }

  public void setDelegate(MatcherProtocolNeedServiceClientSide delegate) {
    this.delegate = delegate;
  }

}

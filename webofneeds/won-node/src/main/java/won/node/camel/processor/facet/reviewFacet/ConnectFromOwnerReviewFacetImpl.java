package won.node.camel.processor.facet.reviewFacet;

import org.apache.camel.Exchange;
import org.springframework.stereotype.Component;
import won.node.camel.processor.AbstractFromOwnerCamelProcessor;
import won.node.camel.processor.annotation.FacetMessageProcessor;
import won.protocol.vocabulary.WON;
import won.protocol.vocabulary.WONMSG;

/**
 * User: MS Date: 12.12.2018
 */
@Component
@FacetMessageProcessor(facetType = WON.REVIEW_FACET_STRING, direction = WONMSG.TYPE_FROM_OWNER_STRING, messageType = WONMSG.TYPE_CONNECT_STRING)
public class ConnectFromOwnerReviewFacetImpl extends AbstractFromOwnerCamelProcessor {

  @Override
  public void process(final Exchange exchange) {
    logger.debug("default facet implementation, not doing anything");
  }
}

package won.node.camel.processor.facet.reviewFacet;

import org.apache.camel.Exchange;
import org.springframework.stereotype.Component;

import won.node.camel.processor.AbstractCamelProcessor;
import won.node.camel.processor.annotation.DefaultFacetMessageProcessor;
import won.node.camel.processor.annotation.FacetMessageProcessor;
import won.protocol.vocabulary.WON;
import won.protocol.vocabulary.WONMSG;

/**
 * User: MS
 * Date: 12.12.2018
 */
@Component
@DefaultFacetMessageProcessor(direction=WONMSG.TYPE_FROM_EXTERNAL_STRING,messageType = WONMSG.TYPE_CONNECTION_MESSAGE_STRING)
@FacetMessageProcessor(facetType = WON.REVIEW_FACET_STRING,direction=WONMSG.TYPE_FROM_EXTERNAL_STRING,messageType =
  WONMSG.TYPE_CONNECTION_MESSAGE_STRING)
public class SendMessageFromNodeReviewFacetImpl extends AbstractCamelProcessor
{
  @Override
  public void process(final Exchange exchange) {
    logger.debug("default facet implementation, not doing anything");
  }
}

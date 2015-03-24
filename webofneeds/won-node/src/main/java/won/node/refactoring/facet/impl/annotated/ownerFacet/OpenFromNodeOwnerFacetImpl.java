package won.node.refactoring.facet.impl.annotated.ownerFacet;

import org.apache.camel.Exchange;
import org.springframework.stereotype.Component;
import won.node.messaging.processors.AbstractInOnlyMessageProcessor;
import won.node.messaging.processors.DefaultFacetMessageProcessor;
import won.node.messaging.processors.FacetMessageProcessor;
import won.protocol.message.WonMessage;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.model.FacetType;
import won.protocol.vocabulary.WON;
import won.protocol.vocabulary.WONMSG;

/**
 * User: syim
 * Date: 05.03.2015
 */
@Component
@DefaultFacetMessageProcessor(direction=WONMSG.TYPE_FROM_EXTERNAL_STRING,messageType = WONMSG.TYPE_OPEN_STRING)
@FacetMessageProcessor(facetType = WON.OWNER_FACET_STRING,direction=WONMSG.TYPE_FROM_EXTERNAL_STRING,messageType =
  WONMSG.TYPE_OPEN_STRING)
public class OpenFromNodeOwnerFacetImpl extends AbstractInOnlyMessageProcessor
{

  final FacetType facetType = FacetType.OwnerFacet;


  public FacetType getFacetType() {
    return facetType;
  }


  @Override
  public void process(final Exchange exchange) {
    WonMessage wonMessage = (WonMessage) exchange.getIn().getHeader(WonCamelConstants.WON_MESSAGE_HEADER);
    //just send the message
    this.sendMessageToOwner(wonMessage, wonMessage.getReceiverNeedURI());
  }
}

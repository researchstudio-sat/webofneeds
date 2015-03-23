package won.node.refactoring.facet.impl.annotated.ownerFacet;

import org.apache.camel.Exchange;
import org.springframework.stereotype.Component;
import won.node.messaging.processors.AbstractInOnlyMessageProcessor;
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
@FacetMessageProcessor(
        facetType =   WON.OWNER_FACET_STRING,
        direction=    WONMSG.TYPE_FROM_OWNER_STRING,
        messageType = WONMSG.TYPE_CONNECT_STRING)
public class ConnectFromOwnerOwnerFacetImpl extends AbstractInOnlyMessageProcessor
  {

  public FacetType getFacetType() {
    return FacetType.OwnerFacet;
  }

  public void process(Exchange exchange) {
    WonMessage wonMessage = (WonMessage) exchange.getIn().getHeader(WonCamelConstants.WON_MESSAGE_EXCHANGE_HEADER);
    //just send the message
    this.sendMessageToNode(wonMessage, wonMessage.getSenderNeedURI(), wonMessage.getReceiverNeedURI());
  }

}

package won.node.camel.processor.facet.chatFacet;

import org.apache.camel.Exchange;
import org.springframework.stereotype.Component;

import won.node.camel.processor.AbstractFromOwnerCamelProcessor;
import won.node.camel.processor.annotation.DefaultFacetMessageProcessor;
import won.node.camel.processor.annotation.FacetMessageProcessor;
import won.protocol.message.WonMessage;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.vocabulary.WON;
import won.protocol.vocabulary.WONMSG;

/**
 * User: syim
 * Date: 05.03.2015
 */
@Component
@DefaultFacetMessageProcessor(direction=WONMSG.TYPE_FROM_OWNER_STRING,messageType = WONMSG.TYPE_CONNECT_STRING)
@FacetMessageProcessor(
        facetType =   WON.CHAT_FACET_STRING,
        direction=    WONMSG.TYPE_FROM_OWNER_STRING,
        messageType = WONMSG.TYPE_CONNECT_STRING)
public class ConnectFromOwnerChatFacetImpl extends AbstractFromOwnerCamelProcessor
  {
    @Override
    public void process(final Exchange exchange) {
      logger.debug("default facet implementation, not doing anything");
      final WonMessage wonMessage = (WonMessage) exchange.getIn().getHeader(WonCamelConstants.MESSAGE_HEADER);
      logger.debug("message with facet {}", wonMessage.getSenderFacetURI());
    }

}

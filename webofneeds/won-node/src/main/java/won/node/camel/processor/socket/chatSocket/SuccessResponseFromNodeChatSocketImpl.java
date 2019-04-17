package won.node.camel.processor.socket.chatSocket;

import org.apache.camel.Exchange;
import org.springframework.stereotype.Component;
import won.node.camel.processor.AbstractCamelProcessor;
import won.node.camel.processor.annotation.DefaultSocketMessageProcessor;
import won.node.camel.processor.annotation.SocketMessageProcessor;
import won.protocol.vocabulary.WON;
import won.protocol.vocabulary.WONMSG;

/**
 * User: quasarchimaere Date: 04.04.2019
 */
@Component
@DefaultSocketMessageProcessor(direction = WONMSG.FromExternalString, messageType = WONMSG.SuccessResponseString)
@SocketMessageProcessor(socketType = WON.ChatSocketString, direction = WONMSG.FromExternalString, messageType = WONMSG.SuccessResponseString)
public class SuccessResponseFromNodeChatSocketImpl extends AbstractCamelProcessor {
    @Override
    public void process(final Exchange exchange) {
        logger.debug("default socket implementation, not doing anything");
    }
}

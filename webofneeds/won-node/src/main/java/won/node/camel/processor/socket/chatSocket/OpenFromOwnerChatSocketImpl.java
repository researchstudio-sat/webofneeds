package won.node.camel.processor.socket.chatSocket;

import org.apache.camel.Exchange;
import org.springframework.stereotype.Component;
import won.node.camel.processor.AbstractCamelProcessor;
import won.node.camel.processor.annotation.DefaultSocketMessageProcessor;
import won.node.camel.processor.annotation.SocketMessageProcessor;
import won.protocol.vocabulary.WON;
import won.protocol.vocabulary.WONMSG;

/**
 * User: syim Date: 05.03.2015
 */
@Component
@DefaultSocketMessageProcessor(direction = WONMSG.FromOwnerString, messageType = WONMSG.OpenMessageString)
@SocketMessageProcessor(socketType = WON.ChatSocketString, direction = WONMSG.FromOwnerString, messageType = WONMSG.OpenMessageString)
public class OpenFromOwnerChatSocketImpl extends AbstractCamelProcessor {
    @Override
    public void process(final Exchange exchange) {
        logger.debug("default socket implementation, not doing anything");
    }
}

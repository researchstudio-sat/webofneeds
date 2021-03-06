package won.node.camel.processor.socket.chatSocket;

import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import won.node.camel.processor.AbstractCamelProcessor;
import won.node.camel.processor.annotation.DefaultSocketMessageProcessor;
import won.node.camel.processor.annotation.SocketMessageProcessor;
import won.protocol.message.WonMessage;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.vocabulary.WONMSG;
import won.protocol.vocabulary.WXCHAT;

import java.lang.invoke.MethodHandles;

/**
 * User: syim Date: 05.03.2015
 */
@Component
@DefaultSocketMessageProcessor(direction = WONMSG.FromOwnerString, messageType = WONMSG.ConnectionMessageString)
@SocketMessageProcessor(socketType = WXCHAT.ChatSocketString, direction = WONMSG.FromOwnerString, messageType = WONMSG.ConnectionMessageString)
public class SendMessageFromOwnerChatSocketImpl extends AbstractCamelProcessor {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Override
    public void process(final Exchange exchange) {
        logger.debug("default socket implementation, not doing anything");
        final WonMessage wonMessage = (WonMessage) exchange.getIn().getHeader(WonCamelConstants.MESSAGE_HEADER);
        logger.debug("message with socket {}", wonMessage.getSenderSocketURI());
    }
}

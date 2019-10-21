package won.node.camel.processor.fixed;

import java.lang.invoke.MethodHandles;
import java.net.URI;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import won.node.camel.processor.AbstractCamelProcessor;
import won.node.camel.processor.annotation.FixedMessageReactionProcessor;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.message.WonMessageDirection;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.vocabulary.WONMSG;

@Component
@FixedMessageReactionProcessor(direction = WONMSG.FromExternalString, messageType = WONMSG.OpenMessageString)
public class OpenMessageFromNodeReactionProcessor extends AbstractCamelProcessor {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Override
    public void process(Exchange exchange) throws Exception {
        // if the connection's socket isAutoOpen and the connection state is
        // REQUEST_RECEIVED
        Message message = exchange.getIn();
        WonMessage wonMessage = (WonMessage) message.getHeader(WonCamelConstants.MESSAGE_HEADER);
        if (connectionService.shouldSendAutoOpenForOpen(wonMessage)) {
            sendAutoOpenForOpen(wonMessage);
        }
    }

    private void sendAutoOpenForOpen(WonMessage connectMessageToReactTo) {
        URI fromWonNodeURI = connectMessageToReactTo.getRecipientNodeURI();
        URI messageURI = wonNodeInformationService.generateEventURI(fromWonNodeURI);
        WonMessage msg = WonMessageBuilder
                        .setMessagePropertiesForOpen(messageURI, connectMessageToReactTo,
                                        "This is an automatic OPEN message sent by the WoN node")
                        .setWonMessageDirection(WonMessageDirection.FROM_SYSTEM).build();
        logger.debug("sending auto-open for connection {}, reacting to open", msg.getSenderURI());
        super.sendSystemMessage(msg);
    }
}

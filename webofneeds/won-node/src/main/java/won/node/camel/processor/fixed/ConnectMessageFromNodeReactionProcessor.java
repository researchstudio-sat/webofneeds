package won.node.camel.processor.fixed;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import won.node.camel.processor.AbstractCamelProcessor;
import won.node.camel.processor.annotation.FixedMessageReactionProcessor;
import won.node.camel.service.WonCamelHelper;
import won.protocol.message.WonMessage;
import won.protocol.message.builder.WonMessageBuilder;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.vocabulary.WONMSG;

import java.net.URI;

@Component
@FixedMessageReactionProcessor(direction = WONMSG.FromExternalString, messageType = WONMSG.ConnectMessageString)
public class ConnectMessageFromNodeReactionProcessor extends AbstractCamelProcessor {
    Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void process(Exchange exchange) throws Exception {
        // if the connection's socket isAutoOpen, send an open automatically.
        Message message = exchange.getIn();
        WonMessage wonMessage = (WonMessage) message.getHeader(WonCamelConstants.MESSAGE_HEADER);
        if (connectionService.shouldSendAutoOpenForConnect(
                        wonMessage,
                        WonCamelHelper.getWonAclEvaluator(exchange).orElse(null),
                        WonCamelHelper.getWonAclOperationRequest(exchange).orElse(null))) {
            sendAutoOpenForConnect(wonMessage);
        }
    }

    private void sendAutoOpenForConnect(WonMessage connectMessageToReactTo) {
        URI fromWonNodeURI = connectMessageToReactTo.getRecipientNodeURI();
        WonMessage msg = WonMessageBuilder
                        .connect()
                        .sockets().reactingTo(connectMessageToReactTo)
                        .direction().fromSystem()
                        .content().text("Connection request accepted automatically by WoN node")
                        .build();
        logger.info("sending auto-open for connection {}-{}, reacting to connect", msg.getSenderSocketURI(),
                        msg.getRecipientSocketURI());
        camelWonMessageService.sendSystemMessage(msg);
    }
}

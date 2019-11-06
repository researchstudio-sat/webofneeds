package won.node.camel.processor.fixed;

import java.lang.invoke.MethodHandles;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import won.node.camel.processor.AbstractCamelProcessor;
import won.node.camel.processor.WonCamelHelper;
import won.node.camel.processor.annotation.FixedMessageProcessor;
import won.protocol.message.WonMessage;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.model.Connection;
import won.protocol.model.ConnectionState;
import won.protocol.vocabulary.WONMSG;

/**
 * User: syim Date: 02.03.2015
 */
@Component
@FixedMessageProcessor(direction = WONMSG.FromOwnerString, messageType = WONMSG.CloseMessageString)
public class CloseMessageFromOwnerProcessor extends AbstractCamelProcessor {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public void process(final Exchange exchange) throws Exception {
        Message message = exchange.getIn();
        WonMessage wonMessage = (WonMessage) message.getHeader(WonCamelConstants.MESSAGE_HEADER);
        logger.debug("CLOSE received from the owner side for connection {}", wonMessage.getSenderURI());
        Connection con = connectionService.getConnectionRequired(wonMessage.getSenderURI());
        ConnectionState originalState = con.getState();
        con = connectionService.closeFromOwner(wonMessage);
        // if the connection was in suggested state, don't send a close message to the
        // remote atom
        if (originalState == ConnectionState.SUGGESTED) {
            WonCamelHelper.suppressMessageToNode(exchange);
        }
    }
}

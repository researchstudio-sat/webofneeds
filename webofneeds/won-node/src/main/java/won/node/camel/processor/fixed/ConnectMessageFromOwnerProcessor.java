package won.node.camel.processor.fixed;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.springframework.stereotype.Component;

import won.node.camel.processor.AbstractCamelProcessor;
import won.node.camel.processor.WonCamelHelper;
import won.node.camel.processor.annotation.FixedMessageProcessor;
import won.protocol.message.WonMessage;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.model.Connection;
import won.protocol.vocabulary.WONMSG;

/**
 * User: syim Date: 02.03.2015
 */
@Component
@FixedMessageProcessor(direction = WONMSG.FromOwnerString, messageType = WONMSG.ConnectMessageString)
public class ConnectMessageFromOwnerProcessor extends AbstractCamelProcessor {
    public void process(final Exchange exchange) throws Exception {
        Message message = exchange.getIn();
        WonMessage wonMessage = (WonMessage) message.getHeader(WonCamelConstants.MESSAGE_HEADER);
        Connection con = connectionService.connectFromOwner(wonMessage);
        WonCamelHelper.putParentURI(exchange, con.getConnectionURI());
        WonCamelHelper.putConnectionURI(exchange, con.getConnectionURI()); // pick that up in the responder
    }
}

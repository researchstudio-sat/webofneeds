package won.node.camel.processor.fixed;

import org.apache.camel.Exchange;
import org.springframework.stereotype.Component;
import won.node.camel.processor.AbstractCamelProcessor;
import won.node.camel.processor.annotation.FixedMessageProcessor;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageType;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.vocabulary.WONMSG;

/**
 * User: quasarchimaere Date: 03.04.2019
 */
@Component
@FixedMessageProcessor(direction = WONMSG.TYPE_FROM_OWNER_STRING, messageType = WONMSG.TYPE_FAILURE_RESPONSE_STRING)
public class FailureResponseFromExternalProcessor extends AbstractCamelProcessor {
    public void process(final Exchange exchange) throws Exception {
        WonMessage responseMessage = (WonMessage) exchange.getIn().getHeader(WonCamelConstants.MESSAGE_HEADER);
        assert responseMessage != null : "wonMessage header must not be null";
        WonMessageType responseToType = responseMessage.getIsResponseToMessageType();
        // only process failureResponse of connect message
        if (WonMessageType.CONNECT.equals(responseToType)) {
            // TODO: define what to do if the connect fails remotely option: create a system
            // message of type CLOSE,
            // and forward it only to the owner. Add an explanation (a reference to the
            // failure response and some
            // expplanation text.
            logger.warn("The remote end responded with a failure message. Our behaviour is now undefined.");
        }
    }
}

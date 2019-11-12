package won.node.camel.processor.fixed;

import static won.node.camel.service.WonCamelHelper.*;

import java.lang.invoke.MethodHandles;

import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import won.node.camel.processor.AbstractCamelProcessor;
import won.node.camel.processor.annotation.FixedMessageProcessor;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageType;
import won.protocol.vocabulary.WONMSG;

/**
 * User: quasarchimaere Date: 03.04.2019
 */
@Component
@FixedMessageProcessor(direction = WONMSG.FromExternalString, messageType = WONMSG.FailureResponseString)
public class FailureResponseFromExternalProcessor extends AbstractCamelProcessor {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public void process(final Exchange exchange) throws Exception {
        WonMessage responseMessage = getMessageRequired(exchange);
        assert responseMessage != null : "wonMessage header must not be null";
        WonMessageType responseToType = responseMessage.getRespondingToMessageType();
        // TODO: forward to owners and delete delivery chain
        logger.warn("TODO: forward to owners and delete the whole delivery chain");
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

package won.node.camel.processor.fixed;

import java.util.Objects;

import org.apache.camel.Exchange;
import org.springframework.stereotype.Component;

import won.node.camel.processor.AbstractCamelProcessor;
import won.node.camel.processor.annotation.FixedMessageProcessor;
import won.protocol.message.WonMessage;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.vocabulary.WONMSG;

/**
 * User: quasarchimaere Date: 03.04.2019
 */
@Component
@FixedMessageProcessor(direction = WONMSG.FromExternalString, messageType = WONMSG.SuccessResponseString)
public class SuccessResponseFromExternalProcessor extends AbstractCamelProcessor {
    public void process(final Exchange exchange) throws Exception {
        WonMessage responseMessage = (WonMessage) exchange.getIn().getHeader(WonCamelConstants.MESSAGE_HEADER);
        Objects.requireNonNull(responseMessage);
        connectionService.grabRemoteConnectionURIFromRemoteResponse(responseMessage);
    }
}

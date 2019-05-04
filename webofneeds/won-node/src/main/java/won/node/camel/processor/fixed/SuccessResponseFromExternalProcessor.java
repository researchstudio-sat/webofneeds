package won.node.camel.processor.fixed;

import org.apache.camel.Exchange;
import org.springframework.stereotype.Component;
import won.node.camel.processor.AbstractCamelProcessor;
import won.node.camel.processor.annotation.FixedMessageProcessor;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageType;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.model.*;
import won.protocol.vocabulary.WONMSG;

import java.net.URI;
import java.util.Optional;

/**
 * User: quasarchimaere Date: 03.04.2019
 */
@Component
@FixedMessageProcessor(direction = WONMSG.FromExternalString, messageType = WONMSG.SuccessResponseString)
public class SuccessResponseFromExternalProcessor extends AbstractCamelProcessor {
    public void process(final Exchange exchange) throws Exception {
        WonMessage responseMessage = (WonMessage) exchange.getIn().getHeader(WonCamelConstants.MESSAGE_HEADER);
        assert responseMessage != null : "wonMessage header must not be null";
        WonMessageType responseToType = responseMessage.getIsResponseToMessageType();
        // only process successResponse of connect message
        if (WonMessageType.CONNECT.equals(responseToType)) {
            MessageEventPlaceholder mep = this.messageEventRepository
                            .findOneByCorrespondingRemoteMessageURI(responseMessage.getIsResponseToMessageURI());
            // update the connection database: set the remote connection URI just obtained
            // from the response
            Optional<Connection> con = this.connectionRepository.findOneByConnectionURIForUpdate(mep.getSenderURI());
            con.get().setTargetConnectionURI(responseMessage.getSenderURI());
            this.connectionRepository.save(con.get());
        }
    }
}

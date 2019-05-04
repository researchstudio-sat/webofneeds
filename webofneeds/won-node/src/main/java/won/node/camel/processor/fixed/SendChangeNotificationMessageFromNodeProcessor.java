package won.node.camel.processor.fixed;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.springframework.stereotype.Component;
import won.node.camel.processor.AbstractCamelProcessor;
import won.node.camel.processor.annotation.FixedMessageProcessor;
import won.protocol.exception.IllegalMessageForConnectionStateException;
import won.protocol.message.WonMessage;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.message.processor.exception.MissingMessagePropertyException;
import won.protocol.model.Connection;
import won.protocol.model.ConnectionState;
import won.protocol.util.RdfUtils;
import won.protocol.vocabulary.WONMSG;

import java.net.URI;

/**
 * User: syim Date: 02.03.2015
 */
@Component
@FixedMessageProcessor(direction = WONMSG.FromExternalString, messageType = WONMSG.ChangeNotificationMessageString)
public class SendChangeNotificationMessageFromNodeProcessor extends AbstractCamelProcessor {
    public void process(final Exchange exchange) throws Exception {
        Message message = exchange.getIn();
        WonMessage wonMessage = (WonMessage) message.getHeader(WonCamelConstants.MESSAGE_HEADER);
        URI connectionUri = wonMessage.getRecipientURI();
        if (connectionUri == null) {
            throw new MissingMessagePropertyException(URI.create(WONMSG.recipient.toString()));
        }
        Connection con = connectionRepository.findOneByConnectionURIForUpdate(connectionUri).get();
        if (con.getState() != ConnectionState.CONNECTED) {
            throw new IllegalMessageForConnectionStateException(connectionUri, "CHANGE_NOTIFICATION_MESSAGE",
                            con.getState());
        }
        if (logger.isDebugEnabled()) {
            logger.debug("received this ChangeNotificationMessage FromExternal:\n{}",
                            RdfUtils.toString(wonMessage.getCompleteDataset()));
            if (wonMessage.getForwardedMessageURI() != null) {
                logger.debug("This message contains the forwarded message {}", wonMessage.getForwardedMessageURI());
            }
        }
    }
}

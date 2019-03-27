package won.node.camel.processor.fixed;

import java.net.URI;
import java.util.List;
import java.util.Objects;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import won.node.camel.processor.AbstractCamelProcessor;
import won.node.camel.processor.annotation.FixedMessageReactionProcessor;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.model.Connection;
import won.protocol.model.ConnectionState;
import won.protocol.util.LoggingUtils;
import won.protocol.util.RdfUtils;
import won.protocol.util.linkeddata.WonLinkedDataUtils;
import won.protocol.vocabulary.WONMSG;

@Component
@FixedMessageReactionProcessor(direction = WONMSG.TYPE_FROM_EXTERNAL_STRING, messageType = WONMSG.TYPE_CONNECTION_MESSAGE_STRING)
/**
 * If the message has a msg:hasInjectIntoConnection property, try to forward it.
 */
public class SendMessageFromNodeReactionProcessor extends AbstractCamelProcessor {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void process(Exchange exchange) throws Exception {
        Message message = exchange.getIn();
        Objects.nonNull(message);
        WonMessage wonMessage = (WonMessage) message.getHeader(WonCamelConstants.MESSAGE_HEADER);
        Objects.nonNull(wonMessage);
        logger.debug("reacting to ConnectionMessage {}", wonMessage.getMessageURI());
        List<URI> injectTargets = wonMessage.getInjectIntoConnectionURIs();
        if (injectTargets.isEmpty()) {
            logger.debug("no injection attempted - nothing to do for us here");
            return;
        }
        injectTargets.forEach(target -> {
            try {
                // don't inject into the connection we're currently on.
                if (target.equals(wonMessage.getReceiverURI())) {
                    return;
                }
                // only inject into those connections that belong to the receiver need of this
                // message
                Connection con = connectionRepository.findOneByConnectionURI(target);
                if (con.getNeedURI().equals(wonMessage.getReceiverNeedURI())) {
                    forward(wonMessage, con);
                }
            } catch (Exception e) {
                LoggingUtils.logMessageAsInfoAndStacktraceAsDebug(logger, e, "Could not forward message {}",
                                wonMessage.getMessageURI());
            }
        });
    }

    public void forward(WonMessage wonMessage, Connection conToSendTo) {
        if (conToSendTo.getState() != ConnectionState.CONNECTED) {
            return;
        }
        if (logger.isDebugEnabled()) {
            logger.debug("injecting message {} received from need {} to connection {}",
                            new Object[] { wonMessage.getMessageURI(), wonMessage.getSenderNeedURI(),
                                            conToSendTo.getConnectionURI() });
        }
        URI injectedMessageURI = wonNodeInformationService.generateEventURI(wonMessage.getReceiverNodeURI());
        URI remoteWonNodeUri = WonLinkedDataUtils
                        .getWonNodeURIForNeedOrConnectionURI(conToSendTo.getRemoteConnectionURI(), linkedDataSource);
        WonMessage newWonMessage = WonMessageBuilder.forwardReceivedNodeToNodeMessageAsNodeToNodeMessage(
                        injectedMessageURI, wonMessage, conToSendTo.getConnectionURI(), conToSendTo.getNeedURI(),
                        wonMessage.getReceiverNodeURI(), conToSendTo.getRemoteConnectionURI(),
                        conToSendTo.getRemoteNeedURI(), remoteWonNodeUri);
        if (logger.isDebugEnabled()) {
            logger.debug("injecting this message: {} ", RdfUtils.toString(newWonMessage.getCompleteDataset()));
        }
        sendSystemMessage(newWonMessage);
    }
}

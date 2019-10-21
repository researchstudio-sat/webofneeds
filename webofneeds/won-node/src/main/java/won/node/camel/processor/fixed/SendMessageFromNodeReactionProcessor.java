package won.node.camel.processor.fixed;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

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
@FixedMessageReactionProcessor(direction = WONMSG.FromExternalString, messageType = WONMSG.ConnectionMessageString)
/**
 * If the message has a msg:injectIntoConnection property, try to forward it.
 */
public class SendMessageFromNodeReactionProcessor extends AbstractCamelProcessor {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

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
                if (target.equals(wonMessage.getRecipientURI())) {
                    return;
                }
                // only inject into those connections that belong to the receiver atom of this
                // message
                Optional<Connection> con = connectionService.getConnection(target);
                if (con.isPresent()) {
                    if (con.get().getAtomURI().equals(wonMessage.getRecipientAtomURI())) {
                        forward(wonMessage, con.get());
                    }
                } else {
                    logger.debug("Could not inject message into connection {}: no connection found", target);
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
            logger.debug("injecting message {} received from atom {} to connection {}",
                            new Object[] { wonMessage.getMessageURI(), wonMessage.getSenderAtomURI(),
                                            conToSendTo.getConnectionURI() });
        }
        URI injectedMessageURI = wonNodeInformationService.generateEventURI(wonMessage.getRecipientNodeURI());
        URI remoteWonNodeUri = WonLinkedDataUtils
                        .getWonNodeURIForAtomOrConnectionURI(conToSendTo.getTargetConnectionURI(), linkedDataSource);
        WonMessage newWonMessage = WonMessageBuilder.forwardReceivedNodeToNodeMessageAsNodeToNodeMessage(
                        injectedMessageURI, wonMessage, conToSendTo.getConnectionURI(), conToSendTo.getAtomURI(),
                        wonMessage.getRecipientNodeURI(), conToSendTo.getTargetConnectionURI(),
                        conToSendTo.getTargetAtomURI(), remoteWonNodeUri);
        if (logger.isDebugEnabled()) {
            logger.debug("injecting this message: {} ", RdfUtils.toString(newWonMessage.getCompleteDataset()));
        }
        sendSystemMessage(newWonMessage);
    }
}

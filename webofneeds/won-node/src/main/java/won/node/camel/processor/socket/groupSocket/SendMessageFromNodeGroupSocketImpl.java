package won.node.camel.processor.socket.groupSocket;

import static won.node.camel.service.WonCamelHelper.*;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.List;
import java.util.Optional;

import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import won.node.camel.processor.AbstractCamelProcessor;
import won.node.camel.processor.annotation.SocketMessageProcessor;
import won.protocol.message.WonMessage;
import won.protocol.message.builder.WonMessageBuilder;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.model.Connection;
import won.protocol.model.ConnectionState;
import won.protocol.repository.ConnectionRepository;
import won.protocol.util.WonRdfUtils;
import won.protocol.util.linkeddata.WonLinkedDataUtils;
import won.protocol.vocabulary.WONMSG;
import won.protocol.vocabulary.WXGROUP;

/**
 * Created with IntelliJ IDEA. User: gabriel Date: 16.09.13 Time: 18:42 To
 * change this template use File | Settings | File Templates.
 */
@Component
@SocketMessageProcessor(socketType = WXGROUP.GroupSocketString, direction = WONMSG.FromExternalString, messageType = WONMSG.ConnectionMessageString)
public class SendMessageFromNodeGroupSocketImpl extends AbstractCamelProcessor {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    @Autowired
    private ConnectionRepository connectionRepository;

    @Override
    public void process(final Exchange exchange) throws Exception {
        final WonMessage wonMessage = (WonMessage) exchange.getIn().getHeader(WonCamelConstants.MESSAGE_HEADER);
        // whatever happens, this message is not sent to the owner:
        exchange.getIn().setHeader(WonCamelConstants.SUPPRESS_MESSAGE_TO_OWNER_HEADER, Boolean.TRUE);
        // avoid message duplication in larger group networks:
        // it is possible that we have already processed this message
        // and through connected groups it was forwarded back to us.
        // if this is the case, we drop the message
        // we check it by comparing the innermost message uri to that of any
        // message we have processed so far.
        // now check if we processed the message earlier
        URI parent = getParentURIRequired(exchange);
        for (URI forwarded : wonMessage.getForwardedMessageURIs()) {
            if (messageEventRepository.findOneByMessageURIAndParentURI(
                            forwarded, parent).isPresent()) {
                if (logger.isDebugEnabled()) {
                    URI groupUri = wonMessage.getRecipientAtomURI();
                    logger.debug("suppressing message {} " + "as its innermost message is {} which has already "
                                    + "been processed by group {}",
                                    new Object[] { wonMessage.getMessageURI(), forwarded, groupUri });
                }
                return;
            }
        }
        final Optional<Connection> conOfIncomingMessage = connectionRepository.findOneBySocketURIAndTargetSocketURI(
                        wonMessage.getRecipientSocketURIRequired(), wonMessage.getSenderSocketURIRequired());
        final List<Connection> consInGroup = connectionRepository
                        .findBySocketURIAndState(conOfIncomingMessage.get().getSocketURI(), ConnectionState.CONNECTED);
        if (consInGroup == null || consInGroup.size() < 2)
            return;
        if (logger.isDebugEnabled()) {
            logger.debug("processing message {} received from atom {} in group {} - preparing to send it to {} group members (text message: '{}'}",
                            new Object[] { wonMessage.getMessageURI(), wonMessage.getSenderAtomURI(),
                                            wonMessage.getRecipientAtomURI(), consInGroup.size() - 1,
                                            WonRdfUtils.MessageUtils.getTextMessage(wonMessage) });
        }
        for (final Connection conToSendTo : consInGroup) {
            try {
                if (!conToSendTo.equals(conOfIncomingMessage)) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("forwarding message {} received from atom {} in group {} to group member {}",
                                        new Object[] { wonMessage.getMessageURI(), wonMessage.getSenderAtomURI(),
                                                        wonMessage.getRecipientAtomURI(),
                                                        conToSendTo.getTargetAtomURI() });
                    }
                    URI remoteWonNodeUri = WonLinkedDataUtils.getWonNodeURIForAtomOrConnectionURI(
                                    conToSendTo.getTargetConnectionURI(), linkedDataSource);
                    WonMessage newWonMessage = WonMessageBuilder
                                    .connectionMessage()
                                    .direction()
                                    /**/.fromSystem()
                                    .forward(wonMessage)
                                    .sockets()
                                    /**/.sender(conToSendTo.getSocketURI())
                                    /**/.recipient(conToSendTo.getTargetSocketURI())
                                    .build();
                    camelWonMessageService.sendSystemMessage(newWonMessage);
                }
            } catch (Exception e) {
                logger.warn("caught Exception:", e);
            }
        }
    }
}

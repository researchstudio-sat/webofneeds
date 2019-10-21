package won.node.camel.processor.fixed;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.Optional;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import won.node.camel.processor.AbstractCamelProcessor;
import won.node.camel.processor.annotation.FixedMessageProcessor;
import won.node.camel.processor.general.OutboundMessageFactoryProcessor;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.message.processor.exception.WonMessageProcessingException;
import won.protocol.model.Connection;
import won.protocol.util.WonRdfUtils;
import won.protocol.vocabulary.WONMSG;

/**
 * User: syim Date: 02.03.2015
 */
@Component
@FixedMessageProcessor(direction = WONMSG.FromOwnerString, messageType = WONMSG.OpenMessageString)
public class OpenMessageFromOwnerProcessor extends AbstractCamelProcessor {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public void process(final Exchange exchange) throws Exception {
        Message message = exchange.getIn();
        WonMessage wonMessage = (WonMessage) message.getHeader(WonCamelConstants.MESSAGE_HEADER);
        logger.debug("OPEN received from the owner side for connection {}", wonMessage.getSenderURI());
        Connection con = connectionService.openFromOwner(wonMessage);
        URI remoteMessageUri = wonNodeInformationService.generateEventURI(wonMessage.getRecipientNodeURI());
        Optional<URI> userDefinedTargetSocketURI = Optional
                        .ofNullable(WonRdfUtils.SocketUtils.getTargetSocket(wonMessage));
        Optional<URI> userDefinedSocketURI = Optional.ofNullable(WonRdfUtils.SocketUtils.getSocket(wonMessage));
        // add the sockets to the message if necessary
        if (!userDefinedSocketURI.isPresent()) {
            // the user did not specify a socket uri. we have to add it
            wonMessage.addMessageProperty(WONMSG.senderSocket, con.getSocketURI());
        }
        if (!userDefinedTargetSocketURI.isPresent()) {
            // the user did not specify a remote uri. we have to add it
            wonMessage.addMessageProperty(WONMSG.recipientSocket, con.getTargetSocketURI());
        }
        // add the information about the corresponding message to the local one
        wonMessage.addMessageProperty(WONMSG.correspondingRemoteMessage, remoteMessageUri);
        // the persister will pick it up later
        // put the factory into the outbound message factory header. It will be used to
        // generate the outbound message
        // after the wonMessage has been processed and saved, to make sure that the
        // outbound message contains
        // all the data that we also store locally
        OutboundMessageFactory outboundMessageFactory = new OutboundMessageFactory(remoteMessageUri, con);
        exchange.getIn().setHeader(WonCamelConstants.OUTBOUND_MESSAGE_FACTORY_HEADER, outboundMessageFactory);
    }

    private class OutboundMessageFactory extends OutboundMessageFactoryProcessor {
        private final Connection connection;

        public OutboundMessageFactory(URI messageURI, Connection connection) {
            super(messageURI);
            this.connection = connection;
        }

        @Override
        public WonMessage process(WonMessage message) throws WonMessageProcessingException {
            // create the message to send to the remote node
            return WonMessageBuilder.setPropertiesForPassingMessageToRemoteNode(message, getMessageURI()).build();
        }
    }
}

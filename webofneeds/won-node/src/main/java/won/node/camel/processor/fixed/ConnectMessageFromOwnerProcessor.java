package won.node.camel.processor.fixed;

import java.net.URI;
import java.util.Optional;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
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
@FixedMessageProcessor(direction = WONMSG.FromOwnerString, messageType = WONMSG.ConnectMessageString)
public class ConnectMessageFromOwnerProcessor extends AbstractCamelProcessor {
    public void process(final Exchange exchange) throws Exception {
        Message message = exchange.getIn();
        WonMessage wonMessage = (WonMessage) message.getHeader(WonCamelConstants.MESSAGE_HEADER);
        Connection con = connectionService.connectFromOwner(wonMessage);
        Optional<URI> userDefinedSocketURI = Optional.ofNullable(WonRdfUtils.SocketUtils.getSocket(wonMessage));
        Optional<URI> userDefinedTargetSocketURI = Optional
                        .ofNullable(WonRdfUtils.SocketUtils.getTargetSocket(wonMessage));
        // prepare the message to pass to the remote node
        URI remoteMessageUri = wonNodeInformationService.generateEventURI(wonMessage.getRecipientNodeURI());
        // set the sender uri in the envelope TODO: TwoMsgs: do not set sender here
        wonMessage.addMessageProperty(WONMSG.sender, con.getConnectionURI());
        // add the information about the new local connection to the original message
        wonMessage.addMessageProperty(WONMSG.correspondingRemoteMessage, remoteMessageUri);
        // the persister will pick it up later
        // add the sockets to the message if necessary
        if (!userDefinedSocketURI.isPresent()) {
            // the user did not specify a socket uri. we have to add it
            wonMessage.addMessageProperty(WONMSG.senderSocket, con.getSocketURI());
        }
        if (!userDefinedTargetSocketURI.isPresent()) {
            // the user did not specify a remote uri. we have to add it
            wonMessage.addMessageProperty(WONMSG.recipientSocket, con.getTargetSocketURI());
        }
        // put the factory into the outbound message factory header. It will be used to
        // generate the outbound message
        // after the wonMessage has been processed and saved, to make sure that the
        // outbound message contains
        // all the data that we also store locally
        OutboundMessageFactory outboundMessageFactory = new OutboundMessageFactory(remoteMessageUri, con);
        message.setHeader(WonCamelConstants.OUTBOUND_MESSAGE_FACTORY_HEADER, outboundMessageFactory);
    }

    private class OutboundMessageFactory extends OutboundMessageFactoryProcessor {
        private final Connection connection;

        public OutboundMessageFactory(URI messageURI, Connection connection) {
            super(messageURI);
            this.connection = connection;
        }

        @Override
        public WonMessage process(WonMessage message) throws WonMessageProcessingException {
            return WonMessageBuilder.setPropertiesForPassingMessageToRemoteNode(message, getMessageURI())
                            .setSenderURI(connection.getConnectionURI()).build();
        }
    }
}

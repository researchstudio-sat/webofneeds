package won.node.camel.processor.fixed;

import java.net.URI;
import java.util.Objects;
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
import won.protocol.model.ConnectionEventType;
import won.protocol.util.WonRdfUtils;
import won.protocol.util.linkeddata.WonLinkedDataUtils;
import won.protocol.vocabulary.WONMSG;

/**
 * User: syim Date: 02.03.2015
 */
@Component
@FixedMessageProcessor(direction = WONMSG.FromOwnerString, messageType = WONMSG.OpenMessageString)
public class OpenMessageFromOwnerProcessor extends AbstractCamelProcessor {
    public void process(final Exchange exchange) throws Exception {
        Message message = exchange.getIn();
        WonMessage wonMessage = (WonMessage) message.getHeader(WonCamelConstants.MESSAGE_HEADER);
        logger.debug("OPEN received from the owner side for connection {}", wonMessage.getSenderURI());
        Connection con = connectionRepository.findOneByConnectionURI(wonMessage.getSenderURI());
        Objects.requireNonNull(con);
        Objects.requireNonNull(con.getTargetAtomURI());
        if (!con.getTargetAtomURI().equals(wonMessage.getRecipientAtomURI()))
            throw new IllegalStateException("remote atom uri must be equal to receiver atom uri");
        if (con.getConnectionURI() == null)
            throw new IllegalStateException("connection uri must not be null");
        if (con.getSocketURI() == null)
            throw new IllegalStateException("connection's socket uri must not be null");
        if (!con.getConnectionURI().equals(wonMessage.getSenderURI()))
            throw new IllegalStateException("connection uri must be equal to sender uri");
        if (wonMessage.getRecipientURI() != null) {
            if (!wonMessage.getRecipientURI().equals(con.getTargetConnectionURI())) {
                throw new IllegalStateException("remote connection uri must be equal to receiver uri");
            }
        }
        // sockets: the remote socket in the connection may be null before the open.
        // check if the owner sent a remote socket. there must not be a clash
        Optional<URI> userDefinedTargetSocketURI = Optional
                        .ofNullable(WonRdfUtils.SocketUtils.getTargetSocket(wonMessage));
        Optional<URI> userDefinedSocketURI = Optional.ofNullable(WonRdfUtils.SocketUtils.getSocket(wonMessage));
        failIfIsNotSocketOfAtom(userDefinedSocketURI, Optional.of(wonMessage.getSenderAtomURI()));
        failIfIsNotSocketOfAtom(userDefinedTargetSocketURI, Optional.of(wonMessage.getRecipientAtomURI()));
        Optional<URI> connectionsTargetSocketURI = Optional.ofNullable(con.getTargetSocketURI());
        // check remote socket info
        if (userDefinedTargetSocketURI.isPresent()) {
            if (connectionsTargetSocketURI.isPresent()) {
                if (!userDefinedTargetSocketURI.get().equals(connectionsTargetSocketURI.get())) {
                    throw new IllegalArgumentException(
                                    "Cannot process OPEN FROM_OWNER: remote socket uri clashes with value already set in connection");
                }
            } else {
                // use the one from the message
                con.setTargetSocketURI(userDefinedTargetSocketURI.get());
            }
        } else {
            // check if neither the message nor the connection have a remote socket set
            if (!connectionsTargetSocketURI.isPresent()) {
                // none defined at all: look up default remote socket
                con.setTargetSocketURI(lookupDefaultSocket(con.getTargetAtomURI()));
            }
        }
        failForIncompatibleSockets(con.getSocketURI(), con.getTypeURI(), con.getTargetSocketURI());
        con.setState(con.getState().transit(ConnectionEventType.OWNER_OPEN));
        connectionRepository.save(con);
        URI remoteMessageUri = wonNodeInformationService.generateEventURI(wonMessage.getRecipientNodeURI());
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

    private URI lookupDefaultSocket(URI atomURI) {
        // look up the default socket and use that one
        return WonLinkedDataUtils.getDefaultSocket(atomURI, true, linkedDataSource)
                        .orElseThrow(() -> new IllegalStateException("No default socket found on " + atomURI));
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

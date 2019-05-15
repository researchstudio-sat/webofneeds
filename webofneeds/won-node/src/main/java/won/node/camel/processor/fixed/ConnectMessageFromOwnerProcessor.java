package won.node.camel.processor.fixed;

import java.net.URI;
import java.util.Optional;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.springframework.stereotype.Component;
import won.node.camel.processor.AbstractCamelProcessor;
import won.node.camel.processor.annotation.FixedMessageProcessor;
import won.node.camel.processor.general.OutboundMessageFactoryProcessor;
import won.protocol.exception.NoSuchConnectionException;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.message.processor.exception.WonMessageProcessingException;
import won.protocol.model.Connection;
import won.protocol.model.ConnectionEventType;
import won.protocol.model.ConnectionState;
import won.protocol.model.Socket;
import won.protocol.util.WonRdfUtils;
import won.protocol.util.linkeddata.WonLinkedDataUtils;
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
        URI senderAtomURI = wonMessage.getSenderAtomURI();
        URI senderNodeURI = wonMessage.getSenderNodeURI();
        URI recipientAtomURI = wonMessage.getRecipientAtomURI();
        // this is a connect from owner. We allow owners to omit sockets for ease of
        // use.
        // If local or remote sockets were not specified, we define them now.
        Optional<URI> userDefinedSocketURI = Optional.ofNullable(WonRdfUtils.SocketUtils.getSocket(wonMessage));
        failIfIsNotSocketOfAtom(userDefinedSocketURI, Optional.of(senderAtomURI));
        Optional<URI> userDefinedTargetSocketURI = Optional
                        .ofNullable(WonRdfUtils.SocketUtils.getTargetSocket(wonMessage));
        failIfIsNotSocketOfAtom(userDefinedTargetSocketURI, Optional.of(recipientAtomURI));
        Optional<URI> connectionURI = Optional.ofNullable(wonMessage.getSenderURI()); // if the uri is known already, we
                                                                                      // can
                                                                                      // load the connection!
        Optional<Connection> con;
        if (connectionURI.isPresent()) {
            // we know the connection: load it
            con = connectionRepository.findOneByConnectionURIForUpdate(connectionURI.get());
            if (!con.isPresent())
                throw new NoSuchConnectionException(connectionURI.get());
            // however, if the sockets don't match, we report an error:
            if (userDefinedSocketURI.isPresent() && !userDefinedSocketURI.equals(con.get().getSocketURI())) {
                throw new IllegalStateException(
                                "Cannot process CONNECT message FROM_OWNER. Specified socket uri conflicts with existing connection data");
            }
            // remote socket uri: may be set on the connection, in which case we may have a
            // conflict
            if (con.get().getTargetSocketURI() != null && userDefinedTargetSocketURI != null
                            && !con.get().getTargetSocketURI().equals(userDefinedTargetSocketURI)) {
                throw new IllegalStateException(
                                "Cannot process CONNECT message FROM_OWNER. Specified remote socket uri conflicts with existing connection data");
            }
            // if the remote socket is not yet set on the connection, we have to set it now.
            if (con.get().getTargetSocketURI() == null) {
                con.get().setTargetSocketURI(
                                userDefinedTargetSocketURI.orElse(lookupDefaultSocket(con.get().getTargetAtomURI())));
            }
            // sockets are set in the connection now.
        } else {
            // we did not know about this connection. try to find out if one exists that we
            // can use
            // the effect of connect should not be surprising. either use specified sockets
            // (if they are) or use default sockets.
            // don't try to be clever and look for suggested connections with other sockets
            // because that leads
            // consecutive connects opening connections between different sockets
            //
            // hence, we can determine our sockets now, before looking at what's there.
            Socket actualSocket = dataService.getSocket(senderAtomURI, userDefinedSocketURI);
            Optional<URI> actualSocketURI = Optional.of(actualSocket.getSocketURI());
            Optional<URI> actualTargetSocketURI = Optional
                            .of(userDefinedTargetSocketURI.orElse(lookupDefaultSocket(recipientAtomURI)));
            con = connectionRepository.findOneByAtomURIAndTargetAtomURIAndSocketURIAndTargetSocketURIForUpdate(
                            senderAtomURI, recipientAtomURI, actualSocketURI.get(), actualTargetSocketURI.get());
            if (!con.isPresent()) {
                // did not find such a connection. It could be the connection exists, but
                // without a remote socket
                con = connectionRepository.findOneByAtomURIAndTargetAtomURIAndSocketURIAndNullTargetSocketForUpdate(
                                senderAtomURI, recipientAtomURI, actualSocketURI.get());
                if (con.isPresent()) {
                    // we found a connection without a remote socket uri. we use this one and we'll
                    // have to set the remote socket uri.
                    con.get().setTargetSocketURI(actualTargetSocketURI.get());
                } else {
                    // did not find such a connection either. We can safely create a new one
                    // create Connection in Database
                    URI connectionUri = wonNodeInformationService.generateConnectionURI(senderNodeURI);
                    con = Optional.of(dataService.createConnection(connectionUri, senderAtomURI, recipientAtomURI, null,
                                    actualSocket.getSocketURI(), actualSocket.getTypeURI(), actualTargetSocketURI.get(),
                                    ConnectionState.REQUEST_SENT, ConnectionEventType.OWNER_OPEN));
                }
            }
        }
        failForExceededCapacity(con.get().getSocketURI());
        failForIncompatibleSockets(con.get().getSocketURI(), con.get().getTargetSocketURI());
        // state transiation
        con.get().setState(con.get().getState().transit(ConnectionEventType.OWNER_OPEN));
        connectionRepository.save(con.get());
        // prepare the message to pass to the remote node
        URI remoteMessageUri = wonNodeInformationService.generateEventURI(wonMessage.getRecipientNodeURI());
        // set the sender uri in the envelope TODO: TwoMsgs: do not set sender here
        wonMessage.addMessageProperty(WONMSG.sender, con.get().getConnectionURI());
        // add the information about the new local connection to the original message
        wonMessage.addMessageProperty(WONMSG.correspondingRemoteMessage, remoteMessageUri);
        // the persister will pick it up later
        // add the sockets to the message if necessary
        if (!userDefinedSocketURI.isPresent()) {
            // the user did not specify a socket uri. we have to add it
            wonMessage.addMessageProperty(WONMSG.senderSocket, con.get().getSocketURI());
        }
        if (!userDefinedTargetSocketURI.isPresent()) {
            // the user did not specify a remote uri. we have to add it
            wonMessage.addMessageProperty(WONMSG.recipientSocket, con.get().getTargetSocketURI());
        }
        // put the factory into the outbound message factory header. It will be used to
        // generate the outbound message
        // after the wonMessage has been processed and saved, to make sure that the
        // outbound message contains
        // all the data that we also store locally
        OutboundMessageFactory outboundMessageFactory = new OutboundMessageFactory(remoteMessageUri, con.get());
        message.setHeader(WonCamelConstants.OUTBOUND_MESSAGE_FACTORY_HEADER, outboundMessageFactory);
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
            return WonMessageBuilder.setPropertiesForPassingMessageToRemoteNode(message, getMessageURI())
                            .setSenderURI(connection.getConnectionURI()).build();
        }
    }
}

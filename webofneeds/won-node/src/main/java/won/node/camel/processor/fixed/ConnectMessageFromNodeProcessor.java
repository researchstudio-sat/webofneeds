package won.node.camel.processor.fixed;

import java.net.URI;
import java.util.Optional;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.springframework.stereotype.Component;

import won.node.camel.processor.AbstractCamelProcessor;
import won.node.camel.processor.annotation.FixedMessageProcessor;
import won.protocol.exception.NoSuchConnectionException;
import won.protocol.message.WonMessage;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.message.processor.exception.MissingMessagePropertyException;
import won.protocol.model.Connection;
import won.protocol.model.ConnectionEventType;
import won.protocol.model.ConnectionState;
import won.protocol.model.Socket;
import won.protocol.util.WonRdfUtils;
import won.protocol.vocabulary.WONMSG;

@Component
@FixedMessageProcessor(direction = WONMSG.FromExternalString, messageType = WONMSG.ConnectMessageString)
public class ConnectMessageFromNodeProcessor extends AbstractCamelProcessor {
    public void process(final Exchange exchange) throws Exception {
        Message message = exchange.getIn();
        WonMessage wonMessage = (WonMessage) message.getHeader(WonCamelConstants.MESSAGE_HEADER);
        // an atom wants to connect.
        // get the required data from the message and create a connection
        URI atomUri = wonMessage.getRecipientAtomURI();
        URI wonNodeUriFromWonMessage = wonMessage.getRecipientNodeURI();
        URI targetAtomUri = wonMessage.getSenderAtomURI();
        URI targetConnectionUri = wonMessage.getSenderURI();
        URI socketURI = WonRdfUtils.SocketUtils.getSocket(wonMessage);
        if (socketURI == null) {
            throw new IllegalArgumentException("cannot process FROM_EXTERNAL connect without recipientSocketURI");
        }
        failIfIsNotSocketOfAtom(Optional.of(socketURI), Optional.of(atomUri));
        Socket socket = dataService.getSocket(atomUri, socketURI == null ? Optional.empty() : Optional.of(socketURI));
        URI connectionURI = wonMessage.getRecipientURI(); // if the uri is known already, we can load the connection!
        // the remote socket must be specified in a message coming from another node
        URI targetSocketURI = WonRdfUtils.SocketUtils.getTargetSocket(wonMessage);
        failIfIsNotSocketOfAtom(Optional.of(targetSocketURI), Optional.of(targetAtomUri));
        // we complain about socket, not targetSocket, because it's a remote
        // message!
        if (targetSocketURI == null)
            throw new MissingMessagePropertyException(URI.create(WONMSG.recipientSocket.toString()));
        if (targetConnectionUri == null)
            throw new MissingMessagePropertyException(URI.create(WONMSG.sender.getURI().toString()));
        Connection con = null;
        if (connectionURI != null) {
            // we already knew about this connection. load it
            con = connectionRepository.findOneByConnectionURIForUpdate(connectionURI).get();
            if (con == null)
                throw new NoSuchConnectionException(connectionURI);
            if (con.getTargetConnectionURI() != null && !targetConnectionUri.equals(con.getTargetConnectionURI())) {
                throw new IllegalStateException(
                                "Cannot process CONNECT message FROM_EXTERNAL. Specified connection uris conflict with existing connection data");
            }
            if (con.getTargetSocketURI() != null && !targetSocketURI.equals(con.getTargetSocketURI())) {
                throw new IllegalStateException(
                                "Cannot process CONNECT message FROM_EXTERNAL. Specified socket uris conflict with existing connection data");
            }
        } else {
            // the sender did not know about our connection. try to find out if one exists
            // that we can use
            // we know which remote socket to connect to. There may be a connection with
            // that information already, either because the hint pointed to the remote
            // socket or because the connection is already in a different state and this
            // is a duplicate connect..
            Optional<Connection> conOpt = connectionRepository
                            .findOneByAtomURIAndTargetAtomURIAndSocketURIAndTargetSocketURIForUpdate(atomUri,
                                            targetAtomUri, socket.getSocketURI(), targetSocketURI);
            if (conOpt.isPresent()) {
                con = conOpt.get();
            } else {
                // did not find such a connection. It could be that the connection exists, but
                // without a remote socket
                conOpt = connectionRepository.findOneByAtomURIAndTargetAtomURIAndSocketURIAndNullTargetSocketForUpdate(
                                atomUri, targetAtomUri, socket.getSocketURI());
                if (conOpt.isPresent()) {
                    // we found a connection without a remote socket uri. we use this one and we'll
                    // have to set the remote socket uri.
                    con = conOpt.get();
                } else {
                    // did not find such a connection either. We can safely create a new one. (see
                    // below)
                }
            }
        }
        failForExceededCapacity(socket.getSocketURI());
        failForIncompatibleSockets(socket.getSocketURI(), targetSocketURI);
        if (con == null) {
            // create Connection in Database
            URI connectionUri = wonNodeInformationService.generateConnectionURI(wonNodeUriFromWonMessage);
            con = dataService.createConnection(connectionUri, atomUri, targetAtomUri, targetConnectionUri,
                            socket.getSocketURI(), socket.getTypeURI(), targetSocketURI,
                            ConnectionState.REQUEST_RECEIVED, ConnectionEventType.PARTNER_OPEN);
        }
        con.setTargetConnectionURI(targetConnectionUri);
        con.setTargetSocketURI(targetSocketURI);
        con.setState(con.getState().transit(ConnectionEventType.PARTNER_OPEN));
        connectionRepository.save(con);
        // set the receiver to the newly generated connection uri
        wonMessage.addMessageProperty(WONMSG.recipient, con.getConnectionURI());
    }
}

package won.node.camel.processor.fixed;

import java.net.URI;
import java.util.Optional;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.springframework.stereotype.Component;

import won.node.camel.processor.AbstractCamelProcessor;
import won.node.camel.processor.annotation.FixedMessageProcessor;
import won.protocol.message.WonMessage;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.model.Connection;
import won.protocol.model.ConnectionEventType;
import won.protocol.model.ConnectionState;
import won.protocol.model.Socket;
import won.protocol.util.WonRdfUtils;
import won.protocol.vocabulary.WONMSG;

/**
 * User: syim Date: 02.03.2015
 */
@Component
@FixedMessageProcessor(direction = WONMSG.FromExternalString, messageType = WONMSG.OpenMessageString)
public class OpenMessageFromNodeProcessor extends AbstractCamelProcessor {
    public void process(final Exchange exchange) throws Exception {
        Message message = exchange.getIn();
        WonMessage wonMessage = (WonMessage) message.getHeader(WonCamelConstants.MESSAGE_HEADER);
        Optional<URI> connectionURIFromWonMessage = Optional.ofNullable(wonMessage.getRecipientURI());
        Optional<Connection> con = Optional.empty();
        if (!connectionURIFromWonMessage.isPresent()) {
            // the opener didn't know about the connection
            // this happens, for example, when both parties get a hint. Both create a
            // connection, but they don't know
            // about each other.
            // That's why we first try to find a connection with the same atoms and socket:
            // let's extract the socket, we'll atom it multiple times here.
            // As the call is coming from the node, it must be present
            // (the node fills it in if the owner leaves it out)
            Optional<URI> socketURI = Optional.of(WonRdfUtils.SocketUtils.getSocket(wonMessage));
            failIfIsNotSocketOfAtom(socketURI, Optional.of(wonMessage.getRecipientAtomURI()));
            Optional<URI> targetSocketURI = Optional.of(WonRdfUtils.SocketUtils.getTargetSocket(wonMessage));
            failIfIsNotSocketOfAtom(targetSocketURI, Optional.of(wonMessage.getSenderAtomURI()));
            if (!socketURI.isPresent())
                throw new IllegalArgumentException(
                                "Cannot process OPEN FROM_EXTERNAl as no socket information is present");
            if (!targetSocketURI.isPresent())
                throw new IllegalArgumentException(
                                "Cannot process OPEN FROM_EXTERNAl as no remote socket information is present");
            con = connectionRepository.findOneByAtomURIAndTargetAtomURIAndSocketURIAndTargetSocketURIForUpdate(
                            wonMessage.getRecipientAtomURI(), wonMessage.getSenderAtomURI(), socketURI.get(),
                            targetSocketURI.get());
            if (!con.isPresent()) {
                // maybe we did not know about the targetsocket yet. let's try that:
                con = connectionRepository.findOneByAtomURIAndTargetAtomURIAndSocketURIAndNullTargetSocketForUpdate(
                                wonMessage.getRecipientAtomURI(), wonMessage.getSenderAtomURI(), socketURI.get());
            }
            if (!con.isPresent()) {
                Socket socket = dataService.getSocket(wonMessage.getRecipientAtomURI(), socketURI);
                // ok, we really do not know about the connection. create it.
                URI connectionUri = wonNodeInformationService.generateConnectionURI(wonMessage.getRecipientNodeURI());
                con = Optional.of(dataService.createConnection(connectionUri, wonMessage.getRecipientAtomURI(),
                                wonMessage.getSenderAtomURI(), wonMessage.getSenderURI(), socket.getSocketURI(),
                                socket.getTypeURI(), targetSocketURI.get(), ConnectionState.REQUEST_RECEIVED,
                                ConnectionEventType.PARTNER_OPEN));
            }
        } else {
            // the opener knew about the connection. just load it.
            con = connectionRepository.findOneByConnectionURIForUpdate(connectionURIFromWonMessage.get());
        }
        // now perform checks
        if (!con.isPresent())
            throw new IllegalStateException("connection must not be null");
        if (con.get().getTargetAtomURI() == null)
            throw new IllegalStateException("remote atom uri must not be null");
        if (!con.get().getTargetAtomURI().equals(wonMessage.getSenderAtomURI()))
            throw new IllegalStateException(
                            "the remote atom uri of the connection must be equal to the sender atom uri of the message");
        if (wonMessage.getSenderURI() == null)
            throw new IllegalStateException("the sender uri must not be null");
        // it is possible that we didn't store the reference to the remote conneciton
        // yet. Now we can do it.
        if (con.get().getTargetConnectionURI() == null) {
            // Set it from the message (it's the sender of the message)
            con.get().setTargetConnectionURI(wonMessage.getSenderURI());
        }
        if (!con.get().getTargetConnectionURI().equals(wonMessage.getSenderURI()))
            throw new IllegalStateException("the sender uri of the message must be equal to the remote connection uri");
        failForIncompatibleSockets(con.get().getSocketURI(), con.get().getTargetSocketURI());
        ConnectionState state = con.get().getState();
        if (state != ConnectionState.CONNECTED) {
            state = state.transit(ConnectionEventType.PARTNER_OPEN);
            if (state == ConnectionState.CONNECTED) {
                // previously unconnected connection would be established. Check capacity:
                failForExceededCapacity(con.get().getSocketURI());
            }
        }
        con.get().setState(state);
        connectionRepository.save(con.get());
        // set the receiver to the local connection uri
        wonMessage.addMessageProperty(WONMSG.recipient, con.get().getConnectionURI());
    }
}

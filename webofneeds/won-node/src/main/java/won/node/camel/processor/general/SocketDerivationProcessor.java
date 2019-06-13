package won.node.camel.processor.general;

import java.net.URI;
import java.util.Optional;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.beans.factory.annotation.Autowired;

import won.node.protocol.MatcherProtocolMatcherServiceClientSide;
import won.node.protocol.impl.MatcherProtocolMatcherClientImpl;
import won.node.socket.ConnectionStateChange;
import won.node.socket.SocketService;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageDirection;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.model.Atom;
import won.protocol.model.Connection;
import won.protocol.repository.AtomRepository;
import won.protocol.repository.ConnectionRepository;

/**
 * Compares the connection state found in the header of the 'in' message with
 * the state the connection is in now and triggers the data derivation.
 */
public class SocketDerivationProcessor implements Processor {
    @Autowired
    ConnectionRepository connectionRepository;
    @Autowired
    AtomRepository atomRepository;
    @Autowired
    SocketService derivationService;

    public SocketDerivationProcessor() {
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        Optional<Connection> con = Optional.empty();
        ConnectionStateChangeBuilder stateChangeBuilder = (ConnectionStateChangeBuilder) exchange.getIn()
                        .getHeader(WonCamelConstants.CONNECTION_STATE_CHANGE_BUILDER_HEADER);
        if (stateChangeBuilder == null) {
            throw new IllegalStateException("expecting to find a ConnectionStateBuilder in 'in' header '"
                            + WonCamelConstants.CONNECTION_STATE_CHANGE_BUILDER_HEADER + "'");
        }
        // first, try to find the connection uri in the header:
        URI conUri = (URI) exchange.getIn().getHeader(WonCamelConstants.CONNECTION_URI_HEADER);
        if (conUri == null) {
            // not found. get it from the message and put it in the header
            WonMessage wonMessage = (WonMessage) exchange.getIn().getHeader(WonCamelConstants.MESSAGE_HEADER);
            conUri = wonMessage.getEnvelopeType() == WonMessageDirection.FROM_EXTERNAL ? wonMessage.getRecipientURI()
                            : wonMessage.getSenderURI();
        }
        if (conUri != null) {
            // found a connection. Put its URI in the header and load it
            con = Optional.of(connectionRepository.findOneByConnectionURI(conUri));
            stateChangeBuilder.newState(con.get().getState());
        } else {
            // found no connection. don't modify the builder
        }
        // only if there is enough data to make a connectionStateChange object, make it
        // and pass it to the data
        // derivation service.
        if (stateChangeBuilder.canBuild()) {
            ConnectionStateChange connectionStateChange = stateChangeBuilder.build();
            if (!con.isPresent()) {
                con = Optional.of(connectionRepository.findOneByConnectionURI(conUri));
            }
            Atom atom = atomRepository.findOneByAtomURI(con.get().getAtomURI());
            derivationService.deriveDataForStateChange(connectionStateChange, atom, con.get());
        }
    }
}

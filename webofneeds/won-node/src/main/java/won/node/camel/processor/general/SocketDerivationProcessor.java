package won.node.camel.processor.general;

import static won.node.camel.processor.WonCamelHelper.*;

import java.net.URI;
import java.util.Optional;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.beans.factory.annotation.Autowired;

import won.node.service.nodebehaviour.ConnectionStateChange;
import won.node.service.nodebehaviour.DataDerivationService;
import won.node.service.persistence.AtomService;
import won.node.service.persistence.ConnectionService;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageDirection;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.model.Atom;
import won.protocol.model.Connection;

/**
 * Compares the connection state found in the header of the 'in' message with
 * the state the connection is in now and triggers the data derivation.
 */
public class SocketDerivationProcessor implements Processor {
    @Autowired
    DataDerivationService dataDerivationService;
    @Autowired
    ConnectionService connectionService;
    @Autowired
    AtomService atomService;

    public SocketDerivationProcessor() {
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        Optional<Connection> con = Optional.empty();
        WonMessageDirection direction = getDirectionRequired(exchange);
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
            WonMessage wonMessage = getMessageRequired(exchange);
            conUri = direction.isFromExternal() ? wonMessage.getRecipientURI()
                            : wonMessage.getSenderURI();
        }
        if (conUri != null) {
            // found a connection. Put its URI in the header and load it
            con = connectionService.getConnection(conUri);
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
                con = connectionService.getConnection(conUri);
            }
            Atom atom = atomService.getAtomRequired(con.get().getAtomURI());
            dataDerivationService.deriveDataForStateChange(connectionStateChange, atom, con.get());
        }
    }
}

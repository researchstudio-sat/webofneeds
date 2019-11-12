package won.node.camel.processor.general;

import static won.node.camel.service.WonCamelHelper.*;

import java.util.Optional;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import won.node.camel.service.WonCamelHelper;
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
    private final Logger logger = LoggerFactory.getLogger(getClass());
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
        WonMessage wonMessage = getMessageRequired(exchange);
        if (!wonMessage.getMessageTypeRequired().isConnectionSpecificMessage()) {
            return;
        }
        WonMessageDirection direction = getDirectionRequired(exchange);
        ConnectionStateChangeBuilder stateChangeBuilder = (ConnectionStateChangeBuilder) exchange.getIn()
                        .getHeader(WonCamelConstants.CONNECTION_STATE_CHANGE_BUILDER_HEADER);
        if (stateChangeBuilder == null) {
            throw new IllegalStateException("expecting to find a ConnectionStateBuilder in 'in' header '"
                            + WonCamelConstants.CONNECTION_STATE_CHANGE_BUILDER_HEADER + "'");
        }
        // first, try to find the connection
        Optional<Connection> con = WonCamelHelper.getConnection(exchange, connectionService);
        // only if there is enough data to make a connectionStateChange object, make it
        // and pass it to the data
        // derivation service.
        if (stateChangeBuilder.canBuild()) {
            ConnectionStateChange connectionStateChange = stateChangeBuilder.build();
            if (!con.isPresent()) {
                logger.warn("Cannot derive data: no connection found");
            }
            Atom atom = atomService.getAtomRequired(con.get().getAtomURI());
            dataDerivationService.deriveDataForStateChange(connectionStateChange, atom, con.get());
        }
    }
}

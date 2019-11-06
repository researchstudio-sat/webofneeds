package won.node.camel.processor.fixed;

import java.net.URI;
import java.util.Optional;

import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import won.node.camel.processor.AbstractCamelProcessor;
import won.node.camel.processor.annotation.FixedMessageReactionProcessor;
import won.node.camel.processor.general.ConnectionStateChangeBuilder;
import won.node.service.nodebehaviour.ConnectionStateChange;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageDirection;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.model.Atom;
import won.protocol.model.Connection;

/**
 * Configured to react to any message, checking whether the message caused a
 * connection state change, then Compares the connection state found in the
 * header of the 'in' message with the state the connection is in now and
 * triggers the data derivation.
 */
@Component
@FixedMessageReactionProcessor()
public class ConnectionStateChangeReactionProcessor extends AbstractCamelProcessor {
    Logger logger = LoggerFactory.getLogger(getClass());

    public ConnectionStateChangeReactionProcessor() {
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        Optional<Connection> con = Optional.empty();
        WonMessage wonMessage = (WonMessage) exchange.getIn().getHeader(WonCamelConstants.MESSAGE_HEADER);
        String msgTypeDir = "[message type: "
                        + wonMessage.getMessageType()
                        + ", direction: " + wonMessage.getEnvelopeType() + "]";
        ConnectionStateChangeBuilder stateChangeBuilder = (ConnectionStateChangeBuilder) exchange.getIn()
                        .getHeader(WonCamelConstants.CONNECTION_STATE_CHANGE_BUILDER_HEADER);
        if (stateChangeBuilder == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("no stateChangeBuilder found in exchange header, cannot check for state change "
                                + msgTypeDir);
            }
            return;
        }
        // first, try to find the connection uri in the header:
        URI conUri = (URI) exchange.getIn().getHeader(WonCamelConstants.CONNECTION_URI_HEADER);
        if (conUri == null) {
            // not found. get it from the message and put it in the header
            wonMessage = (WonMessage) exchange.getIn().getHeader(WonCamelConstants.MESSAGE_HEADER);
            conUri = wonMessage.getEnvelopeType() == WonMessageDirection.FROM_EXTERNAL ? wonMessage.getRecipientURI()
                            : wonMessage.getSenderURI();
        }
        if (conUri != null) {
            // found a connection. Put its URI in the header and load it
            con = connectionService.getConnection(conUri);
            if (!stateChangeBuilder.canBuild()) {
                stateChangeBuilder.newState(con.get().getState());
            }
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
            if (connectionStateChange.isConnect() || connectionStateChange.isDisconnect()) {
                // trigger rematch
                matcherProtocolMatcherClient.atomModified(atom.getAtomURI(), null);
                logger.debug("matchers notified of connection state change {}", msgTypeDir);
            } else {
                logger.debug("no relevant connection state change, not notifying matchers {}", msgTypeDir);
            }
        } else {
            logger.debug("Could not collect ConnectionStateChange information, not checking for state change {}",
                            msgTypeDir);
        }
    }
}

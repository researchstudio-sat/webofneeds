package won.node.camel.processor.general;

import java.net.URI;
import java.util.Collection;

import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import won.node.camel.processor.AbstractCamelProcessor;
import won.protocol.exception.WonMessageProcessingException;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageType;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.model.Atom;
import won.protocol.model.AtomState;
import won.protocol.model.Connection;
import won.protocol.model.ConnectionState;
import won.protocol.util.DataAccessUtils;

/**
 * User: MS 01.12.2018
 */
public class DeleteAtomMessageProcessor extends AbstractCamelProcessor {
    Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public void process(final Exchange exchange) throws Exception {
        WonMessage wonMessage = (WonMessage) exchange.getIn().getHeader(WonCamelConstants.RESPONSE_HEADER);
        WonMessage focalMessage = wonMessage.getFocalMessage();
        if (focalMessage.getMessageType() == WonMessageType.SUCCESS_RESPONSE
                        && focalMessage.getRespondingToMessageType() == WonMessageType.DELETE) {
            URI recipientAtomURI = focalMessage.getRecipientAtomURI();
            if (recipientAtomURI == null) {
                throw new WonMessageProcessingException("recipientAtomURI is not set");
            }
            Atom atom = DataAccessUtils.loadAtom(atomRepository, recipientAtomURI);
            if (atom.getState() == AtomState.DELETED) {
                // Delete Atom
                logger.debug("Set atom to state DELETED. atomURI:{}", recipientAtomURI);
                Collection<Connection> conns = connectionRepository.findByAtomURIAndNotState(atom.getAtomURI(),
                                ConnectionState.CLOSED);
                if (conns.size() > 0) {
                    // Still not closed connections
                    logger.debug("Still open connections for atom. atomURI{}", recipientAtomURI);
                    // TODO: Handle!
                }
                messageEventRepository.deleteByParentURI(atom.getAtomURI());
                atom.resetAllAtomData();
            } else {
                // First Step: Delete message to set atom in DELETED state and start delete
                // process
                logger.debug("DELETING atom. atomURI:{}", recipientAtomURI);
                atom.setState(AtomState.DELETED);
            }
            atomRepository.save(atom);
        }
    }
}

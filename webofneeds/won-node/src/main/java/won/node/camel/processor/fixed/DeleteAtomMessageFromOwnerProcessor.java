package won.node.camel.processor.fixed;

import java.util.Optional;

import javax.persistence.EntityManager;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import won.node.camel.processor.AbstractCamelProcessor;
import won.node.camel.processor.annotation.FixedMessageProcessor;
import won.protocol.exception.IllegalMessageForAtomStateException;
import won.protocol.exception.NoSuchAtomException;
import won.protocol.message.WonMessage;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.model.Atom;
import won.protocol.model.AtomState;
import won.protocol.vocabulary.WONMSG;

/**
 * User: syim Date: 02.03.2015
 */
@Component
@FixedMessageProcessor(direction = WONMSG.FromOwnerString, messageType = WONMSG.DeleteMessageString)
public class DeleteAtomMessageFromOwnerProcessor extends AbstractCamelProcessor {
    @Autowired
    EntityManager entityManager;

    public void process(final Exchange exchange) throws Exception {
        Message message = exchange.getIn();
        WonMessage wonMessage = (WonMessage) message.getHeader(WonCamelConstants.MESSAGE_HEADER);
        Optional<Atom> atom = atomService.getAtom(wonMessage.getAtomURI());
        if (!atom.isPresent()) {
            throw new NoSuchAtomException(wonMessage.getAtomURI());
        }
        if (atom.get().getState() == AtomState.DELETED) {
            throw new IllegalMessageForAtomStateException(atom.get().getAtomURI(), "DELETE", atom.get().getState());
        }
        // the rest of the delete tasks are done in the reaction processor
    }
}

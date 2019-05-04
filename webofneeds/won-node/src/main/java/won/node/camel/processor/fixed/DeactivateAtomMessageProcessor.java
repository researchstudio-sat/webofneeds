package won.node.camel.processor.fixed;

import java.net.URI;

import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import won.node.camel.processor.AbstractCamelProcessor;
import won.node.camel.processor.annotation.FixedMessageProcessor;
import won.protocol.message.WonMessage;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.message.processor.exception.WonMessageProcessingException;
import won.protocol.model.Atom;
import won.protocol.model.AtomState;
import won.protocol.util.DataAccessUtils;
import won.protocol.vocabulary.WONMSG;

/**
 * User: syim Date: 02.03.2015
 */
@Component
@FixedMessageProcessor(direction = WONMSG.FromOwnerString, messageType = WONMSG.DeactivateMessageString)
public class DeactivateAtomMessageProcessor extends AbstractCamelProcessor {
    Logger logger = LoggerFactory.getLogger(this.getClass());

    public void process(final Exchange exchange) throws Exception {
        WonMessage wonMessage = (WonMessage) exchange.getIn().getHeader(WonCamelConstants.MESSAGE_HEADER);
        URI recipientAtomURI = wonMessage.getRecipientAtomURI();
        logger.debug("DEACTIVATING atom. atomURI:{}", recipientAtomURI);
        if (recipientAtomURI == null)
            throw new WonMessageProcessingException("recipientAtomURI is not set");
        Atom atom = DataAccessUtils.loadAtom(atomRepository, recipientAtomURI);
        atom.getMessageContainer().getEvents()
                        .add(messageEventRepository.findOneByMessageURIforUpdate(wonMessage.getMessageURI()));
        atom.setState(AtomState.INACTIVE);
        atom = atomRepository.save(atom);
    }
}

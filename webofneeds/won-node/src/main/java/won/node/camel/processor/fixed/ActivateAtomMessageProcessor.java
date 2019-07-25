package won.node.camel.processor.fixed;

import java.net.URI;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import won.node.camel.processor.AbstractCamelProcessor;
import won.node.camel.processor.annotation.FixedMessageProcessor;
import won.protocol.message.WonMessage;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.model.Atom;
import won.protocol.model.AtomState;
import won.protocol.util.DataAccessUtils;
import won.protocol.vocabulary.WONMSG;

/**
 * User: syim Date: 02.03.2015
 */
@Component
@FixedMessageProcessor(direction = WONMSG.FromOwnerString, messageType = WONMSG.ActivateMessageString)
public class ActivateAtomMessageProcessor extends AbstractCamelProcessor {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public void process(Exchange exchange) throws Exception {
        Message message = exchange.getIn();
        WonMessage wonMessage = (WonMessage) message.getHeader(WonCamelConstants.MESSAGE_HEADER);
        URI recipientAtomURI = wonMessage.getRecipientAtomURI();
        logger.debug("ACTIVATING atom. atomURI:{}", recipientAtomURI);
        if (recipientAtomURI == null)
            throw new IllegalArgumentException("recipientAtomURI is not set");
        Atom atom = DataAccessUtils.loadAtom(atomRepository, recipientAtomURI);
        atom.getMessageContainer().getEvents()
                        .add(messageEventRepository.findOneByMessageURIforUpdate(wonMessage.getMessageURI()));
        atom.setState(AtomState.ACTIVE);
        logger.debug("Setting Atom State: " + atom.getState());
        atomRepository.save(atom);
    }
}

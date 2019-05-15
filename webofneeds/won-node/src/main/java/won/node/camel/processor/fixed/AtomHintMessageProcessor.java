package won.node.camel.processor.fixed;

import java.net.URI;
import java.util.Optional;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import won.node.camel.processor.AbstractCamelProcessor;
import won.node.camel.processor.annotation.FixedMessageProcessor;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageType;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.message.processor.exception.MissingMessagePropertyException;
import won.protocol.model.Atom;
import won.protocol.model.Connection;
import won.protocol.model.ConnectionEventType;
import won.protocol.model.ConnectionState;
import won.protocol.model.Socket;
import won.protocol.repository.ConnectionRepository;
import won.protocol.util.RdfUtils;
import won.protocol.util.WonRdfUtils;
import won.protocol.vocabulary.WON;
import won.protocol.vocabulary.WONMSG;

/**
 * User: syim Date: 02.03.2015
 */
@Component
@FixedMessageProcessor(direction = WONMSG.FromExternalString, messageType = WONMSG.AtomHintMessageString)
public class AtomHintMessageProcessor extends AbstractCamelProcessor {
    @Value("${ignore.hints.suggested.connection.count.max}")
    private Long maxAtomHintsCount = 100L;

    public void process(Exchange exchange) throws Exception {
        Message message = exchange.getIn();
        WonMessage wonMessage = (WonMessage) message.getHeader(WonCamelConstants.MESSAGE_HEADER);
        logger.debug("STORING message with id {}", wonMessage.getMessageURI());
        URI atomURIFromWonMessage = wonMessage.getRecipientAtomURI();
        if (isTooManyHints(atomURIFromWonMessage)) {
            exchange.getIn().setHeader(WonCamelConstants.IGNORE_HINT, Boolean.TRUE);
            return;
        }
        URI otherAtomURIFromWonMessage = wonMessage.getHintTargetAtomURI();
        if (otherAtomURIFromWonMessage == null) {
            throw new MissingMessagePropertyException(URI.create(WONMSG.hintTargetAtom.toString()));
        }
        URI recipientAtomURI = wonMessage.getRecipientAtomURI();
        if (recipientAtomURI == null) {
            throw new MissingMessagePropertyException(URI.create(WONMSG.recipientAtom.toString()));
        }
        if (wonMessage.getHintTargetSocketURI() != null) {
            throw new IllegalArgumentException("An AtomHintMessage must not have a msg:hintTargetSocket property");
        }
        Double wmScore = wonMessage.getHintScore();
        URI wmOriginator = wonMessage.getSenderNodeURI();
        if (wmScore == null) {
            throw new MissingMessagePropertyException(URI.create(WONMSG.hintScore.toString()));
        }
        if (wmScore < 0 || wmScore > 1) {
            throw new IllegalArgumentException("score is not in [0,1]");
        }
        if (wmOriginator == null) {
            throw new IllegalArgumentException("originator is not set");
        }
        // add to atom's messages
        Atom atom = atomRepository.findOneByAtomURI(recipientAtomURI);
        if (atom == null) {
            throw new IllegalArgumentException("atom not found - cannot send atom message to: " + recipientAtomURI);
        }
        atom.getMessageContainer().getEvents()
                        .add(messageEventRepository.findOneByMessageURIforUpdate(wonMessage.getMessageURI()));
        atom = atomRepository.save(atom);
    }

    private boolean isTooManyHints(URI atomURIFromWonMessage) {
        long hintCount = messageEventRepository.countByParentURIAndMessageType(atomURIFromWonMessage,
                        WonMessageType.ATOM_HINT_MESSAGE);
        return (hintCount > maxAtomHintsCount);
    }
}

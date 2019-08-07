package won.node.camel.processor.fixed;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import won.node.camel.processor.AbstractCamelProcessor;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageType;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.message.processor.exception.MissingMessagePropertyException;
import won.protocol.vocabulary.WONMSG;

import java.lang.invoke.MethodHandles;
import java.net.URI;

/**
 * User: syim Date: 02.03.2015
 */
@Component
public class AtomHintMessageProcessor extends AbstractCamelProcessor {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
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
    }

    private boolean isTooManyHints(URI atomURIFromWonMessage) {
        long hintCount = messageEventRepository.countByParentURIAndMessageType(atomURIFromWonMessage,
                        WonMessageType.ATOM_HINT_MESSAGE);
        return (hintCount > maxAtomHintsCount);
    }
}

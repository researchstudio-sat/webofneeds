package won.node.camel.processor.fixed;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.springframework.stereotype.Service;

import won.node.camel.processor.AbstractCamelProcessor;
import won.node.camel.processor.annotation.FixedMessageProcessor;
import won.protocol.message.WonMessage;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.model.Atom;
import won.protocol.vocabulary.WONMSG;

/**
 * Processor for a REPLACE message. Effects:
 * <ul>
 * <li>Changes the atom content</li>
 * <li>Replaces attachments</li>
 * <li>Replaces sockets. All connections of deleted or modified sockets are
 * closed, unless they already are closed</li>
 * <li>Does not change the atom state (ACTIVE/INACTIVE)</li>
 * <li>Triggers a FROM_SYSTEM message in each established connection (via the
 * respective Reaction processor)</li>
 * </ul>
 */
@Service
@FixedMessageProcessor(direction = WONMSG.FromOwnerString, messageType = WONMSG.ReplaceMessageString)
public class ReplaceAtomMessageProcessor extends AbstractCamelProcessor {
    @Override
    public void process(final Exchange exchange) throws Exception {
        Message message = exchange.getIn();
        WonMessage wonMessage = (WonMessage) message.getHeader(WonCamelConstants.MESSAGE_HEADER);
        Atom atom = atomService.replaceAtom(wonMessage);
    }
}

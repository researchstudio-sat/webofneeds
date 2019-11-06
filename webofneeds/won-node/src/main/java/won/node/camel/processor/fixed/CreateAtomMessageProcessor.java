package won.node.camel.processor.fixed;

import static won.node.camel.processor.WonCamelHelper.*;

import java.lang.invoke.MethodHandles;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import won.node.camel.processor.AbstractCamelProcessor;
import won.node.camel.processor.annotation.FixedMessageProcessor;
import won.protocol.message.WonMessage;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.model.Atom;
import won.protocol.vocabulary.WONMSG;

/**
 * User: syim Date: 02.03.2015
 */
@Service
@FixedMessageProcessor(direction = WONMSG.FromOwnerString, messageType = WONMSG.CreateMessageString)
public class CreateAtomMessageProcessor extends AbstractCamelProcessor {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Override
    public void process(final Exchange exchange) throws Exception {
        Message message = exchange.getIn();
        WonMessage wonMessage = (WonMessage) message.getHeader(WonCamelConstants.MESSAGE_HEADER);
        Atom atom = atomService.createAtom(wonMessage);
        putParentURI(exchange, atom.getAtomURI());
        String ownerApplicationID = message.getHeader(WonCamelConstants.OWNER_APPLICATION_ID_HEADER).toString();
        atomService.authorizeOwnerApplicationForAtom(ownerApplicationID, atom);
    }
}

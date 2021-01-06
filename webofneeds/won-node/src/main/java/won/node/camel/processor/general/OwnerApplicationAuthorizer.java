package won.node.camel.processor.general;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.Optional;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.node.camel.processor.AbstractCamelProcessor;
import won.node.camel.service.WonCamelHelper;
import won.protocol.message.WonMessage;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.model.Atom;

/**
 * Adds the ownerapplication that was used to send this message to the sender
 * atom's list of authorized ownerapplications.
 */
public class OwnerApplicationAuthorizer extends AbstractCamelProcessor {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Override
    public void process(Exchange exchange) throws Exception {
        Message message = exchange.getIn();
        WonMessage wonMessage = (WonMessage) message.getHeader(WonCamelConstants.MESSAGE_HEADER);
        if (wonMessage.getEnvelopeTypeRequired().isFromOwner()
                        && !wonMessage.getMessageType().isCreateAtom()) {
            Optional<String> ownerAppIdOpt = WonCamelHelper.getOwnerApplicationId(exchange);
            URI atomUri = wonMessage.getSenderAtomURI();
            if (atomUri != null && ownerAppIdOpt.isPresent()) {
                Optional<Atom> atomOpt = atomService.lockAtom(atomUri);
                if (atomOpt.isPresent()) {
                    atomService.authorizeOwnerApplicationForAtom(ownerAppIdOpt.get(), atomOpt.get());
                }
            }
        }
    }
}

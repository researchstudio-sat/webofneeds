package won.node.camel.predicate;

import org.apache.camel.Exchange;
import org.apache.camel.Predicate;

import won.protocol.message.processor.camel.WonCamelConstants;

public class ShouldSuppressReactionPredicate implements Predicate {
    @Override
    public boolean matches(Exchange exchange) {
        Boolean suppress = (Boolean) exchange.getIn().getHeader(WonCamelConstants.SUPPRESS_MESSAGE_REACTION);
        if (suppress != null && suppress == true) {
            return true;
        }
        return false;
    }
}

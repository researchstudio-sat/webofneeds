package won.node.camel.predicate;

import org.apache.camel.Exchange;
import org.apache.camel.Predicate;

import won.protocol.message.processor.camel.WonCamelConstants;

public class IsReactionAllowedPredicate implements Predicate {
    @Override
    public boolean matches(Exchange exchange) {
        Boolean suppress = (Boolean) exchange.getIn().getHeader(WonCamelConstants.SUPPRESS_MESSAGE_REACTION_HADER);
        if (suppress != null && suppress == true) {
            return false;
        }
        return true;
    }
}

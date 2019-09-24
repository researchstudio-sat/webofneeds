package won.bot.framework.eventbot.behaviour.textmessagecommand;

import won.bot.framework.eventbot.event.BaseAtomAndConnectionSpecificEvent;
import won.protocol.model.Connection;

/**
 * User: ypanchenko Date: 26.02.2016
 */
public class UsageCommandEvent extends BaseAtomAndConnectionSpecificEvent {
    public UsageCommandEvent(final Connection con) {
        super(con);
    }
}

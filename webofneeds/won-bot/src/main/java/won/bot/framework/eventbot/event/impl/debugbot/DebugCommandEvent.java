package won.bot.framework.eventbot.event.impl.debugbot;

import won.bot.framework.eventbot.event.BaseAtomAndConnectionSpecificEvent;
import won.protocol.model.Connection;

/**
 * User: ypanchenko Date: 26.02.2016
 */
public abstract class DebugCommandEvent extends BaseAtomAndConnectionSpecificEvent {
    public DebugCommandEvent(final Connection con) {
        super(con);
    }
}

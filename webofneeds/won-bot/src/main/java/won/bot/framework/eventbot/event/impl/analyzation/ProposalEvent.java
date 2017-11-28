package won.bot.framework.eventbot.event.impl.analyzation;

import won.bot.framework.eventbot.event.BaseNeedAndConnectionSpecificEvent;
import won.protocol.model.Connection;

/**
 * Created by fsuda on 27.11.2017.
 */
public abstract class ProposalEvent extends BaseNeedAndConnectionSpecificEvent {
    public ProposalEvent(Connection con) {
        super(con);
    }
}

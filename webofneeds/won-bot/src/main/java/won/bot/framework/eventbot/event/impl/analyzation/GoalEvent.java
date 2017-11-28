package won.bot.framework.eventbot.event.impl.analyzation;

import won.bot.framework.eventbot.event.BaseNeedAndConnectionSpecificEvent;
import won.protocol.model.Connection;

/**
 * Created by fsuda on 27.11.2017.
 */
public abstract class GoalEvent extends BaseNeedAndConnectionSpecificEvent {
    public GoalEvent(Connection con) {
        super(con);
    }
}

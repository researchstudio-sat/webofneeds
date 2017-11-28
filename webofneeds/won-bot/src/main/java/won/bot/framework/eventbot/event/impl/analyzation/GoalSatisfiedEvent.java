package won.bot.framework.eventbot.event.impl.analyzation;

import won.bot.framework.eventbot.event.BaseEvent;
import won.protocol.model.Connection;

/**
 * Created by fsuda on 27.11.2017.
 */
public class GoalSatisfiedEvent extends GoalEvent{
    public GoalSatisfiedEvent(Connection con) {
        super(con);
    }
}

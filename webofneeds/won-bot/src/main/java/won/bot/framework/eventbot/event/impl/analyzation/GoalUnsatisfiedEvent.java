package won.bot.framework.eventbot.event.impl.analyzation;

import won.protocol.model.Connection;

/**
 * Created by fsuda on 27.11.2017.
 */
public class GoalUnsatisfiedEvent extends GoalEvent {
    public GoalUnsatisfiedEvent(Connection con, Object payload) {
        super(con, payload);
    }
}

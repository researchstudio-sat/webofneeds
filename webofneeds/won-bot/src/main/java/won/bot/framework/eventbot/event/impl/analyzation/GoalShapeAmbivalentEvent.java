package won.bot.framework.eventbot.event.impl.analyzation;

import won.protocol.model.Connection;

/**
 * Created by fsuda on 27.11.2017.
 */
public class GoalShapeAmbivalentEvent extends GoalUnsatisfiedEvent {
    public GoalShapeAmbivalentEvent(Connection con, Object payload) {
        super(con, payload);
    }
}

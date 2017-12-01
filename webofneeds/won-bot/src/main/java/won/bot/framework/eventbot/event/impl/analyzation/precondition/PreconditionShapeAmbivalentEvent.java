package won.bot.framework.eventbot.event.impl.analyzation.precondition;

import won.protocol.model.Connection;

/**
 * Created by fsuda on 27.11.2017.
 */
public class PreconditionShapeAmbivalentEvent extends PreconditionUnmetEvent {
    public PreconditionShapeAmbivalentEvent(Connection con, Object payload) {
        super(con, payload);
    }
}

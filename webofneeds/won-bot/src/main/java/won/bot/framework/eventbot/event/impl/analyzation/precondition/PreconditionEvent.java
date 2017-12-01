package won.bot.framework.eventbot.event.impl.analyzation.precondition;

import won.bot.framework.eventbot.event.BaseNeedAndConnectionSpecificEvent;
import won.protocol.model.Connection;

/**
 * Created by fsuda on 27.11.2017.
 */
public abstract class PreconditionEvent extends BaseNeedAndConnectionSpecificEvent {
    private final Object payload;

    public PreconditionEvent(Connection con, Object payload) {
        super(con);
        this.payload = payload;
    }

    public Object getPayload() {
        return payload;
    }
}

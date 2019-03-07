package won.bot.framework.eventbot.event.impl.analyzation.precondition;

import won.bot.framework.eventbot.event.BaseNeedAndConnectionSpecificEvent;
import won.protocol.model.Connection;
import won.utils.goals.GoalInstantiationResult;

/**
 * Created by fsuda on 27.11.2017.
 */
public abstract class PreconditionEvent extends BaseNeedAndConnectionSpecificEvent {
    private final GoalInstantiationResult payload;
    private final String preconditionUri;

    public PreconditionEvent(Connection con, String preconditionUri, GoalInstantiationResult payload) {
        super(con);
        this.preconditionUri = preconditionUri;
        this.payload = payload;
    }

    public GoalInstantiationResult getPayload() {
        return payload;
    }

    public String getPreconditionUri() {
        return preconditionUri;
    }
}

package won.bot.framework.eventbot.event.impl.analyzation.precondition;

import won.protocol.model.Connection;
import won.utils.goals.GoalInstantiationResult;

import java.net.URI;

/**
 * Created by fsuda on 27.11.2017.
 */
public class PreconditionUnmetEvent extends PreconditionEvent {
    public PreconditionUnmetEvent(Connection con, String preconditionUri, GoalInstantiationResult payload) {
        super(con, preconditionUri, payload);
    }
}

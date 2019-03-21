package won.bot.framework.eventbot.event.impl.analyzation.precondition;

import won.protocol.model.Connection;
import won.utils.goals.GoalInstantiationResult;

/**
 * Created by fsuda on 27.11.2017.
 */
public class PreconditionMetEvent extends PreconditionEvent {
    public PreconditionMetEvent(Connection con, String preconditionUri, GoalInstantiationResult payload) {
        super(con, preconditionUri, payload);
    }
}

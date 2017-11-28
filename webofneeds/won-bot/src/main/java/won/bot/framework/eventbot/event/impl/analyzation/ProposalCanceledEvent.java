package won.bot.framework.eventbot.event.impl.analyzation;

import won.protocol.model.Connection;

/**
 * Created by fsuda on 27.11.2017.
 */
public class ProposalCanceledEvent extends ProposalEvent {
    public ProposalCanceledEvent(Connection con) {
        super(con);
    }
}

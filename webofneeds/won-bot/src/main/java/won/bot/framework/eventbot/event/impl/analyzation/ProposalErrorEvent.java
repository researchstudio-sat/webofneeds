package won.bot.framework.eventbot.event.impl.analyzation;

import won.protocol.model.Connection;

/**
 * Created by fsuda on 27.11.2017.
 */
public class ProposalErrorEvent extends ProposalEvent{
    public ProposalErrorEvent(Connection con) {
        super(con);
    }
}

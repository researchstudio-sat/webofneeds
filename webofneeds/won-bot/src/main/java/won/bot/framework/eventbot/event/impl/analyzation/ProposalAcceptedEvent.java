package won.bot.framework.eventbot.event.impl.analyzation;

import won.protocol.model.Connection;

/**
 * Created by fsuda on 27.11.2017.
 */
public class ProposalAcceptedEvent extends ProposalEvent{
    public ProposalAcceptedEvent(Connection con) {
        super(con);
    }
}

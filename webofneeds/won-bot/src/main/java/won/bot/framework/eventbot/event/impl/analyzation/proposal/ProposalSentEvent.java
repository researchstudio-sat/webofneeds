package won.bot.framework.eventbot.event.impl.analyzation.proposal;

import won.bot.framework.eventbot.event.impl.wonmessage.WonMessageSentOnConnectionEvent;
import won.protocol.model.Connection;

/**
 * Created by fsuda on 08.03.2018.
 */
public class ProposalSentEvent extends ProposalEvent {
    public ProposalSentEvent(Connection con, WonMessageSentOnConnectionEvent proposalEvent) {
        super(con, proposalEvent);
    }
}

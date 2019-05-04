package won.bot.framework.eventbot.event.impl.analyzation.proposal;

import java.net.URI;
import java.util.List;

import won.bot.framework.eventbot.event.BaseAtomAndConnectionSpecificEvent;
import won.bot.framework.eventbot.event.MessageEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.WonMessageReceivedOnConnectionEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.WonMessageSentOnConnectionEvent;
import won.protocol.model.Connection;
import won.protocol.util.WonRdfUtils;

/**
 * Created by fsuda on 08.03.2018.
 */
public abstract class ProposalEvent extends BaseAtomAndConnectionSpecificEvent {
    private MessageEvent proposalEvent;
    private URI proposalUri;

    public ProposalEvent(Connection con, MessageEvent proposalEvent) {
        super(con);
        this.proposalEvent = proposalEvent;
        if (proposalEvent instanceof WonMessageReceivedOnConnectionEvent) {
            this.proposalUri = proposalEvent.getWonMessage().getCorrespondingRemoteMessageURI();
        } else if (proposalEvent instanceof WonMessageSentOnConnectionEvent) {
            this.proposalUri = proposalEvent.getWonMessage().getMessageURI();
        } else {
            throw new IllegalArgumentException(
                            "MessageEvent can only be of the instance WonMessageReceivedOnConnectionEvent or WonMessageSentOnConnectionEvent");
        }
    }

    /**
     * @return The Original URI Of the Proposal
     */
    public URI getProposalUri() {
        return proposalUri;
    }

    public MessageEvent getProposalEvent() {
        return proposalEvent;
    }

    public List<URI> getProposesToCancelEvents() {
        return WonRdfUtils.MessageUtils.getProposesToCancelEvents(proposalEvent.getWonMessage());
    }

    public List<URI> getProposesEvents() {
        return WonRdfUtils.MessageUtils.getProposesEvents(proposalEvent.getWonMessage());
    }

    public boolean hasProposesEvents() {
        return !getProposesEvents().isEmpty();
    }

    public boolean hasProposesToCancelEvents() {
        return !getProposesToCancelEvents().isEmpty();
    }

    /**
     * @return true if there is a mixture between proposeToCancel and proposes
     * triples in the MessageEvent
     */
    public boolean isMixed() {
        return hasProposesToCancelEvents() && hasProposesEvents();
    }
}

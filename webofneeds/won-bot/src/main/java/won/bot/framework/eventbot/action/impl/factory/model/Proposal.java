package won.bot.framework.eventbot.action.impl.factory.model;

import won.bot.framework.eventbot.action.impl.mail.model.UriType;

import java.io.Serializable;
import java.net.URI;

/**
 * Created by fsuda on 13.03.2018.
 */
public class Proposal implements Serializable {
    private URI uri;
    private ProposalState state;

    public Proposal(URI uri, ProposalState state) {
        this.uri = uri;
        this.state = state;
    }

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public ProposalState getState() {
        return state;
    }

    public void setState(ProposalState state) {
        this.state = state;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Proposal proposal = (Proposal) o;

        if (!uri.equals(proposal.uri)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return uri.hashCode();
    }
}

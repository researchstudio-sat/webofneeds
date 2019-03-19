package won.bot.framework.eventbot.action.impl.factory.model;

import java.io.Serializable;
import java.net.URI;

public class Proposal implements Serializable {
    private URI uri;
    private ProposalState state;

    public Proposal(String uri, ProposalState state) {
        this(URI.create(uri), state);
    }

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

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        Proposal proposal = (Proposal) o;

        return uri.equals(proposal.uri);
    }

    @Override
    public int hashCode() {
        return uri.hashCode();
    }

    @Override
    public String toString() {
        return "Proposal{" + "uri=" + uri + ", state=" + state + '}';
    }
}

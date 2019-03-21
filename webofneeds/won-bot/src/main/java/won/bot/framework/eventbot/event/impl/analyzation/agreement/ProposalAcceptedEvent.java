package won.bot.framework.eventbot.event.impl.analyzation.agreement;

import java.net.URI;

import org.apache.jena.rdf.model.Model;

import won.protocol.model.Connection;

/**
 * Created by fsuda on 27.11.2017.
 */
public class ProposalAcceptedEvent extends AgreementEvent {
    private final Model payload;

    public ProposalAcceptedEvent(Connection con, URI proposalUri, Model payload) {
        super(con, proposalUri);
        this.payload = payload;
    }

    public Model getPayload() {
        return payload;
    }
}

package won.bot.framework.eventbot.event.impl.analyzation.agreement;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import won.protocol.model.Connection;

import java.net.URI;

/**
 * Created by fsuda on 27.11.2017.
 */
public class AgreementAcceptedEvent extends AgreementEvent {
    private final Model payload;

    public AgreementAcceptedEvent(Connection con, URI agreementUri, Model payload) {
        super(con, agreementUri);
        this.payload = payload;
    }

    public Model getPayload() {
        return payload;
    }
}

package won.bot.framework.eventbot.event.impl.analyzation.agreement;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import won.bot.framework.eventbot.event.BaseNeedAndConnectionSpecificEvent;
import won.protocol.model.Connection;

/**
 * Created by fsuda on 27.11.2017.
 */
public abstract class AgreementEvent extends BaseNeedAndConnectionSpecificEvent {
    private final Model payload;

    public AgreementEvent(Connection con, Model payload) {
        super(con);
        this.payload = payload;
    }

    public Model getPayload() {
        return payload;
    }
}

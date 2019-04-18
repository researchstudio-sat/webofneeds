package won.bot.framework.eventbot.event.impl.analyzation.agreement;

import java.net.URI;

import won.bot.framework.eventbot.event.BaseAtomAndConnectionSpecificEvent;
import won.protocol.model.Connection;

/**
 * Created by fsuda on 27.11.2017.
 */
public abstract class AgreementEvent extends BaseAtomAndConnectionSpecificEvent {
    private final URI agreementUri;

    public AgreementEvent(Connection con, URI agreementUri) {
        super(con);
        this.agreementUri = agreementUri;
    }

    public URI getAgreementUri() {
        return agreementUri;
    }
}

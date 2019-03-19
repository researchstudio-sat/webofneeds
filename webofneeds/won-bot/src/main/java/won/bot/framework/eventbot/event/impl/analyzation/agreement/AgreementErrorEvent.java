package won.bot.framework.eventbot.event.impl.analyzation.agreement;

import java.net.URI;

import won.protocol.model.Connection;

/**
 * Created by fsuda on 27.11.2017.
 */
public class AgreementErrorEvent extends AgreementEvent {
    public AgreementErrorEvent(Connection con, URI agreementUri) {
        super(con, agreementUri);
    }
}

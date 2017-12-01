package won.bot.framework.eventbot.event.impl.analyzation.agreement;

import won.protocol.model.Connection;

/**
 * Created by fsuda on 27.11.2017.
 */
public class AgreementAcceptedEvent extends AgreementEvent {
    public AgreementAcceptedEvent(Connection con, Object payload) {
        super(con, payload);
    }
}

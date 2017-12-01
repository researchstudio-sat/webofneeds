package won.bot.framework.eventbot.event.impl.analyzation.agreement;

import won.bot.framework.eventbot.event.BaseNeedAndConnectionSpecificEvent;
import won.protocol.model.Connection;

/**
 * Created by fsuda on 27.11.2017.
 */
public abstract class AgreementEvent extends BaseNeedAndConnectionSpecificEvent {
    private final Object payload;

    public AgreementEvent(Connection con, Object payload) {
        super(con);
        this.payload = payload;
    }

    public Object getPayload() {
        return payload;
    }
}

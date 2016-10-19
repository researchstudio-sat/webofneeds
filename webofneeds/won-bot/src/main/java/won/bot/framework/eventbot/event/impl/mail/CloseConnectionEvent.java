package won.bot.framework.eventbot.event.impl.mail;

import won.bot.framework.eventbot.event.BaseEvent;
import won.bot.framework.eventbot.event.BaseNeedAndConnectionSpecificEvent;
import won.bot.framework.eventbot.event.ConnectionSpecificEvent;
import won.protocol.model.Connection;

import java.net.URI;

/**
 * Created by fsuda on 18.10.2016.
 */
public class CloseConnectionEvent extends BaseEvent implements ConnectionSpecificEvent {
    private URI connectionURI;

    @Override
    public URI getConnectionURI() {
        return this.connectionURI;
    }

    public CloseConnectionEvent(URI connectionURI) {
        this.connectionURI = connectionURI;
    }
}

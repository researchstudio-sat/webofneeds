package won.bot.framework.eventbot.event.impl.mail;

import won.bot.framework.eventbot.event.BaseEvent;
import won.bot.framework.eventbot.event.ConnectionSpecificEvent;

import java.net.URI;

/**
 * Created by fsuda on 18.10.2016.
 */
public class OpenConnectionEvent extends BaseEvent implements ConnectionSpecificEvent {
    private String message;
    private URI connectionURI;

    @Override
    public URI getConnectionURI() {
        return this.connectionURI;
    }

    public String getMessage() {
        return message;
    }

    public OpenConnectionEvent(URI connectionURI) {
        this.connectionURI = connectionURI;
    }

    public OpenConnectionEvent(URI connectionURI, String message) {
        this.connectionURI = connectionURI;
    }
}

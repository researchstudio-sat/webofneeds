package won.bot.framework.eventbot.event.impl.factory;

import java.net.URI;

import won.bot.framework.eventbot.event.BaseEvent;

/**
 * Event used when a hintevent is called on a factory need
 */
public class FactoryHintEvent extends BaseEvent {
    private URI requesterURI;
    private URI factoryNeedURI;

    public FactoryHintEvent(URI requesterURI, URI factoryNeedURI) {
        this.requesterURI = requesterURI;
        this.factoryNeedURI = factoryNeedURI;
    }

    public URI getRequesterURI() {
        return requesterURI;
    }

    public URI getFactoryNeedURI() {
        return factoryNeedURI;
    }
}

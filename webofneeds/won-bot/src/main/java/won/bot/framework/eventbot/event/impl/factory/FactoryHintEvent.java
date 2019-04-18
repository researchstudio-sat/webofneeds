package won.bot.framework.eventbot.event.impl.factory;

import java.net.URI;

import won.bot.framework.eventbot.event.BaseEvent;

/**
 * Event used when a hintevent is called on a factory atom
 */
public class FactoryHintEvent extends BaseEvent {
    private URI requesterURI;
    private URI factoryAtomURI;

    public FactoryHintEvent(URI requesterURI, URI factoryAtomURI) {
        this.requesterURI = requesterURI;
        this.factoryAtomURI = factoryAtomURI;
    }

    public URI getRequesterURI() {
        return requesterURI;
    }

    public URI getFactoryAtomURI() {
        return factoryAtomURI;
    }
}

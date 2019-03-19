package won.bot.framework.eventbot.listener.baStateBots.baCCMessagingBots.atomicBots;

import java.net.URI;

import won.bot.framework.eventbot.event.BaseEvent;
import won.bot.framework.eventbot.event.ConnectionSpecificEvent;
import won.bot.framework.eventbot.event.NeedSpecificEvent;
import won.bot.framework.eventbot.event.RemoteNeedSpecificEvent;

/**
 * Event for signalling that the second phase in the Business Activity with atomic outcome has started. It is
 * connection-specific to allow BATestScriptListeners to react to it.
 */
public class SecondPhaseStartedEvent extends BaseEvent
        implements ConnectionSpecificEvent, NeedSpecificEvent, RemoteNeedSpecificEvent {
    private URI needURI;
    private URI connectionURI;
    private URI remoteNeedURI;

    public SecondPhaseStartedEvent(final URI needURI, final URI connectionURI, final URI remoteNeedURI) {
        this.needURI = needURI;
        this.connectionURI = connectionURI;
        this.remoteNeedURI = remoteNeedURI;
    }

    public URI getNeedURI() {
        return needURI;
    }

    public URI getConnectionURI() {
        return connectionURI;
    }

    public URI getRemoteNeedURI() {
        return remoteNeedURI;
    }
}

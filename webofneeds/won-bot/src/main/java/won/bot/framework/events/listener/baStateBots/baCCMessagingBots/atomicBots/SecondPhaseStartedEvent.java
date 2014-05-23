package won.bot.framework.events.listener.baStateBots.baCCMessagingBots.atomicBots;

import won.bot.framework.events.event.BaseEvent;
import won.bot.framework.events.event.ConnectionSpecificEvent;
import won.bot.framework.events.event.NeedSpecificEvent;

import java.net.URI;

/**
 * Event for signalling that the second phase in the Business Activity with atomic outcome has started.
 * It is connection-specific to allow BATestScriptListeners to react to it.
 */
public class SecondPhaseStartedEvent extends BaseEvent implements ConnectionSpecificEvent, NeedSpecificEvent
{
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

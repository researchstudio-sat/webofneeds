package won.bot.framework.events.event.impl.monitor;

import won.bot.framework.events.event.BaseEvent;

import java.net.URI;

/**
 * User: ypanchenko
 * Date: 04.03.2016
 */
public abstract class MessageSpecificEvent extends BaseEvent
{

  private URI messageURI;

  public MessageSpecificEvent(final URI messageURI) {
    this.messageURI = messageURI;
  }

  public URI getMessageURI() {
    return messageURI;
  }
}

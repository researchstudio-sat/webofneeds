package won.bot.framework.events.event.impl.monitor;

import won.bot.framework.events.event.BaseEvent;
import won.protocol.message.WonMessage;

import java.net.URI;

/**
 * User: ypanchenko
 * Date: 04.03.2016
 */
public abstract class MessageSpecificEvent extends BaseEvent
{

  private WonMessage message;

  public MessageSpecificEvent(final WonMessage message) {
    this.message = message;
  }

  public URI getMessageURI() {
    return message.getMessageURI();
  }

  public URI getNeedURI() {
    return message.getSenderNeedURI();
  }
}

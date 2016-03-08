package won.bot.framework.events.event.impl.monitor;

import java.net.URI;

/**
 * User: ypanchenko
 * Date: 04.03.2016
 */
public class MessageDeliveryResponseReceivedEvent extends MessageSpecificEvent
{
  public MessageDeliveryResponseReceivedEvent(final URI messageURI) {
    super(messageURI);
  }
}

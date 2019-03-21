package won.bot.framework.eventbot.event.impl.monitor;

import won.bot.framework.eventbot.event.impl.wonmessage.MessageSpecificEvent;
import won.protocol.message.WonMessage;

/**
 * User: ypanchenko
 * Date: 04.03.2016
 */
public class MessageDispatchStartedEvent extends MessageSpecificEvent
{
  public MessageDispatchStartedEvent(final WonMessage message) {
    super(message);
  }
}

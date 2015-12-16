package won.bot.framework.events.event;

import won.protocol.message.WonMessage;

/**
 * Tagging interface that indicates that the event was created when an
 * actual protocol message was received.
 * Used for counting received messages.
 */
public interface MessageEvent
{
  public WonMessage getWonMessage();
}

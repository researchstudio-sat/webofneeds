package won.bot.framework.eventbot.event;

import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageType;

/**
 * Tagging interface that indicates that the event was created when an actual
 * protocol message was received. Used for counting received messages.
 */
public interface MessageEvent {
    WonMessage getWonMessage();

    WonMessageType getWonMessageType();
}

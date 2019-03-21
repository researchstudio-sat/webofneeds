package won.bot.framework.eventbot.event.impl.command;

import won.bot.framework.eventbot.event.impl.cmd.BaseCommandFailureEvent;
import won.bot.framework.eventbot.event.impl.cmd.CommandEvent;

/**
 * Created by fsuda on 15.02.2018.
 */
public class MessageCommandNotSentEvent<T extends CommandEvent> extends BaseCommandFailureEvent<T> {
    public MessageCommandNotSentEvent(String message, T originalCommandEvent) {
        super(message, originalCommandEvent);
    }

    public MessageCommandNotSentEvent(T originalCommandEvent) {
        super(originalCommandEvent);
    }
}

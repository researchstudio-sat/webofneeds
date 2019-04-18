package won.bot.framework.eventbot.event.impl.debugbot;

import won.protocol.model.Connection;

/**
 * Debugbot command instructing the bot to replace the atom content.
 */
public class ReplaceDebugAtomContentCommandEvent extends DebugCommandEvent {
    public ReplaceDebugAtomContentCommandEvent(final Connection con) {
        super(con);
    }
}

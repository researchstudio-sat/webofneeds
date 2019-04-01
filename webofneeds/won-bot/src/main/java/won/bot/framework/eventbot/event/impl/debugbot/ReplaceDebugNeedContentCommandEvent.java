package won.bot.framework.eventbot.event.impl.debugbot;

import won.protocol.model.Connection;

/**
 * Debugbot command instructing the bot to replace the need content.
 */
public class ReplaceDebugNeedContentCommandEvent extends DebugCommandEvent {
    public ReplaceDebugNeedContentCommandEvent(final Connection con) {
        super(con);
    }
}

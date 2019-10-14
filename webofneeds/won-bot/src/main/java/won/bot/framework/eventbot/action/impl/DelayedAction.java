package won.bot.framework.eventbot.action.impl;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.EventBotAction;

public class DelayedAction extends DelayedDelegatingAction {
    private final long delay;

    public DelayedAction(final EventListenerContext eventListenerContext, long delay, final EventBotAction delegate) {
        super(eventListenerContext, delegate);
        this.delay = delay;
        assert 0L <= this.delay : "Delay must be >= 0";
    }

    @Override
    protected long getDelay() {
        return delay;
    }
}

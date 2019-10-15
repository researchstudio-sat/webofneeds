package won.bot.framework.extensions.matcher;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.impl.LogAction;
import won.bot.framework.eventbot.action.impl.DelayedAction;
import won.bot.framework.eventbot.behaviour.BotBehaviour;
import won.bot.framework.eventbot.bus.EventBus;
import won.bot.framework.eventbot.event.impl.lifecycle.ActEvent;
import won.bot.framework.eventbot.listener.impl.ActionOnEventListener;

import java.util.Optional;

public class MatcherBehaviour extends BotBehaviour {
    private final EventBus eventBus;
    private final long retryInterval;

    public MatcherBehaviour(EventListenerContext context) {
        this(context, 30000L);
    }

    public MatcherBehaviour(EventListenerContext context, long retryInterval) {
        super(context);
        eventBus = context.getEventBus();
        this.retryInterval = retryInterval;
    }

    public MatcherBehaviour(EventListenerContext context, String name) {
        this(context, name, 30000L);
    }

    public MatcherBehaviour(EventListenerContext context, String name, long retryInterval) {
        super(context, name);
        eventBus = context.getEventBus();
        this.retryInterval = retryInterval;
    }

    protected void onActivate(Optional<Object> message) {
        // register as matcher on node
        RegisterMatcherAction registerMatcher = new RegisterMatcherAction(context);
        subscribeWithAutoCleanup(ActEvent.class, new ActionOnEventListener(context, registerMatcher, 1));
        // retry in case of failed registering
        DelayedAction delayedRegistration = new DelayedAction(context, retryInterval, registerMatcher);
        subscribeWithAutoCleanup(MatcherExtensionRegisterFailedEvent.class, delayedRegistration);
        // optional action on successful registering
        subscribeWithAutoCleanup(MatcherExtensionRegisterSucceededEvent.class,
                        new LogAction(context, "Successfully registered as Matcher"));
    }

    public EventBus getEventBus() {
        return eventBus;
    }
}

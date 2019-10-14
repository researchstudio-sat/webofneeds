package won.bot.framework.extensions.matcher;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.impl.LogAction;
import won.bot.framework.eventbot.action.impl.RandomDelayedAction;
import won.bot.framework.eventbot.action.impl.matcher.RegisterMatcherAction;
import won.bot.framework.eventbot.behaviour.BotBehaviour;
import won.bot.framework.eventbot.bus.EventBus;
import won.bot.framework.eventbot.event.impl.lifecycle.ActEvent;
import won.bot.framework.eventbot.event.impl.matcher.MatcherExtensionRegisterFailedEvent;
import won.bot.framework.eventbot.event.impl.matcher.MatcherExtensionRegisterSucceededEvent;
import won.bot.framework.eventbot.listener.impl.ActionOnEventListener;

import java.util.Optional;

public class MatcherBehaviour extends BotBehaviour {
    private final EventBus eventBus;
    private static final long retryInterval = 30000L; // TODO: let this be injected via config

    public MatcherBehaviour(EventListenerContext context) {
        super(context);
        eventBus = context.getEventBus();
    }

    public MatcherBehaviour(EventListenerContext context, String name) {
        super(context, name);
        eventBus = context.getEventBus();
    }

    protected void onActivate(Optional<Object> message) {
        // register as matcher on node
        // TODO: move RegisterMatcherAction to extension
        RegisterMatcherAction registerMatcher = new RegisterMatcherAction(context);
        subscribeWithAutoCleanup(ActEvent.class, new ActionOnEventListener(context, registerMatcher, 1));
        // retry in case of failed registering
        // TODO: move MatcherExtensionRegisterFailedEvent to extension
        // using a random delayed action because we don't have a non-random one
        RandomDelayedAction delayedRegistration = new RandomDelayedAction(context, retryInterval, retryInterval, 0L,
                        registerMatcher);
        subscribeWithAutoCleanup(MatcherExtensionRegisterFailedEvent.class,
                        new ActionOnEventListener(context, delayedRegistration));
        // optional action on successful registering
        // TODO: move MatcherExtensionRegisterSucceededEvent to extension
        LogAction afterRegistration = new LogAction(context, "Successfully registered as Matcher");
        subscribeWithAutoCleanup(MatcherExtensionRegisterSucceededEvent.class,
                        new ActionOnEventListener(context, afterRegistration));
    }

    public EventBus getEventBus() {
        return eventBus;
    }
}

package won.bot.framework.bot.extension;

import org.apache.jena.query.Dataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.impl.LogAction;
import won.bot.framework.eventbot.action.impl.RandomDelayedAction;
import won.bot.framework.eventbot.action.impl.matcher.RegisterMatcherAction;
import won.bot.framework.eventbot.behaviour.BotBehaviour;
import won.bot.framework.eventbot.bus.EventBus;
import won.bot.framework.eventbot.event.impl.lifecycle.ActEvent;
import won.bot.framework.eventbot.event.impl.matcher.*;
import won.bot.framework.eventbot.listener.impl.ActionOnEventListener;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.Optional;

public class MatcherBehaviour extends BotBehaviour {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final EventBus bus;
    private final int retryInterval = 30000;

    public MatcherBehaviour(EventListenerContext context) {
        super(context);
        bus = context.getEventBus();
    }

    public MatcherBehaviour(EventListenerContext context, String name) {
        super(context, name);
        bus = context.getEventBus();
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
    // Events to be sent out from here (use them in your bot implementation)
    // MatcherExtensionAtomCreatedEvent
    // MatcherExtensionAtomModifiedEvent
    // MatcherExtensionAtomActivatedEvent
    // MatcherExtensionAtomDeactivatedEvent
    // MatcherExtensionAtomDeletedEvent - does not yet exist

    // Methods to be called via the implementations of MatcherExtension methods
    public final void matcherExtensionRegistered(final URI wonNodeUri) {
        if (isActive()) {
            // EventBotActionUtils.rememberInNodeListIfNamePresent(getEventListenerContext(),wonNodeUri);
            bus.publish(new MatcherExtensionRegisterSucceededEvent(wonNodeUri));
        } else {
            logger.info("not publishing event for call to matcherExtensionRegistered() as the behaviour is deactivated");
        }
    }

    public final void matcherExtensionNewAtomCreatedNotification(final URI wonNodeURI, final URI atomURI,
                    final Dataset atomModel) {
        if (isActive()) {
            Dataset dataset = context.getLinkedDataSource().getDataForResource(atomURI);
            bus.publish(new MatcherExtensionAtomCreatedEvent(atomURI, dataset));
        } else {
            logger.info("not publishing event for call to matcherExtensionNewAtomCreatedNotification() as the behaviour is deactivated");
        }
    }

    public final void matcherExtensionAtomModifiedNotification(final URI wonNodeURI, final URI atomURI) {
        if (isActive()) {
            bus.publish(new MatcherExtensionAtomModifiedEvent(atomURI));
        } else {
            logger.info("not publishing event for call to matcherExtensionAtomModifiedNotification() as the behaviour is deactivated");
        }
    }

    public final void matcherExtensionAtomActivatedNotification(final URI wonNodeURI, final URI atomURI) {
        if (isActive()) {
            bus.publish(new MatcherExtensionAtomActivatedEvent(atomURI));
        } else {
            logger.info("not publishing event for call to matcherExtensionAtomActivatedNotification() as the behaviour is deactivated");
        }
    }

    public final void matcherExtensionAtomDeactivatedNotification(final URI wonNodeURI, final URI atomURI) {
        if (isActive()) {
            bus.publish(new MatcherExtensionAtomDeactivatedEvent(atomURI));
        } else {
            logger.info("not publishing event for call to matcherExtensionAtomDeactivatedNotification() as the behaviour is deactivated");
        }
    }
    // This method and event do not exist yet
    // public final void matcherExtensionAtomDeletedNotification(URI wonNodeURI, URI
    // atomURI) {
    // if (getLifecyclePhase().isActive()) {
    // bus.publish(new MatcherExtensionAtomDeletedEvent(atomURI));
    // } else {
    // logger.info("not publishing event for call to
    // matcherExtensionAtomDeletedNotification() as the behaviour is deactivated");
    // }
    // }
}

package won.bot.framework.eventbot.behaviour;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.action.EventBotAction;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.listener.EventListener;
import won.bot.framework.eventbot.listener.impl.ActionOnEventListener;

import java.util.Optional;

/**
 * Combines two behaviours. Deactivates automatically after execution.
 */
public final class CoordinationBehaviour extends BotBehaviour {
    private BotBehaviour behaviourA;
    private BotBehaviour behaviourB;
    private CoordinationType typeA;
    private CoordinationType typeB;

    public static enum CoordinationType {
        ACTIVATE, DEACTIVATE
    }

    private CoordinationBehaviour(EventListenerContext context) {
        super(context);
    }

    private CoordinationBehaviour(EventListenerContext context, String name) {
        super(context, name);
    }

    private CoordinationBehaviour(EventListenerContext context, BotBehaviour behaviourA, BotBehaviour behaviourB,
                    CoordinationType typeA, CoordinationType typeB) {
        super(context);
        this.behaviourA = behaviourA;
        this.behaviourB = behaviourB;
        this.typeA = typeA;
        this.typeB = typeB;
    }

    public static CoordinationBehaviour connectActivateActivate(EventListenerContext context, BotBehaviour behaviourA,
                    BotBehaviour behaviourB) {
        return new CoordinationBehaviour(context, behaviourA, behaviourB, CoordinationType.ACTIVATE,
                        CoordinationType.ACTIVATE);
    }

    public static CoordinationBehaviour connectActivateDeactivate(EventListenerContext context, BotBehaviour behaviourA,
                    BotBehaviour behaviourB) {
        return new CoordinationBehaviour(context, behaviourA, behaviourB, CoordinationType.ACTIVATE,
                        CoordinationType.DEACTIVATE);
    }

    public static CoordinationBehaviour connectDeactivateActivate(EventListenerContext context, BotBehaviour behaviourA,
                    BotBehaviour behaviourB) {
        return new CoordinationBehaviour(context, behaviourA, behaviourB, CoordinationType.DEACTIVATE,
                        CoordinationType.ACTIVATE);
    }

    public static CoordinationBehaviour connectDeactivateDeactivate(EventListenerContext context,
                    BotBehaviour behaviourA, BotBehaviour behaviourB) {
        return new CoordinationBehaviour(context, behaviourA, behaviourB, CoordinationType.DEACTIVATE,
                        CoordinationType.DEACTIVATE);
    }

    @Override
    protected void onActivate(Optional<Object> message) {
        EventBotAction actionToExecute = null;
        if (typeB == CoordinationType.ACTIVATE) {
            actionToExecute = new BaseEventBotAction(context) {
                @Override
                protected void doRun(Event event, EventListener executingListener) throws Exception {
                    BotBehaviourEvent botBehaviourEvent = (BotBehaviourEvent) event;
                    behaviourB.activate(botBehaviourEvent.getMessage());
                    deactivate(botBehaviourEvent.getMessage());
                }
            };
        } else {
            actionToExecute = new BaseEventBotAction(context) {
                @Override
                protected void doRun(Event event, EventListener executingListener) throws Exception {
                    BotBehaviourEvent botBehaviourEvent = (BotBehaviourEvent) event;
                    behaviourB.deactivate(botBehaviourEvent.getMessage());
                    deactivate(botBehaviourEvent.getMessage());
                }
            };
        }
        Class<? extends Event> eventClazz = null;
        if (typeA == CoordinationType.ACTIVATE) {
            eventClazz = BotBehaviourActivatedEvent.class;
        } else {
            eventClazz = BotBehaviourDeactivatedEvent.class;
        }
        subscribeWithAutoCleanup(eventClazz, new ActionOnEventListener(context,
                        event -> ((BotBehaviourEvent) event).getBehaviour() == behaviourA, actionToExecute));
    }
}

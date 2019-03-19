package won.bot.framework.eventbot.behaviour;

import java.util.Optional;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.action.impl.factory.InitFactoryAction;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.factory.InitFactoryFinishedEvent;
import won.bot.framework.eventbot.event.impl.lifecycle.InitializeEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.bot.framework.eventbot.listener.impl.ActionOnEventListener;
import won.bot.framework.eventbot.listener.impl.ActionOnFirstEventListener;

/**
 * InitFactoryBotBehaviour
 */
public class FactoryBotInitBehaviour extends BotBehaviour {
    public FactoryBotInitBehaviour(EventListenerContext context) {
        super(context);
    }

    public FactoryBotInitBehaviour(EventListenerContext context, String name) {
        super(context, name);
    }

    @Override
    protected void onActivate(Optional<Object> message) {
        subscribeWithAutoCleanup(InitializeEvent.class,
                new ActionOnEventListener(context, new InitFactoryAction(context)));

        subscribeWithAutoCleanup(InitFactoryFinishedEvent.class,
                new ActionOnFirstEventListener(context, new BaseEventBotAction(context) {
                    @Override
                    protected void doRun(Event event, EventListener executingListener) throws Exception {
                        FactoryBotInitBehaviour.this.deactivate();
                    }
                }));

    }
}

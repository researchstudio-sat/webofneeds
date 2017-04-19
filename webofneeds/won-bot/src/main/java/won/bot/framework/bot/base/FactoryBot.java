package won.bot.framework.bot.base;

import won.bot.framework.bot.context.FactoryBotContextWrapper;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.action.impl.factory.FactoryHintCheckAction;
import won.bot.framework.eventbot.action.impl.factory.InitFactoryAction;
import won.bot.framework.eventbot.bus.EventBus;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.factory.InitFactoryFinishedEvent;
import won.bot.framework.eventbot.event.impl.lifecycle.InitializeEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.HintFromMatcherEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.bot.framework.eventbot.listener.impl.ActionOnEventListener;
import won.bot.framework.eventbot.listener.impl.ActionOnFirstEventListener;

public abstract class FactoryBot extends EventBot {
    @Override
    protected final void initializeEventListeners() {
        if(!(super.getBotContextWrapper() instanceof FactoryBotContextWrapper)){
            logger.error("FactoryBot does not work without a FactoryBotContextWrapper");
            throw new IllegalStateException("FactoryBot does not work without a FactoryBotContextWrapper");
        }
        if(getNeedProducer() == null) {
            logger.error("FactoryBots do not work without a set needProducer");
            throw new IllegalStateException("FactoryBots do not work without a set needProducer");
        }

        EventListenerContext ctx = getEventListenerContext();
        EventBus bus = getEventBus();

        bus.subscribe(InitializeEvent.class, new ActionOnEventListener(
            ctx,
            "InitFactoryBot",
            new InitFactoryAction(ctx)
        ));

        bus.subscribe(HintFromMatcherEvent.class, new ActionOnEventListener(
            ctx,
            "HintReceived",
            new FactoryHintCheckAction(ctx)
        ));

        bus.subscribe(InitFactoryFinishedEvent.class, new ActionOnFirstEventListener(
            ctx,
            "InitFactoryBotComplete",
            new BaseEventBotAction(ctx) {
                @Override
                protected void doRun(Event event, EventListener executingListener) throws Exception {
                    initializeFactoryEventListeners();
                    bus.unsubscribe(executingListener);
                }
            }
        ));
    }

    /*
    * Override this method to initialize your remaining event listeners. Will be called after InitFactoryFinishedEvent
    * is called
    * the first event is published.
    */
    protected abstract void initializeFactoryEventListeners();

    public final FactoryBotContextWrapper getBotContextWrapper(){
        return (FactoryBotContextWrapper) super.getBotContextWrapper();
    }
}

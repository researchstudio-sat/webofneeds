package won.bot.framework.bot.base;

import java.util.Optional;

import won.bot.framework.bot.context.FactoryBotContextWrapper;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.behaviour.BotBehaviour;
import won.bot.framework.eventbot.behaviour.ExecuteWonMessageCommandBehaviour;
import won.bot.framework.eventbot.behaviour.FactoryBotHintBehaviour;
import won.bot.framework.eventbot.behaviour.FactoryBotInitBehaviour;

public abstract class FactoryBot extends EventBot {
    @Override
    protected final void initializeEventListeners() {
        if (!(super.getBotContextWrapper() instanceof FactoryBotContextWrapper)) {
            logger.error("FactoryBot does not work without a FactoryBotContextWrapper");
            throw new IllegalStateException("FactoryBot does not work without a FactoryBotContextWrapper");
        }
        if (getNeedProducer() == null) {
            logger.error("FactoryBots do not work without a set needProducer");
            throw new IllegalStateException("FactoryBots do not work without a set needProducer");
        }

        EventListenerContext ctx = getEventListenerContext();

        BotBehaviour factoryBotInitBehaviour = new FactoryBotInitBehaviour(ctx);

        BotBehaviour factoryBotHintBehaviour = new FactoryBotHintBehaviour(ctx);
        BotBehaviour messageCommandBehaviour = new ExecuteWonMessageCommandBehaviour(ctx);

        BotBehaviour runningBehaviour = new BotBehaviour(ctx) {
            @Override
            protected void onActivate(Optional<Object> message) {
                initializeFactoryEventListeners();
            }
        };

        factoryBotInitBehaviour.onDeactivateActivate(runningBehaviour, factoryBotHintBehaviour,
                messageCommandBehaviour);

        factoryBotInitBehaviour.activate();
    }

    /*
     * Override this method to initialize your remaining event listeners. Will be called after InitFactoryFinishedEvent
     * is called the first event is published.
     */
    protected abstract void initializeFactoryEventListeners();

    public final FactoryBotContextWrapper getBotContextWrapper() {
        return (FactoryBotContextWrapper) super.getBotContextWrapper();
    }
}

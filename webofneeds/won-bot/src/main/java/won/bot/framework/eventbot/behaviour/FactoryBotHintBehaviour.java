package won.bot.framework.eventbot.behaviour;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.impl.factory.FactoryHintCheckAction;
import won.bot.framework.eventbot.action.impl.factory.InitFactoryAction;
import won.bot.framework.eventbot.event.impl.lifecycle.InitializeEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.HintFromMatcherEvent;
import won.bot.framework.eventbot.listener.impl.ActionOnEventListener;

/**
 * InitFactoryBotBehaviour
 */
public class FactoryBotHintBehaviour extends BotBehaviour {
    public FactoryBotHintBehaviour(EventListenerContext context) {
        super(context);
    }

    public FactoryBotHintBehaviour(EventListenerContext context, String name) {
        super(context, name);
    }

    @Override
    protected void onActivate() {
        subscribeWithAutoCleanup(HintFromMatcherEvent.class, new ActionOnEventListener(
            context,
            new FactoryHintCheckAction(context)
        ));
    }
}

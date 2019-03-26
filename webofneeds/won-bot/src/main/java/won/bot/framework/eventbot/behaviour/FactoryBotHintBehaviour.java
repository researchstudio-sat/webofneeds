package won.bot.framework.eventbot.behaviour;

import java.util.Optional;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.impl.factory.FactoryHintCheckAction;
import won.bot.framework.eventbot.event.impl.wonmessage.HintFromMatcherEvent;
import won.bot.framework.eventbot.listener.impl.ActionOnEventListener;

/**
 * FactoryBotHintBehaviour
 */
public class FactoryBotHintBehaviour extends BotBehaviour {
  public FactoryBotHintBehaviour(EventListenerContext context) {
    super(context);
  }

  public FactoryBotHintBehaviour(EventListenerContext context, String name) {
    super(context, name);
  }

  @Override
  protected void onActivate(Optional<Object> message) {
    subscribeWithAutoCleanup(HintFromMatcherEvent.class,
        new ActionOnEventListener(context, new FactoryHintCheckAction(context)));
  }
}

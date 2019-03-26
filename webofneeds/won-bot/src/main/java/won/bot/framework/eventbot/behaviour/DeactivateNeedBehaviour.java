package won.bot.framework.eventbot.behaviour;

import java.util.Optional;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.impl.wonmessage.execCommand.ExecuteDeactivateNeedCommandAction;
import won.bot.framework.eventbot.event.impl.command.deactivate.DeactivateNeedCommandEvent;
import won.bot.framework.eventbot.listener.impl.ActionOnEventListener;

/**
 * Created by fsuda on 17.05.2017.
 */
public class DeactivateNeedBehaviour extends BotBehaviour {
  public DeactivateNeedBehaviour(EventListenerContext context) {
    super(context);
  }

  public DeactivateNeedBehaviour(EventListenerContext context, String name) {
    super(context, name);
  }

  @Override
  protected void onActivate(Optional<Object> message) {
    this.subscribeWithAutoCleanup(DeactivateNeedCommandEvent.class,
        new ActionOnEventListener(context, new ExecuteDeactivateNeedCommandAction(context)));
  }
}

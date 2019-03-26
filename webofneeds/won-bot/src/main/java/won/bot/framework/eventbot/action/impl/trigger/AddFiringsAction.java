package won.bot.framework.eventbot.action.impl.trigger;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.listener.EventListener;

public class AddFiringsAction extends BaseEventBotAction {
  private FireCountLimitedBotTrigger trigger;
  private int numFirings;

  public AddFiringsAction(EventListenerContext eventListenerContext, FireCountLimitedBotTrigger trigger,
      int numFirings) {
    super(eventListenerContext);
    this.trigger = trigger;
    this.numFirings = numFirings;
  }

  @Override
  protected void doRun(Event event, EventListener executingListener) throws Exception {
    this.trigger.addFirings(numFirings);
  }
}

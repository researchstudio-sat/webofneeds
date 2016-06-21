package won.bot.impl;

import won.bot.framework.bot.base.EventBot;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.impl.matcher.IndexNeedAction;
import won.bot.framework.eventbot.action.impl.matcher.RegisterMatcherAction;
import won.bot.framework.eventbot.bus.EventBus;
import won.bot.framework.eventbot.event.impl.lifecycle.ActEvent;
import won.bot.framework.eventbot.event.impl.matcher.NeedCreatedEventForMatcher;
import won.bot.framework.eventbot.listener.BaseEventListener;
import won.bot.framework.eventbot.listener.impl.ActionOnEventListener;

/**
 * User: LEIH-NB
 * Date: 29.04.14
 */
public class MatcherBot extends EventBot
{

  private BaseEventListener matcherRegistrator;
  private BaseEventListener matcherIndexer;

  @Override
  protected void initializeEventListeners()
  {
    EventListenerContext ctx = getEventListenerContext();
    EventBus bus = getEventBus();

    this.matcherRegistrator = new ActionOnEventListener(
      ctx,
      new RegisterMatcherAction(ctx),
      1
    );
    bus.subscribe(ActEvent.class,this.matcherRegistrator);

    this.matcherIndexer = new ActionOnEventListener(ctx,new IndexNeedAction(ctx));
    bus.subscribe(NeedCreatedEventForMatcher.class,matcherIndexer);
  }
}

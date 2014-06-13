package won.bot.impl;

import won.bot.framework.bot.base.EventBot;
import won.bot.framework.events.EventListenerContext;
import won.bot.framework.events.action.impl.IndexNeedAction;
import won.bot.framework.events.action.impl.RegisterMatcherAction;
import won.bot.framework.events.bus.EventBus;
import won.bot.framework.events.event.impl.ActEvent;
import won.bot.framework.events.event.impl.NeedCreatedEventForMatcher;
import won.bot.framework.events.listener.BaseEventListener;
import won.bot.framework.events.listener.impl.ActionOnEventListener;

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

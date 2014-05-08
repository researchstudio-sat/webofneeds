/*
 * Copyright 2012  Research Studios Austria Forschungsges.m.b.H.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package won.bot.impl;

import won.bot.framework.bot.base.EventBot;
import won.bot.framework.events.EventListenerContext;
import won.bot.framework.events.action.BaseEventBotAction;
import won.bot.framework.events.action.impl.*;
import won.bot.framework.events.bus.EventBus;
import won.bot.framework.events.event.Event;
import won.bot.framework.events.event.impl.*;
import won.bot.framework.events.listener.BaseEventListener;
import won.bot.framework.events.listener.impl.ActionOnEventListener;
import won.bot.framework.events.listener.impl.ActionOnceAfterNEventsListener;

/**
 *
 */
public class MatcherProtocolTestBot extends EventBot
{

  private static final int NO_OF_NEEDS = 2;


  //we use protected members so we can extend the class and
  //access the listeners for unit test assertions and stats
  //
  //we use BaseEventListener as their types so we can access the generic
  //functionality offered by that class
  protected BaseEventListener matcherRegistrator;
  protected BaseEventListener needCreator;
  protected BaseEventListener matcher;
  protected BaseEventListener allNeedsDeactivator;
  protected BaseEventListener workDoneSignaller;
  protected BaseEventListener matcherNotifier;
  private static final String NAME_NEEDS = "needs";

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

    //create needs every trigger execution until 2 needs are created
    this.needCreator = new ActionOnEventListener(
        ctx,
        new CreateNeedWithFacetsAction(ctx,NAME_NEEDS),
        NO_OF_NEEDS
    );

    bus.subscribe(MatcherRegisteredEvent.class, new ActionOnEventListener(ctx, new BaseEventBotAction(ctx)
    {
      @Override
      protected void doRun(final Event event) throws Exception {
        getEventListenerContext().getEventBus().subscribe(ActEvent.class, needCreator);
      }
    },1));

    this.matcherNotifier = new ActionOnceAfterNEventsListener(ctx,4,new LogAction(ctx,
      "Received all events for newly created needs."));
    bus.subscribe(NeedCreatedEventForMatcher.class,matcherNotifier);
    bus.subscribe(NeedCreatedEvent.class,matcherNotifier);

    this.matcher = new ActionOnceAfterNEventsListener(
            ctx,
        NO_OF_NEEDS, new MatchNeedsAction(ctx)
    );
    //count until 1 need is created, then create a comment facet
    bus.subscribe(NeedCreatedEvent.class, this.matcher);

   this.allNeedsDeactivator = new ActionOnEventListener(ctx, new DeactivateAllNeedsAction(ctx), 1);
   bus.subscribe(HintFromMatcherEvent.class, this.allNeedsDeactivator);

  this.workDoneSignaller = new ActionOnceAfterNEventsListener(
          ctx,
      4, new SignalWorkDoneAction(ctx)
  );
  bus.subscribe(NeedDeactivatedEvent.class, this.workDoneSignaller);
  bus.subscribe(NeedDeactivatedEventForMatcher.class, this.workDoneSignaller);

  }

}

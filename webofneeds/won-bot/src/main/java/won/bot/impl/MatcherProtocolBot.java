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
import won.bot.framework.events.EventBus;
import won.bot.framework.events.event.ActEvent;
import won.bot.framework.events.event.HintFromMatcherEvent;
import won.bot.framework.events.event.NeedCreatedEvent;
import won.bot.framework.events.event.NeedDeactivatedEvent;
import won.bot.framework.events.listener.*;
import won.bot.framework.events.listener.action.CreateNeedAction;
import won.bot.framework.events.listener.action.MatchNeedsAction;
import won.bot.framework.events.listener.action.SignalWorkDoneAction;

/**
 *
 */
public class MatcherProtocolBot extends EventBot
{

  private static final int NO_OF_NEEDS = 2;


  //we use protected members so we can extend the class and
  //access the listeners for unit test assertions and stats
  //
  //we use BaseEventListener as their types so we can access the generic
  //functionality offered by that class
  protected BaseEventListener needCreator;
  protected BaseEventListener matcher;
  protected BaseEventListener allNeedsDeactivator;
  protected BaseEventListener workDoneSignaller;
    private static final String NAME_NEEDS = "needs";

  @Override
  protected void initializeEventListeners()
  {
    EventListenerContext ctx = getEventListenerContext();
    EventBus bus = getEventBus();

    //create needs every trigger execution until 2 needs are created
    this.needCreator = new ExecuteOnEventListener(
        ctx,
        new CreateNeedAction(ctx,NAME_NEEDS),
        NO_OF_NEEDS
    );
    bus.subscribe(ActEvent.class,this.needCreator);

    this.matcher = new ExecuteOnceAfterNEventsListener(
            ctx,
        NO_OF_NEEDS, new MatchNeedsAction(ctx)
    );
    //count until 1 need is created, then create a comment facet
    bus.subscribe(NeedCreatedEvent.class, this.matcher);

     this.allNeedsDeactivator = new DeactivateAllNeedsOnHintListener(ctx);
     bus.subscribe(HintFromMatcherEvent.class, this.allNeedsDeactivator);

      this.workDoneSignaller = new ExecuteOnceAfterNEventsListener(
              ctx,
          2, new SignalWorkDoneAction(ctx)
      );
      bus.subscribe(NeedDeactivatedEvent.class, this.workDoneSignaller);

  }

}

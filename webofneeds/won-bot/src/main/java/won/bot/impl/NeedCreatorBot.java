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
import won.bot.framework.events.action.impl.CreateNeedWithFacetsAction;
import won.bot.framework.events.action.impl.SignalWorkDoneAction;
import won.bot.framework.events.bus.EventBus;
import won.bot.framework.events.event.Event;
import won.bot.framework.events.event.impl.ActEvent;
import won.bot.framework.events.event.impl.NeedCreatedEvent;
import won.bot.framework.events.event.impl.NeedProducerExhaustedEvent;
import won.bot.framework.events.listener.BaseEventListener;
import won.bot.framework.events.listener.impl.ActionOnEventListener;

/**
 *
 */
public class NeedCreatorBot extends EventBot
{

  protected BaseEventListener groupMemberCreator;
  protected BaseEventListener workDoneSignaller;

  @Override
  protected void initializeEventListeners() {
    final EventListenerContext ctx = getEventListenerContext();

    EventBus bus = getEventBus();

    //create needs every trigger execution until N needs are created
    this.groupMemberCreator = new ActionOnEventListener(
      ctx, "groupMemberCreator",
      new CreateNeedWithFacetsAction(ctx),
      -1
    );
    bus.subscribe(ActEvent.class, this.groupMemberCreator);

    bus.subscribe(NeedCreatedEvent.class, new ActionOnEventListener(ctx, "logger", new BaseEventBotAction(ctx)
    {
      private int count = 0;
      @Override
      protected void doRun(final Event event) throws Exception {
        int cnt = 0;
        synchronized (this){
          count++;
          cnt = count;
        }
        if (cnt % 200 == 0) {
          logger.info("created {} needs", cnt);
        }
      }
    }));
    //When the needproducer is exhausted, stop.
    this.workDoneSignaller = new ActionOnEventListener(
      ctx, "workDoneSignaller",
      new SignalWorkDoneAction(ctx),
      1
    );
    bus.subscribe(NeedProducerExhaustedEvent.class, this.workDoneSignaller);
  }

}

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
import won.bot.framework.events.listener.EventListener;
import won.bot.framework.events.listener.impl.ActionOnEventListener;
import won.bot.framework.events.listener.impl.ActionOnceAfterNEventsListener;

/**
 *
 */
public class RandomSimulatorBot extends EventBot
{

  private static final double PROB_OPEN_ON_HINT = 0.5;
  private static final double PROB_MESSAGE_ON_OPEN = 0.8;
  private static final double PROB_MESSAGE_ON_MESSAGE = 0.9;
  private static final long MIN_RECATION_TIMEOUT_MILLIS = 1000;
  private static final long MAX_REACTION_TIMEOUT_MILLIS = 60 * 1000;
  private static final long MIN_NEXT_CREATION_TIMEOUT_MILLIS = 100;
  private static final long MAX_NEXT_CREATION_TIMEOUT_MILLIS = 3 * 1000;

  protected BaseEventListener groupMemberCreator;
  protected BaseEventListener workDoneSignaller;



  @Override
  protected void initializeEventListeners() {
    final EventListenerContext ctx = getEventListenerContext();

    EventBus bus = getEventBus();

    //create the first need when the first actEvent happens
    this.groupMemberCreator = new ActionOnceAfterNEventsListener(
      ctx, "groupMemberCreator",1,
      new CreateNeedWithFacetsAction(ctx)
    );
    bus.subscribe(ActEvent.class, this.groupMemberCreator);

    //each time a need was created, wait for a random interval, then create another one
    bus.subscribe(NeedCreatedEvent.class, new ActionOnEventListener(ctx,
      new RandomDelayedAction(ctx,MIN_NEXT_CREATION_TIMEOUT_MILLIS, MAX_NEXT_CREATION_TIMEOUT_MILLIS,this.hashCode(),
      new CreateNeedWithFacetsAction(ctx)))
    );

    //print a logging message every N needs
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

    //when a hint is received, connect fraction of the cases after a random timeout
    bus.subscribe(HintFromMatcherEvent.class,
      new ActionOnEventListener(ctx, "hint-reactor",
        new RandomDelayedAction(ctx,MIN_RECATION_TIMEOUT_MILLIS, MAX_REACTION_TIMEOUT_MILLIS, (long) this.hashCode(),
          new ProbabilisticSelectionAction(ctx, PROB_OPEN_ON_HINT, (long) this.hashCode(),
            new OpenConnectionAction(ctx),
            new CloseConnectionAction(ctx)))));

    //when an open or connect is received, send message or close randomly after a random timeout
    EventListener opener =
      new ActionOnEventListener(ctx, "open-reactor",
        new RandomDelayedAction(ctx,MIN_RECATION_TIMEOUT_MILLIS, MAX_REACTION_TIMEOUT_MILLIS, (long) this.hashCode(),
          new ProbabilisticSelectionAction(ctx, PROB_MESSAGE_ON_OPEN, (long) this.hashCode(),
            new OpenConnectionAction(ctx),
            new CloseConnectionAction(ctx))));
    bus.subscribe(OpenFromOtherNeedEvent.class,opener);
    bus.subscribe(ConnectFromOtherNeedEvent.class,opener);

    //when an open is received, send message or close randomly after a random timeout
    EventListener replyer = new ActionOnEventListener(ctx, "message-reactor",
      new RandomDelayedAction(ctx,MIN_RECATION_TIMEOUT_MILLIS, MAX_REACTION_TIMEOUT_MILLIS, (long) this.hashCode(),
        new ProbabilisticSelectionAction(ctx, PROB_MESSAGE_ON_MESSAGE, (long) this.hashCode(),
          new SendMessageAction(ctx),
          new CloseConnectionAction(ctx))));
    bus.subscribe(MessageFromOtherNeedEvent.class, replyer);
    bus.subscribe(OpenFromOtherNeedEvent.class, replyer);


    //When the needproducer is exhausted, stop.
    this.workDoneSignaller = new ActionOnEventListener(
      ctx, "workDoneSignaller",
      new SignalWorkDoneAction(ctx),
      1
    );
    bus.subscribe(NeedProducerExhaustedEvent.class, this.workDoneSignaller);
  }

}

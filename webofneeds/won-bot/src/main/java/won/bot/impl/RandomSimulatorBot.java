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
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.action.impl.MultipleActions;
import won.bot.framework.eventbot.action.impl.ProbabilisticSelectionAction;
import won.bot.framework.eventbot.action.impl.RandomDelayedAction;
import won.bot.framework.eventbot.action.impl.counter.Counter;
import won.bot.framework.eventbot.action.impl.counter.CounterImpl;
import won.bot.framework.eventbot.action.impl.counter.DecrementCounterAction;
import won.bot.framework.eventbot.action.impl.counter.IncrementCounterAction;
import won.bot.framework.eventbot.action.impl.lifecycle.SignalWorkDoneAction;
import won.bot.framework.eventbot.action.impl.needlifecycle.CreateNeedWithFacetsAction;
import won.bot.framework.eventbot.action.impl.wonmessage.CloseConnectionAction;
import won.bot.framework.eventbot.action.impl.wonmessage.OpenConnectionAction;
import won.bot.framework.eventbot.action.impl.wonmessage.SendFeedbackForHintAction;
import won.bot.framework.eventbot.action.impl.wonmessage.SendMessageAction;
import won.bot.framework.eventbot.bus.EventBus;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.NeedCreationFailedEvent;
import won.bot.framework.eventbot.event.impl.lifecycle.ActEvent;
import won.bot.framework.eventbot.event.impl.needlifecycle.NeedCreatedEvent;
import won.bot.framework.eventbot.event.impl.needlifecycle.NeedProducerExhaustedEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.ConnectFromOtherNeedEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.HintFromMatcherEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.MessageFromOtherNeedEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.OpenFromOtherNeedEvent;
import won.bot.framework.eventbot.listener.BaseEventListener;
import won.bot.framework.eventbot.listener.EventListener;
import won.bot.framework.eventbot.listener.impl.ActionOnEventListener;
import won.bot.framework.eventbot.listener.impl.ActionOnceAfterNEventsListener;

/**
 *
 */
public class RandomSimulatorBot extends EventBot {
  private static final double PROB_OPEN_ON_HINT = 0.3;
  private static final double PROB_MESSAGE_ON_OPEN = 0.5;
  private static final double PROB_MESSAGE_ON_MESSAGE = 0.5;
  private static final long MIN_RECATION_TIMEOUT_MILLIS = 1 * 1000;
  private static final long MAX_REACTION_TIMEOUT_MILLIS = 3 * 1000;
  private static final long MIN_NEXT_CREATION_TIMEOUT_MILLIS = 1 * 1000;
  private static final long MAX_NEXT_CREATION_TIMEOUT_MILLIS = 3 * 1000;

  protected BaseEventListener groupMemberCreator;
  protected BaseEventListener workDoneSignaller;

  @Override protected void initializeEventListeners() {
    final EventListenerContext ctx = getEventListenerContext();

    EventBus bus = getEventBus();

    final Counter needCreationSuccessfulCounter = new CounterImpl("needsCreated");
    final Counter needCreationFailedCounter = new CounterImpl("needCreationFailed");
    final Counter needCreationStartedCounter = new CounterImpl("creationStarted");
    final Counter creationUnfinishedCounter = new CounterImpl("creationUnfinished");

    //create the first need when the first actEvent happens
    this.groupMemberCreator = new ActionOnceAfterNEventsListener(ctx, "groupMemberCreator", 1,
        new MultipleActions(ctx, new IncrementCounterAction(ctx, needCreationStartedCounter),
            new IncrementCounterAction(ctx, creationUnfinishedCounter),
            new CreateNeedWithFacetsAction(ctx, getBotContextWrapper().getNeedCreateListName())));
    bus.subscribe(ActEvent.class, this.groupMemberCreator);

    //when a need is created (or it failed), decrement the creationUnfinishedCounter
    EventListener downCounter = new ActionOnEventListener(ctx, "downCounter",
        new DecrementCounterAction(ctx, creationUnfinishedCounter));
    //count a successful need creation
    bus.subscribe(NeedCreatedEvent.class, downCounter);
    //if a creation failed, we don't want to keep us from keeping the correct count
    bus.subscribe(NeedCreationFailedEvent.class, downCounter);
    //we count the one execution when the creator realizes that the producer is exhausted, we have to count down
    //once for that, too.
    bus.subscribe(NeedProducerExhaustedEvent.class, downCounter);

    //also, keep track of what worked and what didn't
    bus.subscribe(NeedCreationFailedEvent.class,
        new ActionOnEventListener(ctx, new IncrementCounterAction(ctx, needCreationFailedCounter)));
    bus.subscribe(NeedCreatedEvent.class,
        new ActionOnEventListener(ctx, new IncrementCounterAction(ctx, needCreationSuccessfulCounter)));

    //print a logging message every N needs
    bus.subscribe(NeedCreatedEvent.class, new ActionOnEventListener(ctx, "logger", new BaseEventBotAction(ctx) {
      int lastOutput = 0;

      @Override protected void doRun(final Event event, EventListener executingListener) throws Exception {
        int cnt = needCreationStartedCounter.getCount();
        int unfinishedCount = creationUnfinishedCounter.getCount();
        int successCnt = needCreationSuccessfulCounter.getCount();
        int failedCnt = needCreationFailedCounter.getCount();
        if (cnt - lastOutput >= 200) {
          logger.info("started creation of {} needs, creation not yet finished for {}. Successful: {}, failed: {}",
              new Object[] { cnt, unfinishedCount, successCnt, failedCnt });
          lastOutput = cnt;
        }
      }
    }));

    //each time a need was created, wait for a random interval, then create another one
    bus.subscribe(NeedCreatedEvent.class, new ActionOnEventListener(ctx,
            new RandomDelayedAction(ctx, MIN_NEXT_CREATION_TIMEOUT_MILLIS, MAX_NEXT_CREATION_TIMEOUT_MILLIS,
                this.hashCode(), new CreateNeedWithFacetsAction(ctx, getBotContextWrapper().getNeedCreateListName()))));

    //when a hint is received, connect fraction of the cases after a random timeout
    bus.subscribe(HintFromMatcherEvent.class, new ActionOnEventListener(ctx, "hint-reactor",
            new RandomDelayedAction(ctx, MIN_RECATION_TIMEOUT_MILLIS, MAX_REACTION_TIMEOUT_MILLIS,
                (long) this.hashCode(), new MultipleActions(ctx, new SendFeedbackForHintAction(ctx),
                new ProbabilisticSelectionAction(ctx, PROB_OPEN_ON_HINT, (long) this.hashCode(),
                    new OpenConnectionAction(ctx, "Hi!"), new CloseConnectionAction(ctx, "Bye!"))))));

    //when an open or connect is received, send message or close randomly after a random timeout
    EventListener opener = new ActionOnEventListener(ctx, "open-reactor",
        new RandomDelayedAction(ctx, MIN_RECATION_TIMEOUT_MILLIS, MAX_REACTION_TIMEOUT_MILLIS, (long) this.hashCode(),
            new ProbabilisticSelectionAction(ctx, PROB_MESSAGE_ON_OPEN, (long) this.hashCode(),
                new OpenConnectionAction(ctx, "Hi!"), new CloseConnectionAction(ctx, "Bye!"))));
    bus.subscribe(OpenFromOtherNeedEvent.class, opener);
    bus.subscribe(ConnectFromOtherNeedEvent.class, opener);

    //when an open is received, send message or close randomly after a random timeout
    EventListener replyer = new ActionOnEventListener(ctx, "message-reactor",
        new RandomDelayedAction(ctx, MIN_RECATION_TIMEOUT_MILLIS, MAX_REACTION_TIMEOUT_MILLIS, (long) this.hashCode(),
            new ProbabilisticSelectionAction(ctx, PROB_MESSAGE_ON_MESSAGE, (long) this.hashCode(),
                new SendMessageAction(ctx), new CloseConnectionAction(ctx, "Bye!"))));
    bus.subscribe(MessageFromOtherNeedEvent.class, replyer);
    bus.subscribe(OpenFromOtherNeedEvent.class, replyer);

    //When the needproducer is exhausted, stop.
    this.workDoneSignaller = new ActionOnEventListener(ctx, "workDoneSignaller", new SignalWorkDoneAction(ctx), 1);
    bus.subscribe(NeedProducerExhaustedEvent.class, this.workDoneSignaller);
  }

}

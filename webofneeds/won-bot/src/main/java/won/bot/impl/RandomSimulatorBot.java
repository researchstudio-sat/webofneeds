/*
 * Copyright 2012 Research Studios Austria Forschungsges.m.b.H. Licensed under
 * the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable
 * law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package won.bot.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import won.bot.framework.eventbot.action.impl.atomlifecycle.CreateAtomWithSocketsAction;
import won.bot.framework.eventbot.action.impl.wonmessage.CloseConnectionAction;
import won.bot.framework.eventbot.action.impl.wonmessage.OpenConnectionAction;
import won.bot.framework.eventbot.action.impl.wonmessage.SendFeedbackForHintAction;
import won.bot.framework.eventbot.action.impl.wonmessage.SendMessageAction;
import won.bot.framework.eventbot.bus.EventBus;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.AtomCreationFailedEvent;
import won.bot.framework.eventbot.event.impl.lifecycle.ActEvent;
import won.bot.framework.eventbot.event.impl.atomlifecycle.AtomCreatedEvent;
import won.bot.framework.eventbot.event.impl.atomlifecycle.AtomProducerExhaustedEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.ConnectFromOtherAtomEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.AtomHintFromMatcherEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.MessageFromOtherAtomEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.OpenFromOtherAtomEvent;
import won.bot.framework.eventbot.listener.BaseEventListener;
import won.bot.framework.eventbot.listener.EventListener;
import won.bot.framework.eventbot.listener.impl.ActionOnEventListener;
import won.bot.framework.eventbot.listener.impl.ActionOnceAfterNEventsListener;

/**
 *
 */
public class RandomSimulatorBot extends EventBot {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private static final double PROB_OPEN_ON_HINT = 0.3;
    private static final double PROB_MESSAGE_ON_OPEN = 0.5;
    private static final double PROB_MESSAGE_ON_MESSAGE = 0.5;
    private static final long MIN_RECATION_TIMEOUT_MILLIS = 1 * 1000;
    private static final long MAX_REACTION_TIMEOUT_MILLIS = 3 * 1000;
    private static final long MIN_NEXT_CREATION_TIMEOUT_MILLIS = 1 * 1000;
    private static final long MAX_NEXT_CREATION_TIMEOUT_MILLIS = 3 * 1000;
    protected BaseEventListener groupMemberCreator;
    protected BaseEventListener workDoneSignaller;

    @Override
    protected void initializeEventListeners() {
        final EventListenerContext ctx = getEventListenerContext();
        EventBus bus = getEventBus();
        final Counter atomCreationSuccessfulCounter = new CounterImpl("atomsCreated");
        final Counter atomCreationFailedCounter = new CounterImpl("atomCreationFailed");
        final Counter atomCreationStartedCounter = new CounterImpl("creationStarted");
        final Counter creationUnfinishedCounter = new CounterImpl("creationUnfinished");
        // create the first atom when the first actEvent happens
        this.groupMemberCreator = new ActionOnceAfterNEventsListener(ctx, "groupMemberCreator", 1, new MultipleActions(
                        ctx, new IncrementCounterAction(ctx, atomCreationStartedCounter),
                        new IncrementCounterAction(ctx, creationUnfinishedCounter),
                        new CreateAtomWithSocketsAction(ctx, getBotContextWrapper().getAtomCreateListName())));
        bus.subscribe(ActEvent.class, this.groupMemberCreator);
        // when an atom is created (or it failed), decrement the
        // creationUnfinishedCounter
        EventListener downCounter = new ActionOnEventListener(ctx, "downCounter",
                        new DecrementCounterAction(ctx, creationUnfinishedCounter));
        // count a successful atom creation
        bus.subscribe(AtomCreatedEvent.class, downCounter);
        // if a creation failed, we don't want to keep us from keeping the correct count
        bus.subscribe(AtomCreationFailedEvent.class, downCounter);
        // we count the one execution when the creator realizes that the producer is
        // exhausted, we have to count down
        // once for that, too.
        bus.subscribe(AtomProducerExhaustedEvent.class, downCounter);
        // also, keep track of what worked and what didn't
        bus.subscribe(AtomCreationFailedEvent.class,
                        new ActionOnEventListener(ctx, new IncrementCounterAction(ctx, atomCreationFailedCounter)));
        bus.subscribe(AtomCreatedEvent.class,
                        new ActionOnEventListener(ctx, new IncrementCounterAction(ctx, atomCreationSuccessfulCounter)));
        // print a logging message every N atoms
        bus.subscribe(AtomCreatedEvent.class, new ActionOnEventListener(ctx, "logger", new BaseEventBotAction(ctx) {
            int lastOutput = 0;

            @Override
            protected void doRun(final Event event, EventListener executingListener) throws Exception {
                int cnt = atomCreationStartedCounter.getCount();
                int unfinishedCount = creationUnfinishedCounter.getCount();
                int successCnt = atomCreationSuccessfulCounter.getCount();
                int failedCnt = atomCreationFailedCounter.getCount();
                if (cnt - lastOutput >= 200) {
                    logger.info("started creation of {} atoms, creation not yet finished for {}. Successful: {}, failed: {}",
                                    new Object[] { cnt, unfinishedCount, successCnt, failedCnt });
                    lastOutput = cnt;
                }
            }
        }));
        // each time an atom was created, wait for a random interval, then create
        // another
        // one
        bus.subscribe(AtomCreatedEvent.class, new ActionOnEventListener(ctx, new RandomDelayedAction(ctx,
                        MIN_NEXT_CREATION_TIMEOUT_MILLIS, MAX_NEXT_CREATION_TIMEOUT_MILLIS, this.hashCode(),
                        new CreateAtomWithSocketsAction(ctx, getBotContextWrapper().getAtomCreateListName()))));
        // when a hint is received, connect fraction of the cases after a random timeout
        bus.subscribe(AtomHintFromMatcherEvent.class, new ActionOnEventListener(ctx, "hint-reactor",
                        new RandomDelayedAction(ctx, MIN_RECATION_TIMEOUT_MILLIS, MAX_REACTION_TIMEOUT_MILLIS,
                                        (long) this.hashCode(),
                                        new MultipleActions(ctx, new SendFeedbackForHintAction(ctx),
                                                        new ProbabilisticSelectionAction(ctx, PROB_OPEN_ON_HINT,
                                                                        (long) this.hashCode(),
                                                                        new OpenConnectionAction(ctx, "Hi!"),
                                                                        new CloseConnectionAction(ctx, "Bye!"))))));
        // when an open or connect is received, send message or close randomly after a
        // random timeout
        EventListener opener = new ActionOnEventListener(ctx, "open-reactor",
                        new RandomDelayedAction(ctx, MIN_RECATION_TIMEOUT_MILLIS, MAX_REACTION_TIMEOUT_MILLIS,
                                        (long) this.hashCode(),
                                        new ProbabilisticSelectionAction(ctx, PROB_MESSAGE_ON_OPEN,
                                                        (long) this.hashCode(), new OpenConnectionAction(ctx, "Hi!"),
                                                        new CloseConnectionAction(ctx, "Bye!"))));
        bus.subscribe(OpenFromOtherAtomEvent.class, opener);
        bus.subscribe(ConnectFromOtherAtomEvent.class, opener);
        // when an open is received, send message or close randomly after a random
        // timeout
        EventListener replyer = new ActionOnEventListener(ctx, "message-reactor",
                        new RandomDelayedAction(ctx, MIN_RECATION_TIMEOUT_MILLIS, MAX_REACTION_TIMEOUT_MILLIS,
                                        (long) this.hashCode(),
                                        new ProbabilisticSelectionAction(ctx, PROB_MESSAGE_ON_MESSAGE,
                                                        (long) this.hashCode(), new SendMessageAction(ctx),
                                                        new CloseConnectionAction(ctx, "Bye!"))));
        bus.subscribe(MessageFromOtherAtomEvent.class, replyer);
        bus.subscribe(OpenFromOtherAtomEvent.class, replyer);
        // When the atomproducer is exhausted, stop.
        this.workDoneSignaller = new ActionOnEventListener(ctx, "workDoneSignaller", new SignalWorkDoneAction(ctx), 1);
        bus.subscribe(AtomProducerExhaustedEvent.class, this.workDoneSignaller);
    }
}

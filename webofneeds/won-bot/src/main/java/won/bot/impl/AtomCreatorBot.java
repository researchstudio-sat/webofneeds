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
import won.bot.framework.eventbot.action.impl.atomlifecycle.CreateAtomWithSocketsAction;
import won.bot.framework.eventbot.action.impl.counter.*;
import won.bot.framework.eventbot.action.impl.listener.UnsubscribeListenerAction;
import won.bot.framework.eventbot.action.impl.monitor.MatchingLoadTestMonitorAction;
import won.bot.framework.eventbot.bus.EventBus;
import won.bot.framework.eventbot.event.AtomCreationFailedEvent;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.atomlifecycle.AtomCreatedEvent;
import won.bot.framework.eventbot.event.impl.atomlifecycle.AtomProducerExhaustedEvent;
import won.bot.framework.eventbot.event.impl.lifecycle.ActEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.AtomHintFromMatcherEvent;
import won.bot.framework.eventbot.listener.BaseEventListener;
import won.bot.framework.eventbot.listener.EventListener;
import won.bot.framework.eventbot.listener.impl.ActionOnEventListener;

import java.lang.invoke.MethodHandles;

/**
 *
 */
public class AtomCreatorBot extends EventBot {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    protected BaseEventListener groupMemberCreator;
    protected BaseEventListener workDoneSignaller;

    @Override
    protected void initializeEventListeners() {
        final EventListenerContext ctx = getEventListenerContext();
        final EventBus bus = getEventBus();
        final Counter atomCreationSuccessfulCounter = new CounterImpl("atomsCreated");
        final Counter atomCreationFailedCounter = new CounterImpl("atomCreationFailed");
        final Counter atomCreationStartedCounter = new CounterImpl("creationStarted");
        // create a targeted counter that will publish an event when the target is
        // reached
        // in this case, 0 unfinished atom creations means that all atoms were created
        final Counter creationUnfinishedCounter = new TargetCounterDecorator(ctx, new CounterImpl("creationUnfinished"),
                        0);
        // create atoms every trigger execution until the atom producer is exhausted
        this.groupMemberCreator = new ActionOnEventListener(ctx, "groupMemberCreator", new MultipleActions(ctx,
                        new IncrementCounterAction(ctx, atomCreationStartedCounter),
                        new IncrementCounterAction(ctx, creationUnfinishedCounter),
                        new CreateAtomWithSocketsAction(ctx, getBotContextWrapper().getAtomCreateListName())), -1);
        bus.subscribe(ActEvent.class, this.groupMemberCreator);
        bus.subscribe(AtomCreatedEvent.class, new ActionOnEventListener(ctx, "logger", new BaseEventBotAction(ctx) {
            int lastOutput = 0;

            @Override
            protected void doRun(final Event event, EventListener executingListener) throws Exception {
                int cnt = atomCreationStartedCounter.getCount();
                int unfinishedCount = creationUnfinishedCounter.getCount();
                int successCnt = atomCreationSuccessfulCounter.getCount();
                int failedCnt = atomCreationFailedCounter.getCount();
                if (cnt - lastOutput >= 1) {
                    logger.info("started creation of {} atoms, creation not yet finished for {}. Successful: {}, failed: {}",
                                    new Object[] { cnt, unfinishedCount, successCnt, failedCnt });
                    lastOutput = cnt;
                }
            }
        }));
        // When the atomproducer is exhausted, stop the creator.
        getEventBus().subscribe(AtomProducerExhaustedEvent.class,
                        new ActionOnEventListener(ctx, new UnsubscribeListenerAction(ctx, groupMemberCreator)));
        // also, keep track of what worked and what didn't
        bus.subscribe(AtomCreationFailedEvent.class,
                        new ActionOnEventListener(ctx, new IncrementCounterAction(ctx, atomCreationFailedCounter)));
        bus.subscribe(AtomCreatedEvent.class,
                        new ActionOnEventListener(ctx, new IncrementCounterAction(ctx, atomCreationSuccessfulCounter)));
        // when an atom is created (or it failed), decrement the halfCreatedAtom counter
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
        EventListener loadTestMonitor = new ActionOnEventListener(ctx, "loadTestMonitor",
                        new MatchingLoadTestMonitorAction(ctx));
        bus.subscribe(AtomCreatedEvent.class, loadTestMonitor);
        bus.subscribe(AtomHintFromMatcherEvent.class, loadTestMonitor);
        // wait for the targetCountReached event of the finishedCounter. We don't use
        // another target counter, so we don't need to do more filtering.
        // this.workDoneSignaller = new ActionOnEventListener(
        // ctx, "workDoneSignaller",
        // new SignalWorkDoneAction(ctx));
        // bus.subscribe(TargetCountReachedEvent.class, this.workDoneSignaller);
    }
}

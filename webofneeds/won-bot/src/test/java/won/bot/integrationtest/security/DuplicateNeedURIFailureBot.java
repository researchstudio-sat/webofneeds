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

package won.bot.integrationtest.security;

import won.bot.IntegrationtestBot;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.impl.LogAction;
import won.bot.framework.eventbot.action.impl.LogErrorAction;
import won.bot.framework.eventbot.action.impl.MultipleActions;
import won.bot.framework.eventbot.action.impl.PublishEventAction;
import won.bot.framework.eventbot.action.impl.lifecycle.SignalWorkDoneAction;
import won.bot.framework.eventbot.action.impl.needlifecycle.CreateNeedWithFacetsAction;
import won.bot.framework.eventbot.action.impl.needlifecycle.DeactivateAllNeedsAction;
import won.bot.framework.eventbot.bus.EventBus;
import won.bot.framework.eventbot.event.NeedCreationFailedEvent;
import won.bot.framework.eventbot.event.impl.lifecycle.ActEvent;
import won.bot.framework.eventbot.event.impl.needlifecycle.NeedCreatedEvent;
import won.bot.framework.eventbot.event.impl.needlifecycle.NeedDeactivatedEvent;
import won.bot.framework.eventbot.event.impl.test.SuccessEvent;
import won.bot.framework.eventbot.event.impl.test.TestFailedEvent;
import won.bot.framework.eventbot.event.impl.test.TestPassedEvent;
import won.bot.framework.eventbot.listener.impl.ActionOnEventListener;
import won.bot.framework.eventbot.listener.impl.ActionOnFirstNEventsListener;
import won.bot.framework.eventbot.listener.impl.ActionOnceAfterNEventsListener;
import won.bot.integrationtest.failsim.ConstantNewNeedURIDecorator;

/**
 *
 */
public class DuplicateNeedURIFailureBot extends IntegrationtestBot {
    @Override
    protected void initializeEventListeners() {
        EventListenerContext ctx = getEventListenerContext();
        EventBus bus = getEventBus();

        // create needs every trigger execution until 2 needs are created

        bus.subscribe(ActEvent.class, new ActionOnEventListener(ctx, new CreateNeedWithFacetsAction(
                // use a decorator that will cause the same need URI to be used in each create message
                new ConstantNewNeedURIDecorator(ctx, "constantNeedURI" + System.currentTimeMillis()),
                getBotContextWrapper().getNeedCreateListName()), 2));

        // log error if we can create 2 needs
        bus.subscribe(NeedCreatedEvent.class, new ActionOnceAfterNEventsListener(ctx, 2, new MultipleActions(ctx,
                new LogErrorAction(ctx, "Should not have been able to create 2 needs with identical URI"),
                new PublishEventAction(ctx,
                        new TestFailedEvent(this, "Should not have been able to create 2 needs with identical URI")))));

        // log success if we could create 1 need
        bus.subscribe(NeedCreatedEvent.class, new ActionOnFirstNEventsListener(ctx, 1, new MultipleActions(ctx,
                new LogAction(ctx, "Good: could create one need"), new PublishEventAction(ctx, new SuccessEvent()))));

        // log success if we got an error for 2nd need
        bus.subscribe(NeedCreationFailedEvent.class,
                new ActionOnFirstNEventsListener(ctx, 1,
                        new MultipleActions(ctx, new LogAction(ctx, "Good: need creation failed for 2nd need."),
                                new PublishEventAction(ctx, new SuccessEvent()))));

        // when we have 2 SuccessEvents, we're done. Deacivate the needs and signal we're finished
        bus.subscribe(SuccessEvent.class,
                new ActionOnceAfterNEventsListener(ctx, 2, new MultipleActions(ctx, new LogAction(ctx, "Test passed."),
                        new PublishEventAction(ctx, new TestPassedEvent(this)), new DeactivateAllNeedsAction(ctx))));

        // when we have a FailureEvent, we're done, too. Deacivate the needs and signal we're finished
        bus.subscribe(TestFailedEvent.class, new ActionOnceAfterNEventsListener(ctx, 1,
                new MultipleActions(ctx, new LogAction(ctx, "Test failed."), new DeactivateAllNeedsAction(ctx))));

        // wait for the needDeactivated event, then say we're done.
        bus.subscribe(NeedDeactivatedEvent.class,
                new ActionOnceAfterNEventsListener(ctx, 1, new SignalWorkDoneAction(ctx, this)));

        // TODO: fix: bot runs forever even if test fails.
        // TODO: fix: need 1 is not deactivated if test fails.
    }

}

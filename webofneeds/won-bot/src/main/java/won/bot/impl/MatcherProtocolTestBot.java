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
import won.bot.framework.eventbot.action.impl.LogAction;
import won.bot.framework.eventbot.action.impl.RandomDelayedAction;
import won.bot.framework.eventbot.action.impl.lifecycle.SignalWorkDoneAction;
import won.bot.framework.eventbot.action.impl.matcher.MatchNeedsAction;
import won.bot.framework.eventbot.action.impl.matcher.RegisterMatcherAction;
import won.bot.framework.eventbot.action.impl.needlifecycle.CreateNeedWithFacetsAction;
import won.bot.framework.eventbot.action.impl.needlifecycle.DeactivateAllNeedsAction;
import won.bot.framework.eventbot.bus.EventBus;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.lifecycle.ActEvent;
import won.bot.framework.eventbot.event.impl.matcher.MatcherRegisterFailedEvent;
import won.bot.framework.eventbot.event.impl.matcher.MatcherRegisteredEvent;
import won.bot.framework.eventbot.event.impl.matcher.NeedCreatedEventForMatcher;
import won.bot.framework.eventbot.event.impl.matcher.NeedDeactivatedEventForMatcher;
import won.bot.framework.eventbot.event.impl.needlifecycle.NeedCreatedEvent;
import won.bot.framework.eventbot.event.impl.needlifecycle.NeedDeactivatedEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.HintFromMatcherEvent;
import won.bot.framework.eventbot.listener.BaseEventListener;
import won.bot.framework.eventbot.listener.EventListener;
import won.bot.framework.eventbot.listener.impl.ActionOnEventListener;
import won.bot.framework.eventbot.listener.impl.ActionOnceAfterNEventsListener;

/**
 *
 */
public class MatcherProtocolTestBot extends EventBot {

  private static final int NO_OF_NEEDS = 2;

  // we use protected members so we can extend the class and
  // access the listeners for unit test assertions and stats
  //
  // we use BaseEventListener as their types so we can access the generic
  // functionality offered by that class
  protected BaseEventListener matcherRegistrator;
  protected BaseEventListener needCreator;
  protected BaseEventListener matcher;
  protected BaseEventListener allNeedsDeactivator;
  protected BaseEventListener workDoneSignaller;
  protected BaseEventListener matcherNotifier;
  private int registrationMatcherRetryInterval;

  public void setRegistrationMatcherRetryInterval(final int registrationMatcherRetryInterval) {
    this.registrationMatcherRetryInterval = registrationMatcherRetryInterval;
  }

  @Override
  protected void initializeEventListeners() {
    EventListenerContext ctx = getEventListenerContext();
    EventBus bus = getEventBus();

    // subscribe this bot with the WoN nodes' 'new need' topic
    RegisterMatcherAction registerMatcherAction = new RegisterMatcherAction(ctx);
    this.matcherRegistrator = new ActionOnEventListener(ctx, registerMatcherAction, 1);
    bus.subscribe(ActEvent.class, this.matcherRegistrator);
    RandomDelayedAction delayedRegistration = new RandomDelayedAction(ctx, registrationMatcherRetryInterval,
        registrationMatcherRetryInterval, 0, registerMatcherAction);
    ActionOnEventListener matcherRetryRegistrator = new ActionOnEventListener(ctx, delayedRegistration);
    bus.subscribe(MatcherRegisterFailedEvent.class, matcherRetryRegistrator);

    // create needs every trigger execution until 2 needs are created
    this.needCreator = new ActionOnEventListener(ctx,
        new CreateNeedWithFacetsAction(ctx, getBotContextWrapper().getNeedCreateListName()), NO_OF_NEEDS);

    bus.subscribe(MatcherRegisteredEvent.class, new ActionOnEventListener(ctx, new BaseEventBotAction(ctx) {
      @Override
      protected void doRun(final Event event, EventListener executingListener) throws Exception {
        getEventListenerContext().getEventBus().subscribe(ActEvent.class, needCreator);
      }
    }, 1));

    this.matcherNotifier = new ActionOnceAfterNEventsListener(ctx, 4,
        new LogAction(ctx, "Received all events for newly created needs."));
    bus.subscribe(NeedCreatedEventForMatcher.class, matcherNotifier);
    bus.subscribe(NeedCreatedEvent.class, matcherNotifier);

    this.matcher = new ActionOnceAfterNEventsListener(ctx, NO_OF_NEEDS, new MatchNeedsAction(ctx));
    // count until 1 need is created, then create a comment facet
    bus.subscribe(NeedCreatedEvent.class, this.matcher);

    this.allNeedsDeactivator = new ActionOnEventListener(ctx, new DeactivateAllNeedsAction(ctx), 1);
    bus.subscribe(HintFromMatcherEvent.class, this.allNeedsDeactivator);

    this.workDoneSignaller = new ActionOnceAfterNEventsListener(ctx, 4, new SignalWorkDoneAction(ctx));
    bus.subscribe(NeedDeactivatedEvent.class, this.workDoneSignaller);
    bus.subscribe(NeedDeactivatedEventForMatcher.class, this.workDoneSignaller);

  }

}

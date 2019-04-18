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

import won.bot.framework.bot.base.EventBot;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.action.impl.LogAction;
import won.bot.framework.eventbot.action.impl.RandomDelayedAction;
import won.bot.framework.eventbot.action.impl.lifecycle.SignalWorkDoneAction;
import won.bot.framework.eventbot.action.impl.matcher.MatchAtomsAction;
import won.bot.framework.eventbot.action.impl.matcher.RegisterMatcherAction;
import won.bot.framework.eventbot.action.impl.atomlifecycle.CreateAtomWithSocketsAction;
import won.bot.framework.eventbot.action.impl.atomlifecycle.DeactivateAllAtomsAction;
import won.bot.framework.eventbot.bus.EventBus;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.lifecycle.ActEvent;
import won.bot.framework.eventbot.event.impl.matcher.MatcherRegisterFailedEvent;
import won.bot.framework.eventbot.event.impl.matcher.MatcherRegisteredEvent;
import won.bot.framework.eventbot.event.impl.matcher.AtomCreatedEventForMatcher;
import won.bot.framework.eventbot.event.impl.matcher.AtomDeactivatedEventForMatcher;
import won.bot.framework.eventbot.event.impl.atomlifecycle.AtomCreatedEvent;
import won.bot.framework.eventbot.event.impl.atomlifecycle.AtomDeactivatedEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.HintFromMatcherEvent;
import won.bot.framework.eventbot.listener.BaseEventListener;
import won.bot.framework.eventbot.listener.EventListener;
import won.bot.framework.eventbot.listener.impl.ActionOnEventListener;
import won.bot.framework.eventbot.listener.impl.ActionOnceAfterNEventsListener;

/**
 *
 */
public class MatcherProtocolTestBot extends EventBot {
    private static final int NO_OF_ATOMS = 2;
    // we use protected members so we can extend the class and
    // access the listeners for unit test assertions and stats
    //
    // we use BaseEventListener as their types so we can access the generic
    // functionality offered by that class
    protected BaseEventListener matcherRegistrator;
    protected BaseEventListener atomCreator;
    protected BaseEventListener matcher;
    protected BaseEventListener allAtomsDeactivator;
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
        // subscribe this bot with the WoN nodes' 'new atom' topic
        RegisterMatcherAction registerMatcherAction = new RegisterMatcherAction(ctx);
        this.matcherRegistrator = new ActionOnEventListener(ctx, registerMatcherAction, 1);
        bus.subscribe(ActEvent.class, this.matcherRegistrator);
        RandomDelayedAction delayedRegistration = new RandomDelayedAction(ctx, registrationMatcherRetryInterval,
                        registrationMatcherRetryInterval, 0, registerMatcherAction);
        ActionOnEventListener matcherRetryRegistrator = new ActionOnEventListener(ctx, delayedRegistration);
        bus.subscribe(MatcherRegisterFailedEvent.class, matcherRetryRegistrator);
        // create atoms every trigger execution until 2 atoms are created
        this.atomCreator = new ActionOnEventListener(ctx,
                        new CreateAtomWithSocketsAction(ctx, getBotContextWrapper().getAtomCreateListName()),
                        NO_OF_ATOMS);
        bus.subscribe(MatcherRegisteredEvent.class, new ActionOnEventListener(ctx, new BaseEventBotAction(ctx) {
            @Override
            protected void doRun(final Event event, EventListener executingListener) throws Exception {
                getEventListenerContext().getEventBus().subscribe(ActEvent.class, atomCreator);
            }
        }, 1));
        this.matcherNotifier = new ActionOnceAfterNEventsListener(ctx, 4,
                        new LogAction(ctx, "Received all events for newly created atoms."));
        bus.subscribe(AtomCreatedEventForMatcher.class, matcherNotifier);
        bus.subscribe(AtomCreatedEvent.class, matcherNotifier);
        this.matcher = new ActionOnceAfterNEventsListener(ctx, NO_OF_ATOMS, new MatchAtomsAction(ctx));
        // count until 1 atom is created, then create a comment socket
        bus.subscribe(AtomCreatedEvent.class, this.matcher);
        this.allAtomsDeactivator = new ActionOnEventListener(ctx, new DeactivateAllAtomsAction(ctx), 1);
        bus.subscribe(HintFromMatcherEvent.class, this.allAtomsDeactivator);
        this.workDoneSignaller = new ActionOnceAfterNEventsListener(ctx, 4, new SignalWorkDoneAction(ctx));
        bus.subscribe(AtomDeactivatedEvent.class, this.workDoneSignaller);
        bus.subscribe(AtomDeactivatedEventForMatcher.class, this.workDoneSignaller);
    }
}

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
import won.bot.framework.eventbot.action.EventBotAction;
import won.bot.framework.eventbot.action.impl.LogAction;
import won.bot.framework.eventbot.action.impl.MultipleActions;
import won.bot.framework.eventbot.action.impl.RandomDelayedAction;
import won.bot.framework.eventbot.action.impl.matcher.RegisterMatcherAction;
import won.bot.framework.eventbot.action.impl.needlifecycle.CreateEchoNeedWithFacetsAction;
import won.bot.framework.eventbot.action.impl.wonmessage.ConnectWithAssociatedNeedAction;
import won.bot.framework.eventbot.action.impl.wonmessage.RespondWithEchoToMessageAction;
import won.bot.framework.eventbot.bus.EventBus;
import won.bot.framework.eventbot.event.impl.lifecycle.ActEvent;
import won.bot.framework.eventbot.event.impl.matcher.MatcherRegisterFailedEvent;
import won.bot.framework.eventbot.event.impl.matcher.NeedCreatedEventForMatcher;
import won.bot.framework.eventbot.event.impl.needlifecycle.NeedCreatedEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.CloseFromOtherNeedEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.MessageFromOtherNeedEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.OpenFromOtherNeedEvent;
import won.bot.framework.eventbot.filter.impl.NeedUriInNamedListFilter;
import won.bot.framework.eventbot.filter.impl.NotFilter;
import won.bot.framework.eventbot.listener.BaseEventListener;
import won.bot.framework.eventbot.listener.impl.ActionOnEventListener;
import won.protocol.model.FacetType;

/**
 * Bot that creates a new 'Re:' need for each need that is created in the system and connects. When a connection is
 * established, all messages are just echoed.
 */
public class EchoBot extends EventBot {
    private BaseEventListener matcherRegistrator;
    protected BaseEventListener needCreator;
    protected BaseEventListener needConnector;
    protected BaseEventListener autoOpener;
    protected BaseEventListener autoResponder;
    protected BaseEventListener connectionCloser;
    protected BaseEventListener needDeactivator;
    private Integer numberOfEchoNeedsPerNeed;
    private int registrationMatcherRetryInterval;

    public void setRegistrationMatcherRetryInterval(final int registrationMatcherRetryInterval) {
        this.registrationMatcherRetryInterval = registrationMatcherRetryInterval;
    }

    @Override
    protected void initializeEventListeners() {
        EventListenerContext ctx = getEventListenerContext();
        EventBus bus = getEventBus();

        // register with WoN nodes, be notified when new needs are created
        RegisterMatcherAction registerMatcherAction = new RegisterMatcherAction(ctx);
        this.matcherRegistrator = new ActionOnEventListener(ctx, registerMatcherAction, 1);
        bus.subscribe(ActEvent.class, this.matcherRegistrator);
        RandomDelayedAction delayedRegistration = new RandomDelayedAction(ctx, registrationMatcherRetryInterval,
                registrationMatcherRetryInterval, 0, registerMatcherAction);
        ActionOnEventListener matcherRetryRegistrator = new ActionOnEventListener(ctx, delayedRegistration);
        bus.subscribe(MatcherRegisterFailedEvent.class, matcherRetryRegistrator);

        // create the echo need - if we're not reacting to the creation of our own echo need.
        this.needCreator = new ActionOnEventListener(ctx,
                new NotFilter(new NeedUriInNamedListFilter(ctx, ctx.getBotContextWrapper().getNeedCreateListName())),
                prepareCreateNeedAction(ctx));
        bus.subscribe(NeedCreatedEventForMatcher.class, this.needCreator);

        // as soon as the echo need is created, connect to original
        this.needConnector = new ActionOnEventListener(ctx, "needConnector", new RandomDelayedAction(ctx, 5000, 5000, 1,
                new ConnectWithAssociatedNeedAction(ctx, FacetType.ChatFacet.getURI(), FacetType.ChatFacet.getURI(),
                        "Greetings! I am the EchoBot! I will repeat everything you say, which you might "
                                + "find useful for testing purposes.")));
        bus.subscribe(NeedCreatedEvent.class, this.needConnector);

        // add a listener that auto-responds to messages by a message
        // after 10 messages, it unsubscribes from all events
        // subscribe it to:
        // * message events - so it responds
        // * open events - so it initiates the chain reaction of responses
        this.autoResponder = new ActionOnEventListener(ctx, new RespondWithEchoToMessageAction(ctx));
        bus.subscribe(OpenFromOtherNeedEvent.class, this.autoResponder);
        bus.subscribe(MessageFromOtherNeedEvent.class, this.autoResponder);

        bus.subscribe(CloseFromOtherNeedEvent.class,
                new ActionOnEventListener(ctx, new LogAction(ctx, "received close message from remote need.")));
    }

    private EventBotAction prepareCreateNeedAction(final EventListenerContext ctx) {
        if (numberOfEchoNeedsPerNeed == null) {
            return new CreateEchoNeedWithFacetsAction(ctx);
        } else {
            CreateEchoNeedWithFacetsAction[] actions = new CreateEchoNeedWithFacetsAction[numberOfEchoNeedsPerNeed];
            for (int i = 0; i < numberOfEchoNeedsPerNeed; i++) {
                actions[i] = new CreateEchoNeedWithFacetsAction(ctx);
            }
            return new MultipleActions(ctx, actions);
        }
    }

    public void setNumberOfEchoNeedsPerNeed(final Integer numberOfEchoNeedsPerNeed) {
        this.numberOfEchoNeedsPerNeed = numberOfEchoNeedsPerNeed;
    }
}

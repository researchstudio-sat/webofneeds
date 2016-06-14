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
import won.bot.framework.events.action.EventBotAction;
import won.bot.framework.events.action.impl.*;
import won.bot.framework.events.action.impl.matcher.RegisterMatcherAction;
import won.bot.framework.events.action.impl.needlifecycle.CreateEchoNeedWithFacetsAction;
import won.bot.framework.events.action.impl.wonmessage.ConnectWithAssociatedNeedAction;
import won.bot.framework.events.action.impl.wonmessage.RespondWithEchoToMessageAction;
import won.bot.framework.events.bus.EventBus;
import won.bot.framework.events.event.impl.lifecycle.ActEvent;
import won.bot.framework.events.event.impl.matcher.NeedCreatedEventForMatcher;
import won.bot.framework.events.event.impl.needlifecycle.NeedCreatedEvent;
import won.bot.framework.events.event.impl.wonmessage.CloseFromOtherNeedEvent;
import won.bot.framework.events.event.impl.wonmessage.MessageFromOtherNeedEvent;
import won.bot.framework.events.event.impl.wonmessage.OpenFromOtherNeedEvent;
import won.bot.framework.events.filter.impl.NeedUriInNamedListFilter;
import won.bot.framework.events.filter.impl.NotFilter;
import won.bot.framework.events.listener.BaseEventListener;
import won.bot.framework.events.listener.impl.ActionOnEventListener;
import won.protocol.model.FacetType;

/**
 * Bot that creates a new 'Re:' need for each need that is created in the system and connects. When a connection is established,
 * all messages are just echoed.
 */
public class EchoBot extends EventBot
{

  private static final String NAME_NEEDS = "echoNeeds";
  private BaseEventListener matcherRegistrator;
  protected BaseEventListener needCreator;
  protected BaseEventListener needConnector;
  protected BaseEventListener autoOpener;
  protected BaseEventListener autoResponder;
  protected BaseEventListener connectionCloser;
  protected BaseEventListener needDeactivator;

  private Integer numberOfEchoNeedsPerNeed;

  @Override
  protected void initializeEventListeners()
  {
    EventListenerContext ctx = getEventListenerContext();
    EventBus bus = getEventBus();

    //register with WoN noodes, be notified when new needs are created
    this.matcherRegistrator = new ActionOnEventListener(
      ctx,
      new RegisterMatcherAction(ctx),
      1
    );
    bus.subscribe(ActEvent.class, this.matcherRegistrator);

    //create the echo need - if we're not reacting to the creation of our own echo need.
    this.needCreator = new ActionOnEventListener(
            ctx,
            new NotFilter(new NeedUriInNamedListFilter(ctx, NAME_NEEDS)),
            prepareCreateNeedAction(ctx)
            );
    bus.subscribe(NeedCreatedEventForMatcher.class, this.needCreator);

    //as soon as the echo need is created, connect to original
    this.needConnector =
            new ActionOnEventListener(
                    ctx,
                    "needConnector",
                    new RandomDelayedAction(ctx, 5000,5000,1,
                        new ConnectWithAssociatedNeedAction(ctx, FacetType.OwnerFacet.getURI(), FacetType.OwnerFacet
                          .getURI(), "Greetings! I am the EchoBot! I will repeat everything you say, which you might " +
                          "find useful for testing purposes.")));
    bus.subscribe(NeedCreatedEvent.class, this.needConnector);


    //add a listener that auto-responds to messages by a message
    //after 10 messages, it unsubscribes from all events
    //subscribe it to:
    // * message events - so it responds
    // * open events - so it initiates the chain reaction of responses
    this.autoResponder = new ActionOnEventListener(ctx, new RespondWithEchoToMessageAction(ctx));
    bus.subscribe(OpenFromOtherNeedEvent.class, this.autoResponder);
    bus.subscribe(MessageFromOtherNeedEvent.class, this.autoResponder);

    bus.subscribe(CloseFromOtherNeedEvent.class,
                  new ActionOnEventListener(ctx, new LogAction(ctx,"received close message from remote need.")));
  }

  private EventBotAction prepareCreateNeedAction(final EventListenerContext ctx) {
    if (numberOfEchoNeedsPerNeed == null) {
      return new CreateEchoNeedWithFacetsAction(ctx, NAME_NEEDS);
    } else {
      CreateEchoNeedWithFacetsAction[] actions = new CreateEchoNeedWithFacetsAction[numberOfEchoNeedsPerNeed];
      for (int i = 0; i < numberOfEchoNeedsPerNeed; i++) {
        actions[i] = new CreateEchoNeedWithFacetsAction(ctx,NAME_NEEDS);
      }
      return new MultipleActions(ctx, actions);
    }
  }

  public void setNumberOfEchoNeedsPerNeed(final Integer numberOfEchoNeedsPerNeed) {
    this.numberOfEchoNeedsPerNeed = numberOfEchoNeedsPerNeed;
  }
}

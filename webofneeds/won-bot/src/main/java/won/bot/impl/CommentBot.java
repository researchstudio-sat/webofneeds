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
import won.bot.framework.bot.context.CommentBotContextWrapper;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.action.impl.lifecycle.SignalWorkDoneAction;
import won.bot.framework.eventbot.action.impl.needlifecycle.CreateNeedWithFacetsAction;
import won.bot.framework.eventbot.action.impl.needlifecycle.DeactivateAllNeedsAction;
import won.bot.framework.eventbot.action.impl.wonmessage.ConnectFromListToListAction;
import won.bot.framework.eventbot.action.impl.wonmessage.OpenConnectionAction;
import won.bot.framework.eventbot.bus.EventBus;
import won.bot.framework.eventbot.event.BaseEvent;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.lifecycle.ActEvent;
import won.bot.framework.eventbot.event.impl.needlifecycle.NeedCreatedEvent;
import won.bot.framework.eventbot.event.impl.needlifecycle.NeedDeactivatedEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.ConnectFromOtherNeedEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.OpenFromOtherNeedEvent;
import won.bot.framework.eventbot.listener.BaseEventListener;
import won.bot.framework.eventbot.listener.EventListener;
import won.bot.framework.eventbot.listener.impl.ActionOnEventListener;
import won.bot.framework.eventbot.listener.impl.ActionOnceAfterNEventsListener;
import won.protocol.model.FacetType;

/**
 *
 */
public class CommentBot extends EventBot {
  private static final int NO_OF_NEEDS = 1;
  private static final long MILLIS_BETWEEN_MESSAGES = 10;

  // we use protected members so we can extend the class and
  // access the listeners for unit test assertions and stats
  //
  // we use BaseEventListener as their types so we can access the generic
  // functionality offered by that class
  protected BaseEventListener needCreator;
  protected BaseEventListener commentFacetCreator;
  protected BaseEventListener needConnector;
  protected BaseEventListener autoOpener;
  protected BaseEventListener autoResponder;
  protected BaseEventListener connectionCloser;
  protected BaseEventListener allNeedsDeactivator;
  protected BaseEventListener needDeactivator;
  protected BaseEventListener workDoneSignaller;

  @Override
  protected void initializeEventListeners() {
    EventListenerContext ctx = getEventListenerContext();
    final EventBus bus = getEventBus();

    CommentBotContextWrapper botContextWrapper = (CommentBotContextWrapper) getBotContextWrapper();

    // create needs every trigger execution until 2 needs are created
    this.needCreator = new ActionOnEventListener(ctx,
        new CreateNeedWithFacetsAction(ctx, botContextWrapper.getNeedCreateListName()), NO_OF_NEEDS);
    bus.subscribe(ActEvent.class, this.needCreator);

    // count until 1 need is created, then create a comment facet
    this.commentFacetCreator = new ActionOnEventListener(ctx,
        new CreateNeedWithFacetsAction(ctx, botContextWrapper.getCommentListName(), FacetType.CommentFacet.getURI()),
        1);
    bus.subscribe(NeedCreatedEvent.class, this.commentFacetCreator);

    this.needConnector = new ActionOnceAfterNEventsListener(ctx, 2,
        new ConnectFromListToListAction(ctx, botContextWrapper.getNeedCreateListName(),
            botContextWrapper.getCommentListName(), FacetType.ChatFacet.getURI(), FacetType.CommentFacet.getURI(),
            MILLIS_BETWEEN_MESSAGES, "Hi, I am the " + "CommentBot."));
    bus.subscribe(NeedCreatedEvent.class, this.needConnector);

    this.autoOpener = new ActionOnEventListener(ctx, new OpenConnectionAction(ctx, "Hi!"));
    bus.subscribe(OpenFromOtherNeedEvent.class, this.autoOpener);
    bus.subscribe(ConnectFromOtherNeedEvent.class, this.autoOpener);

    BaseEventListener assertionRunner = new ActionOnceAfterNEventsListener(ctx, 1, new BaseEventBotAction(ctx) {
      @Override
      protected void doRun(final Event event, EventListener executingListener) throws Exception {
        executeAssertionsForEstablishedConnectionInternal(bus);
      }
    });

    bus.subscribe(OpenFromOtherNeedEvent.class, assertionRunner);

    // deactivate all needs when the assertion was executed
    this.allNeedsDeactivator = new ActionOnEventListener(ctx, new DeactivateAllNeedsAction(ctx), 1);
    bus.subscribe(AssertionsExecutedEvent.class, this.allNeedsDeactivator);

    // add a listener that counts two NeedDeactivatedEvents and then tells the
    // framework that the bot's work is done
    this.workDoneSignaller = new ActionOnceAfterNEventsListener(ctx, 2, new SignalWorkDoneAction(ctx));
    bus.subscribe(NeedDeactivatedEvent.class, this.workDoneSignaller);

  }

  private void executeAssertionsForEstablishedConnectionInternal(EventBus bus) {
    executeAssertionsForEstablishedConnection();
    bus.publish(new AssertionsExecutedEvent());
  }

  protected void executeAssertionsForEstablishedConnection() {

  }

  private class AssertionsExecutedEvent extends BaseEvent {
    private AssertionsExecutedEvent() {
    }
  }

}

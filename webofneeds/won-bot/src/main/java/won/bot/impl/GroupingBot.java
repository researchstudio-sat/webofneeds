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

import java.util.ArrayList;
import java.util.List;

import won.bot.framework.bot.base.EventBot;
import won.bot.framework.bot.context.GroupBotContextWrapper;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.action.impl.lifecycle.SignalWorkDoneAction;
import won.bot.framework.eventbot.action.impl.needlifecycle.CreateNeedWithFacetsAction;
import won.bot.framework.eventbot.action.impl.needlifecycle.DeactivateAllNeedsOfListAction;
import won.bot.framework.eventbot.action.impl.wonmessage.ConnectFromListToListAction;
import won.bot.framework.eventbot.action.impl.wonmessage.OpenConnectionAction;
import won.bot.framework.eventbot.action.impl.wonmessage.RespondToMessageAction;
import won.bot.framework.eventbot.bus.EventBus;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.lifecycle.ActEvent;
import won.bot.framework.eventbot.event.impl.listener.FinishedEvent;
import won.bot.framework.eventbot.event.impl.needlifecycle.NeedCreatedEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.CloseFromOtherNeedEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.ConnectFromOtherNeedEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.MessageFromOtherNeedEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.OpenFromOtherNeedEvent;
import won.bot.framework.eventbot.filter.impl.AcceptOnceFilter;
import won.bot.framework.eventbot.filter.impl.FinishedEventFilter;
import won.bot.framework.eventbot.filter.impl.NeedUriEventFilter;
import won.bot.framework.eventbot.filter.impl.NeedUriInNamedListFilter;
import won.bot.framework.eventbot.filter.impl.OrFilter;
import won.bot.framework.eventbot.listener.BaseEventListener;
import won.bot.framework.eventbot.listener.EventListener;
import won.bot.framework.eventbot.listener.impl.ActionOnEventListener;
import won.bot.framework.eventbot.listener.impl.ActionOnceAfterNEventsListener;
import won.bot.framework.eventbot.listener.impl.AutomaticMessageResponderListener;
import won.bot.framework.eventbot.listener.impl.WaitForNEventsListener;
import won.protocol.model.FacetType;
import won.protocol.util.WonRdfUtils;

/**
 *
 */
public class GroupingBot extends EventBot {

  protected static final int NO_OF_GROUPMEMBERS = 5;
  protected static final int NO_OF_MESSAGES = 5;
  protected static final long MILLIS_BETWEEN_MESSAGES = 1;
  // we use protected members so we can extend the class and
  // access the listeners for unit test assertions and stats
  //
  // we use BaseEventListener as their types so we can access the generic
  // functionality offered by that class
  protected BaseEventListener groupMemberCreator;
  protected BaseEventListener groupCreator;
  protected BaseEventListener needConnector;
  protected BaseEventListener autoOpener;
  protected BaseEventListener autoResponderCreator;
  protected BaseEventListener receiverFinishedListener;
  protected BaseEventListener messagesDoneListener;
  protected BaseEventListener conversationStarter;
  protected BaseEventListener workDoneSignaller;
  protected List<BaseEventListener> autoResponders;
  protected List<BaseEventListener> messageCounters;

  @Override
  protected void initializeEventListeners() {
    final EventListenerContext ctx = getEventListenerContext();
    GroupBotContextWrapper botContextWrapper = (GroupBotContextWrapper) getBotContextWrapper();

    EventBus bus = getEventBus();

    // for each created need (in the group), add a listener that will auto-respond
    // to messages directed at that need
    // create a filter that only accepts events for needs in the group:
    NeedUriInNamedListFilter groupMemberFilter = new NeedUriInNamedListFilter(ctx,
        botContextWrapper.getGroupMembersListName());
    // remember the auto-responders in a list
    this.autoResponders = new ArrayList<BaseEventListener>();
    // remember the listeners that wait for all messages
    this.messageCounters = new ArrayList<BaseEventListener>();
    // make a composite filter, with one filter for each autoResponder that wait for
    // the FinishedEvents the responders emit.
    // that filter will be used to shut down all needs after all the autoResponders
    // have finished.
    final OrFilter mainAutoResponderFilter = new OrFilter();
    // listen to NeedCreatedEvents
    this.autoResponderCreator = new ActionOnEventListener(ctx, groupMemberFilter, new BaseEventBotAction(ctx) {
      @Override
      protected void doRun(final Event event, EventListener executingListener) throws Exception {
        // create a listener that automatically answers messages, only for that need
        // URI. We let it send NO_OF_MESSAGES messages
        logger.debug("created auto responder");
        AutomaticMessageResponderListener listener = new AutomaticMessageResponderListener(ctx, "autoResponder",
            NeedUriEventFilter.forEvent(event), NO_OF_MESSAGES, MILLIS_BETWEEN_MESSAGES);

        // create a listener that publishes a FinishedEvent after having received all
        // messages from the group
        WaitForNEventsListener waitForMessagesListener = new WaitForNEventsListener(ctx, "messageCounter",
            NeedUriEventFilter.forEvent(event), NO_OF_MESSAGES * (NO_OF_GROUPMEMBERS - 1));
        messageCounters.add(waitForMessagesListener);
        // add a filter that will wait for the FinishedEvent emitted by that listener
        // wrap it in an acceptonce filter to make extra sure we count each listener
        // only once.
        mainAutoResponderFilter.addFilter(new AcceptOnceFilter(new FinishedEventFilter(waitForMessagesListener)));

        ActionOnEventListener debugger = new ActionOnEventListener(ctx, NeedUriEventFilter.forEvent(event),
            new BaseEventBotAction(ctx) {
              @Override
              protected void doRun(Event event, EventListener executingListener) throws Exception {
                if (event instanceof MessageFromOtherNeedEvent) {
                  MessageFromOtherNeedEvent msg = (MessageFromOtherNeedEvent) event;
                  logger.debug("processing event {} wonMessage {} - text message '{}', sent by {} to {}",
                      new Object[] { event.toString(), msg.getWonMessage().getMessageURI(),
                          WonRdfUtils.MessageUtils.getTextMessage(msg.getWonMessage()), msg.getRemoteNeedURI(),
                          msg.getNeedURI() });
                }
              }
            });
        getEventBus().subscribe(MessageFromOtherNeedEvent.class, debugger);

        // finally, subscribe to the message events
        getEventBus().subscribe(MessageFromOtherNeedEvent.class, waitForMessagesListener);
        getEventBus().subscribe(MessageFromOtherNeedEvent.class, listener);
      }
    });
    getEventBus().subscribe(NeedCreatedEvent.class, this.autoResponderCreator);

    // count until N needs were created, then create need with group facet (the
    // others will connect to that facet)
    this.groupCreator = new ActionOnceAfterNEventsListener(ctx, "groupCreator", NO_OF_GROUPMEMBERS,
        new CreateNeedWithFacetsAction(ctx, botContextWrapper.getGroupListName(), FacetType.GroupFacet.getURI()));
    bus.subscribe(NeedCreatedEvent.class, this.groupCreator);

    // wait for N+1 needCreatedEvents, then connect the members with the group facet
    // of the third need
    this.needConnector = new ActionOnceAfterNEventsListener(ctx, "needConnector", NO_OF_GROUPMEMBERS + 1,
        new ConnectFromListToListAction(ctx, botContextWrapper.getGroupListName(),
            botContextWrapper.getGroupMembersListName(), FacetType.GroupFacet.getURI(), FacetType.ChatFacet.getURI(),
            MILLIS_BETWEEN_MESSAGES, "Hi from the " + "GroupingBot!"));
    bus.subscribe(NeedCreatedEvent.class, this.needConnector);

    // add a listener that is informed of the connect/open events and that
    // auto-opens
    // subscribe it to:
    // * connect events - so it responds with open
    // * open events - so it responds with open (if the open received was the first
    // open, and we still need to accept the connection)
    this.autoOpener = new ActionOnEventListener(ctx, new OpenConnectionAction(ctx, "Hi from the GroupingBot!"));
    bus.subscribe(ConnectFromOtherNeedEvent.class, this.autoOpener);

    // now, once all connections have been opened, make 1 bot send a message to the
    // group, the subsequent listener will cause let wild chatting to begin
    this.conversationStarter = new ActionOnceAfterNEventsListener(ctx, "conversationStarter", NO_OF_GROUPMEMBERS,
        new RespondToMessageAction(ctx, MILLIS_BETWEEN_MESSAGES));
    bus.subscribe(OpenFromOtherNeedEvent.class, this.conversationStarter);

    // for each group member, there are 2 listeners waiting for messages. when they
    // are all finished, we're done.
    this.messagesDoneListener = new ActionOnceAfterNEventsListener(ctx, "messagesDoneListener", mainAutoResponderFilter,
        NO_OF_GROUPMEMBERS, new DeactivateAllNeedsOfListAction(ctx, botContextWrapper.getGroupMembersListName()));
    bus.subscribe(FinishedEvent.class, this.messagesDoneListener);

    // When the group facet need is deactivated, all connections are closed. wait
    // for the close events and signal work done.
    this.workDoneSignaller = new ActionOnceAfterNEventsListener(ctx, "workDoneSignaller", NO_OF_GROUPMEMBERS,
        new SignalWorkDoneAction(ctx));
    bus.subscribe(CloseFromOtherNeedEvent.class, this.workDoneSignaller);

    // start the whole thing:

    // create needs every trigger execution until N needs are created
    this.groupMemberCreator = new ActionOnEventListener(ctx, "groupMemberCreator",
        new CreateNeedWithFacetsAction(ctx, botContextWrapper.getGroupMembersListName(), FacetType.ChatFacet.getURI()),
        NO_OF_GROUPMEMBERS);
    bus.subscribe(ActEvent.class, this.groupMemberCreator);
  }

}

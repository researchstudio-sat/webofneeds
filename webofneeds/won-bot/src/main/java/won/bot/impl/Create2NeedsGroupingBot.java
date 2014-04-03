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
import won.bot.framework.events.Event;
import won.bot.framework.events.EventBus;
import won.bot.framework.events.event.*;
import won.bot.framework.events.listener.*;
import won.bot.framework.events.listener.action.*;
import won.bot.framework.events.listener.filter.*;
import won.protocol.model.FacetType;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class Create2NeedsGroupingBot extends EventBot
{

  protected static final int NO_OF_GROUPMEMBERS = 5;
  protected static final int NO_OF_MESSAGES = 5;
  protected static final long MILLIS_BETWEEN_MESSAGES = 10;
  protected static final String NAME_GROUPS = "groups";
  protected static final String NAME_GROUPMEMBERS = "groupmembers";
  //we use protected members so we can extend the class and
  //access the listeners for unit test assertions and stats
  //
  //we use BaseEventListener as their types so we can access the generic
  //functionality offered by that class
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

    EventBus bus = getEventBus();

    //create needs every trigger execution until N needs are created
    this.groupMemberCreator = new ExecuteOnEventListener(
      ctx, "groupMemberCreator",
      new CreateNeedAction(ctx, NAME_GROUPMEMBERS),
      NO_OF_GROUPMEMBERS
    );
    bus.subscribe(ActEvent.class, this.groupMemberCreator);

    //for each created need (in the group), add a listener that will auto-respond to messages directed at that need
    //create a filter that only accepts events for needs in the group:
    NeedUriInNamedListFilter groupMemberFilter = new NeedUriInNamedListFilter(ctx, NAME_GROUPMEMBERS);
    //remember the auto-responders in a list
    this.autoResponders = new ArrayList<BaseEventListener>();
    //remember the listeners that wait for all messages
    this.messageCounters = new ArrayList<BaseEventListener>();
    //make a composite filter, with one filter for each autoResponder that wait for the FinishedEvents the responders emit.
    //that filter will be used to shut down all needs after all the autoResponders have finished.
    final OrFilter mainAutoResponderFilter = new OrFilter();
    //listen to NeedCreatedEvents
    this.autoResponderCreator = new DelegateOnEventListener(ctx, groupMemberFilter, new DefaultEventListener(ctx)
    {
      @Override
      protected void doOnEvent(final Event event) throws Exception {
        //create a listener that automatically answers messages, only for that need URI
        AutomaticMessageResponderListener listener = new AutomaticMessageResponderListener(ctx, "autoResponder",
                                                                                           NeedUriEventFilter
                                                                                             .forEvent(event),
                                                                                           NO_OF_MESSAGES,
                                                                                           MILLIS_BETWEEN_MESSAGES);
        //remember the listener for later
        autoResponders.add(listener);
        //add a filter that will wait for the FinishedEvent emitted by that listener
        //wrap it in an acceptonce filter to make extra sure we count each listener only once.
        mainAutoResponderFilter.addFilter(
          new AcceptOnceFilter(
            new FinishedEventFilter(listener)));

        //create a listener that publishes a FinishedEvent after having received all messages from the group
        WaitForNEventsListener waitForMessagesListener = new WaitForNEventsListener(ctx, "messageCounter",
                                                                                    NeedUriEventFilter.forEvent(event),
                                                                                    NO_OF_MESSAGES * (NO_OF_GROUPMEMBERS - 1));
        messageCounters.add(waitForMessagesListener);
        //add a filter that will wait for the FinishedEvent emitted by that listener
        //wrap it in an acceptonce filter to make extra sure we count each listener only once.
        mainAutoResponderFilter.addFilter(
          new AcceptOnceFilter(
            new FinishedEventFilter(waitForMessagesListener)));
        //finally, subscribe to the message events
        getEventBus().subscribe(MessageFromOtherNeedEvent.class, waitForMessagesListener);
        getEventBus().subscribe(MessageFromOtherNeedEvent.class, listener);
      }
    });
    getEventBus().subscribe(NeedCreatedEvent.class, this.autoResponderCreator);


    //count until N needs were created, then create need with group facet (the others will connect to that facet)
    this.groupCreator = new ExecuteOnceAfterNEventsListener(
      ctx, "groupCreator",
      NO_OF_GROUPMEMBERS,
      new CreateNeedWithFacetsAction(ctx, NAME_GROUPS, FacetType.GroupFacet.getURI()));
    bus.subscribe(NeedCreatedEvent.class, this.groupCreator);

    //wait for N+1 needCreatedEvents, then connect the members with the group facet of the third need
    this.needConnector = new ExecuteOnceAfterNEventsListener(ctx, "needConnector", NO_OF_GROUPMEMBERS + 1,
                                                             new ConnectFromListToListAction(ctx, NAME_GROUPMEMBERS,
                                                                                             NAME_GROUPS,
                                                                                             FacetType.OwnerFacet
                                                                                                      .getURI(),
                                                                                             FacetType.GroupFacet
                                                                                                      .getURI(),
                                                                                             MILLIS_BETWEEN_MESSAGES));
    bus.subscribe(NeedCreatedEvent.class, this.needConnector);

    //add a listener that is informed of the connect/open events and that auto-opens
    //subscribe it to:
    // * connect events - so it responds with open
    // * open events - so it responds with open (if the open received was the first open, and we still need to accept the connection)
    this.autoOpener = new AutomaticConnectionOpenerListener(ctx);
    bus.subscribe(ConnectFromOtherNeedEvent.class, this.autoOpener);

    //now, once all connections have been opened, make 1 bot send a message to the group, the subsequent listener will cause let wild chatting to begin
    this.conversationStarter = new DelegateOnceAfterNEventsListener(ctx, "conversationStarter", NO_OF_GROUPMEMBERS,
                                                                    new AutomaticMessageResponderListener(ctx, 1,
                                                                                                          MILLIS_BETWEEN_MESSAGES)
    );
    bus.subscribe(ConnectFromOtherNeedEvent.class, this.conversationStarter);

    //for each group member, there are 2 listeners waiting for messages. when they are all finished, we're done.
    this.messagesDoneListener = new ExecuteOnceAfterNEventsListener(
      ctx, "messagesDoneListener", mainAutoResponderFilter,
      NO_OF_GROUPMEMBERS * 2,
      new DeactivateAllNeedsOfGroupAction(ctx, NAME_GROUPS));
    bus.subscribe(FinishedEvent.class, this.messagesDoneListener);


    //When the group facet need is deactivated, all connections are closed. wait for the close events and signal work done.
    this.workDoneSignaller = new ExecuteOnceAfterNEventsListener(
      ctx, "workDoneSignaller",
      NO_OF_GROUPMEMBERS, new SignalWorkDoneAction(ctx)
    );
    bus.subscribe(CloseFromOtherNeedEvent.class, this.workDoneSignaller);
  }

}

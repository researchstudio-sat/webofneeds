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
import won.bot.framework.events.EventListenerContext;
import won.bot.framework.events.action.BaseEventBotAction;
import won.bot.framework.events.action.impl.*;
import won.bot.framework.events.bus.EventBus;
import won.bot.framework.events.event.Event;
import won.bot.framework.events.event.impl.*;
import won.bot.framework.events.listener.BaseEventListener;
import won.bot.framework.events.listener.EventListener;
import won.bot.framework.events.listener.impl.ActionOnEventListener;
import won.bot.framework.events.listener.impl.ActionOnceAfterNEventsListener;
import won.bot.framework.events.listener.impl.AutomaticMessageResponderListener;
import won.bot.integrationtest.failsim.BaseEventListenerContextDecorator;
import won.bot.integrationtest.failsim.DuplicateMessageSenderDecorator;
import won.protocol.model.FacetType;
import won.protocol.util.WonRdfUtils;

/**
 *
 */
public class DuplicateMessageSendingConversationBot extends IntegrationtestBot
{

  private static final int NO_OF_NEEDS = 2;
  private static final int NO_OF_MESSAGES = 10;
  private static final long MILLIS_BETWEEN_MESSAGES = 100;
    private static final String NAME_NEEDS = "needs";


  @Override
  protected void initializeEventListeners()
  {
    EventListenerContext ctx = getDuplicateMessageSenderDecorator(getEventListenerContext());
    final EventBus bus = getEventBus();

    //we're not expecting any failure messages in this test:
    bus.subscribe(
            FailureResponseEvent.class,
            new ActionOnEventListener(
                    ctx,
                    new BaseEventBotAction(ctx){
                        @Override
                        protected void doRun(Event event) throws Exception {
                          FailureResponseEvent failureResponseEvent = (FailureResponseEvent)event;
                          bus.publish(new TestFailedEvent(
                                  DuplicateMessageSendingConversationBot.this,
                                  "Message failed: "  + failureResponseEvent.getOriginalMessageURI()
                                          + ": " + WonRdfUtils.MessageUtils.getTextMessage(failureResponseEvent.getFailureMessage())));
                        }
                      }));

    //create needs every trigger execution until 2 needs are created

    bus.subscribe(ActEvent.class,new ActionOnEventListener(
            ctx,
            new CreateNeedWithFacetsAction(ctx,NAME_NEEDS),
            NO_OF_NEEDS
        ));

    //connect needs
    bus.subscribe(NeedCreatedEvent.class, new ActionOnceAfterNEventsListener(ctx,"needConnector",
            NO_OF_NEEDS * 2, new ConnectFromListToListAction(ctx,NAME_NEEDS,NAME_NEEDS,FacetType.OwnerFacet.getURI(),FacetType.OwnerFacet.getURI(), MILLIS_BETWEEN_MESSAGES)));

    //add a listener that is informed of the connect/open events and that auto-opens
    //subscribe it to:
    // * connect events - so it responds with open
    // * open events - so it responds with open (if the open received was the first open, and we still need to accept the connection)
    bus.subscribe(ConnectFromOtherNeedEvent.class, new ActionOnEventListener(ctx, new OpenConnectionAction(ctx)));

    //add a listener that auto-responds to messages by a message
    //after 10 messages, it unsubscribes from all events
    //subscribe it to:
    // * message events - so it responds
    // * open events - so it initiates the chain reaction of responses
    BaseEventListener autoResponder = new AutomaticMessageResponderListener(ctx, NO_OF_MESSAGES, MILLIS_BETWEEN_MESSAGES);
    bus.subscribe(OpenFromOtherNeedEvent.class, autoResponder);
    bus.subscribe(MessageFromOtherNeedEvent.class, autoResponder);

    //add a listener that closes the connection after it has seen 10 messages
    bus.subscribe( MessageFromOtherNeedEvent.class, new ActionOnceAfterNEventsListener(
            ctx,
            NO_OF_MESSAGES, new CloseConnectionAction(ctx)
      ));
    //add a listener that closes the connection when a failureEvent occurs
    EventListener onFailureConnectionCloser = new ActionOnEventListener(ctx, new CloseConnectionAction(ctx));
    bus.subscribe(FailureResponseEvent.class, onFailureConnectionCloser);

    //add a listener that auto-responds to a close message with a deactivation of both needs.
    //subscribe it to:
    // * close events

    bus.subscribe(CloseFromOtherNeedEvent.class,
            new ActionOnEventListener(ctx,
                    new MultipleActions(ctx,
                      new DeactivateAllNeedsAction(ctx),
                      new PublishEventAction(ctx, new TestPassedEvent(this)))
                            ,1));

    //add a listener that counts two NeedDeactivatedEvents and then tells the
    //framework that the bot's work is done
    bus.subscribe(NeedDeactivatedEvent.class, new ActionOnceAfterNEventsListener(
            ctx,
            NO_OF_NEEDS, new SignalWorkDoneAction(ctx)
          ));
  }

  protected BaseEventListenerContextDecorator getDuplicateMessageSenderDecorator(EventListenerContext eventListenerContext) {
    return new DuplicateMessageSenderDecorator(eventListenerContext);
  }


}

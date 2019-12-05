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
package won.bot.integrationtest.security;

import won.bot.IntegrationtestBot;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.action.impl.MultipleActions;
import won.bot.framework.eventbot.action.impl.PublishEventAction;
import won.bot.framework.eventbot.action.impl.atomlifecycle.CreateAtomWithSocketsAction;
import won.bot.framework.eventbot.action.impl.atomlifecycle.DeactivateAllAtomsAction;
import won.bot.framework.eventbot.action.impl.lifecycle.SignalWorkDoneAction;
import won.bot.framework.eventbot.action.impl.wonmessage.CloseConnectionAction;
import won.bot.framework.eventbot.action.impl.wonmessage.ConnectFromListToListAction;
import won.bot.framework.eventbot.action.impl.wonmessage.OpenConnectionAction;
import won.bot.framework.eventbot.bus.EventBus;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.atomlifecycle.AtomCreatedEvent;
import won.bot.framework.eventbot.event.impl.atomlifecycle.AtomDeactivatedEvent;
import won.bot.framework.eventbot.event.impl.lifecycle.ActEvent;
import won.bot.framework.eventbot.event.impl.test.TestFailedEvent;
import won.bot.framework.eventbot.event.impl.test.TestPassedEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.CloseFromOtherAtomEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.ConnectFromOtherAtomEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.FailureResponseEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.MessageFromOtherAtomEvent;
import won.bot.framework.eventbot.listener.BaseEventListener;
import won.bot.framework.eventbot.listener.EventListener;
import won.bot.framework.eventbot.listener.impl.ActionOnEventListener;
import won.bot.framework.eventbot.listener.impl.ActionOnceAfterNEventsListener;
import won.bot.framework.eventbot.listener.impl.AutomaticMessageResponderListener;
import won.bot.integrationtest.failsim.BaseEventListenerContextDecorator;
import won.bot.integrationtest.failsim.DuplicateMessageSenderDecorator;
import won.protocol.model.SocketType;
import won.protocol.util.WonRdfUtils;

/**
 *
 */
public class DuplicateMessageSendingConversationBot extends IntegrationtestBot {
    private static final int NO_OF_ATOMS = 2;
    private static final int NO_OF_MESSAGES = 10;
    private static final long MILLIS_BETWEEN_MESSAGES = 100;

    @Override
    protected void initializeEventListeners() {
        EventListenerContext ctx = getDuplicateMessageSenderDecorator(getEventListenerContext());
        final EventBus bus = getEventBus();
        // we're not expecting any failure messages in this test:
        bus.subscribe(FailureResponseEvent.class, new ActionOnEventListener(ctx, new BaseEventBotAction(ctx) {
            @Override
            protected void doRun(Event event, EventListener executingListener) throws Exception {
                FailureResponseEvent failureResponseEvent = (FailureResponseEvent) event;
                bus.publish(new TestFailedEvent(DuplicateMessageSendingConversationBot.this, "Message failed: "
                                + failureResponseEvent.getOriginalMessageURI() + ": "
                                + WonRdfUtils.MessageUtils.getTextMessage(failureResponseEvent.getFailureMessage())));
            }
        }));
        // create atoms every trigger execution until 2 atoms are created
        bus.subscribe(ActEvent.class, new ActionOnEventListener(ctx,
                        new CreateAtomWithSocketsAction(ctx, getBotContextWrapper().getAtomCreateListName()),
                        NO_OF_ATOMS));
        // connect atoms
        bus.subscribe(AtomCreatedEvent.class, new ActionOnceAfterNEventsListener(ctx, "atomConnector", NO_OF_ATOMS * 2,
                        new ConnectFromListToListAction(ctx, getBotContextWrapper().getAtomCreateListName(),
                                        getBotContextWrapper().getAtomCreateListName(), SocketType.ChatSocket.getURI(),
                                        SocketType.ChatSocket.getURI(), MILLIS_BETWEEN_MESSAGES, "Hi!")));
        // add a listener that is informed of the connect/open events and that
        // auto-opens
        // subscribe it to:
        // * connect events - so it responds with open
        // * open events - so it responds with open (if the open received was the first
        // open, and we still need to accept the connection)
        bus.subscribe(ConnectFromOtherAtomEvent.class,
                        new ActionOnEventListener(ctx, new OpenConnectionAction(ctx, "Hi!")));
        // add a listener that auto-responds to messages by a message
        // after 10 messages, it unsubscribes from all events
        // subscribe it to:
        // * message events - so it responds
        // * open events - so it initiates the chain reaction of responses
        BaseEventListener autoResponder = new AutomaticMessageResponderListener(ctx, NO_OF_MESSAGES,
                        MILLIS_BETWEEN_MESSAGES);
        bus.subscribe(ConnectFromOtherAtomEvent.class, autoResponder);
        bus.subscribe(MessageFromOtherAtomEvent.class, autoResponder);
        // add a listener that closes the connection after it has seen 10 messages
        bus.subscribe(MessageFromOtherAtomEvent.class, new ActionOnceAfterNEventsListener(ctx, NO_OF_MESSAGES,
                        new CloseConnectionAction(ctx, "Bye!")));
        // add a listener that closes the connection when a failureEvent occurs
        EventListener onFailureConnectionCloser = new ActionOnEventListener(ctx,
                        new CloseConnectionAction(ctx, "Bye!"));
        bus.subscribe(FailureResponseEvent.class, onFailureConnectionCloser);
        // add a listener that auto-responds to a close message with a deactivation of
        // both atoms.
        // subscribe it to:
        // * close events
        bus.subscribe(CloseFromOtherAtomEvent.class, new ActionOnEventListener(ctx, new MultipleActions(ctx,
                        new DeactivateAllAtomsAction(ctx), new PublishEventAction(ctx, new TestPassedEvent(this))), 1));
        // add a listener that counts two AtomDeactivatedEvents and then tells the
        // framework that the bot's work is done
        bus.subscribe(AtomDeactivatedEvent.class,
                        new ActionOnceAfterNEventsListener(ctx, NO_OF_ATOMS, new SignalWorkDoneAction(ctx)));
    }

    protected BaseEventListenerContextDecorator getDuplicateMessageSenderDecorator(
                    EventListenerContext eventListenerContext) {
        return new DuplicateMessageSenderDecorator(eventListenerContext);
    }
}

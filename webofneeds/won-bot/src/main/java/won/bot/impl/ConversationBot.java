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
import won.bot.framework.eventbot.action.impl.lifecycle.SignalWorkDoneAction;
import won.bot.framework.eventbot.action.impl.atomlifecycle.CreateAtomWithSocketsAction;
import won.bot.framework.eventbot.action.impl.atomlifecycle.DeactivateAllAtomsAction;
import won.bot.framework.eventbot.action.impl.wonmessage.CloseConnectionAction;
import won.bot.framework.eventbot.action.impl.wonmessage.ConnectFromListToListAction;
import won.bot.framework.eventbot.action.impl.wonmessage.OpenConnectionAction;
import won.bot.framework.eventbot.bus.EventBus;
import won.bot.framework.eventbot.event.impl.lifecycle.ActEvent;
import won.bot.framework.eventbot.event.impl.atomlifecycle.AtomCreatedEvent;
import won.bot.framework.eventbot.event.impl.atomlifecycle.AtomDeactivatedEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.CloseFromOtherAtomEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.ConnectFromOtherAtomEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.FailureResponseEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.MessageFromOtherAtomEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.OpenFromOtherAtomEvent;
import won.bot.framework.eventbot.listener.BaseEventListener;
import won.bot.framework.eventbot.listener.EventListener;
import won.bot.framework.eventbot.listener.impl.ActionOnEventListener;
import won.bot.framework.eventbot.listener.impl.ActionOnceAfterNEventsListener;
import won.bot.framework.eventbot.listener.impl.AutomaticMessageResponderListener;
import won.protocol.model.SocketType;

/**
 *
 */
public class ConversationBot extends EventBot {
    private static final int NO_OF_ATOMS = 2;
    private static final int NO_OF_MESSAGES = 10;
    private static final long MILLIS_BETWEEN_MESSAGES = 100;
    // we use protected members so we can extend the class and
    // access the listeners for unit test assertions and stats
    //
    // we use BaseEventListener as their types so we can access the generic
    // functionality offered by that class
    protected BaseEventListener atomCreator;
    protected BaseEventListener atomConnector;
    protected BaseEventListener autoOpener;
    protected BaseEventListener autoResponder;
    protected BaseEventListener connectionCloser;
    protected BaseEventListener atomDeactivator;
    protected BaseEventListener workDoneSignaller;

    @Override
    protected void initializeEventListeners() {
        EventListenerContext ctx = getEventListenerContext();
        EventBus bus = getEventBus();
        // create atoms every trigger execution until 2 atoms are created
        this.atomCreator = new ActionOnEventListener(ctx,
                        new CreateAtomWithSocketsAction(ctx, getBotContextWrapper().getAtomCreateListName()),
                        NO_OF_ATOMS);
        bus.subscribe(ActEvent.class, this.atomCreator);
        // count until 2 atoms were created, then
        // * connect the 2 atoms
        this.atomConnector = new ActionOnceAfterNEventsListener(ctx, "atomConnector", NO_OF_ATOMS,
                        new ConnectFromListToListAction(ctx, ctx.getBotContextWrapper().getAtomCreateListName(),
                                        ctx.getBotContextWrapper().getAtomCreateListName(),
                                        SocketType.ChatSocket.getURI(), SocketType.ChatSocket.getURI(),
                                        MILLIS_BETWEEN_MESSAGES, "Hi, I am the ConversationBot."));
        bus.subscribe(AtomCreatedEvent.class, this.atomConnector);
        // add a listener that is informed of the connect/open events and that
        // auto-opens
        // subscribe it to:
        // * connect events - so it responds with open
        // * open events - so it responds with open (if the open received was the first
        // open, and we still atom to accept the connection)
        this.autoOpener = new ActionOnEventListener(ctx,
                        new OpenConnectionAction(ctx, "Hi, I " + "am the ConversationBot, too!"));
        bus.subscribe(ConnectFromOtherAtomEvent.class, this.autoOpener);
        // add a listener that auto-responds to messages by a message
        // after 10 messages, it unsubscribes from all events
        // subscribe it to:
        // * message events - so it responds
        // * open events - so it initiates the chain reaction of responses
        this.autoResponder = new AutomaticMessageResponderListener(ctx, NO_OF_MESSAGES, MILLIS_BETWEEN_MESSAGES);
        bus.subscribe(OpenFromOtherAtomEvent.class, this.autoResponder);
        bus.subscribe(MessageFromOtherAtomEvent.class, this.autoResponder);
        // add a listener that closes the connection after it has seen 10 messages
        this.connectionCloser = new ActionOnceAfterNEventsListener(ctx, NO_OF_MESSAGES,
                        new CloseConnectionAction(ctx, "Bye!"));
        bus.subscribe(MessageFromOtherAtomEvent.class, this.connectionCloser);
        // add a listener that closes the connection when a failureEvent occurs
        EventListener onFailureConnectionCloser = new ActionOnEventListener(ctx,
                        new CloseConnectionAction(ctx, "Bye!"));
        bus.subscribe(FailureResponseEvent.class, onFailureConnectionCloser);
        // add a listener that auto-responds to a close message with a deactivation of
        // both atoms.
        // subscribe it to:
        // * close events
        this.atomDeactivator = new ActionOnEventListener(ctx, new DeactivateAllAtomsAction(ctx), 1);
        bus.subscribe(CloseFromOtherAtomEvent.class, this.atomDeactivator);
        // add a listener that counts two AtomDeactivatedEvents and then tells the
        // framework that the bot's work is done
        this.workDoneSignaller = new ActionOnceAfterNEventsListener(ctx, NO_OF_ATOMS, new SignalWorkDoneAction(ctx));
        bus.subscribe(AtomDeactivatedEvent.class, this.workDoneSignaller);
    }
}

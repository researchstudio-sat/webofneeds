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
import won.bot.framework.bot.context.CommentBotContextWrapper;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.action.impl.lifecycle.SignalWorkDoneAction;
import won.bot.framework.eventbot.action.impl.atomlifecycle.CreateAtomWithSocketsAction;
import won.bot.framework.eventbot.action.impl.atomlifecycle.DeactivateAllAtomsAction;
import won.bot.framework.eventbot.action.impl.wonmessage.ConnectFromListToListAction;
import won.bot.framework.eventbot.action.impl.wonmessage.OpenConnectionAction;
import won.bot.framework.eventbot.bus.EventBus;
import won.bot.framework.eventbot.event.BaseEvent;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.lifecycle.ActEvent;
import won.bot.framework.eventbot.event.impl.atomlifecycle.AtomCreatedEvent;
import won.bot.framework.eventbot.event.impl.atomlifecycle.AtomDeactivatedEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.ConnectFromOtherAtomEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.OpenFromOtherAtomEvent;
import won.bot.framework.eventbot.listener.BaseEventListener;
import won.bot.framework.eventbot.listener.EventListener;
import won.bot.framework.eventbot.listener.impl.ActionOnEventListener;
import won.bot.framework.eventbot.listener.impl.ActionOnceAfterNEventsListener;
import won.protocol.model.SocketType;

/**
 *
 */
public class CommentBot extends EventBot {
    private static final int NO_OF_ATOMS = 1;
    private static final long MILLIS_BETWEEN_MESSAGES = 10;
    // we use protected members so we can extend the class and
    // access the listeners for unit test assertions and stats
    //
    // we use BaseEventListener as their types so we can access the generic
    // functionality offered by that class
    protected BaseEventListener atomCreator;
    protected BaseEventListener commentSocketCreator;
    protected BaseEventListener atomConnector;
    protected BaseEventListener autoOpener;
    protected BaseEventListener autoResponder;
    protected BaseEventListener connectionCloser;
    protected BaseEventListener allAtomsDeactivator;
    protected BaseEventListener atomDeactivator;
    protected BaseEventListener workDoneSignaller;

    @Override
    protected void initializeEventListeners() {
        EventListenerContext ctx = getEventListenerContext();
        final EventBus bus = getEventBus();
        CommentBotContextWrapper botContextWrapper = (CommentBotContextWrapper) getBotContextWrapper();
        // create atoms every trigger execution until 2 atoms are created
        this.atomCreator = new ActionOnEventListener(ctx,
                        new CreateAtomWithSocketsAction(ctx, botContextWrapper.getAtomCreateListName()), NO_OF_ATOMS);
        bus.subscribe(ActEvent.class, this.atomCreator);
        // count until 1 atom is created, then create a comment socket
        this.commentSocketCreator = new ActionOnEventListener(ctx, new CreateAtomWithSocketsAction(ctx,
                        botContextWrapper.getCommentListName(), SocketType.CommentSocket.getURI()), 1);
        bus.subscribe(AtomCreatedEvent.class, this.commentSocketCreator);
        this.atomConnector = new ActionOnceAfterNEventsListener(ctx, 2,
                        new ConnectFromListToListAction(ctx, botContextWrapper.getAtomCreateListName(),
                                        botContextWrapper.getCommentListName(), SocketType.ChatSocket.getURI(),
                                        SocketType.CommentSocket.getURI(), MILLIS_BETWEEN_MESSAGES,
                                        "Hi, I am the " + "CommentBot."));
        bus.subscribe(AtomCreatedEvent.class, this.atomConnector);
        this.autoOpener = new ActionOnEventListener(ctx, new OpenConnectionAction(ctx, "Hi!"));
        bus.subscribe(OpenFromOtherAtomEvent.class, this.autoOpener);
        bus.subscribe(ConnectFromOtherAtomEvent.class, this.autoOpener);
        BaseEventListener assertionRunner = new ActionOnceAfterNEventsListener(ctx, 1, new BaseEventBotAction(ctx) {
            @Override
            protected void doRun(final Event event, EventListener executingListener) throws Exception {
                executeAssertionsForEstablishedConnectionInternal(bus);
            }
        });
        bus.subscribe(OpenFromOtherAtomEvent.class, assertionRunner);
        // deactivate all atoms when the assertion was executed
        this.allAtomsDeactivator = new ActionOnEventListener(ctx, new DeactivateAllAtomsAction(ctx), 1);
        bus.subscribe(AssertionsExecutedEvent.class, this.allAtomsDeactivator);
        // add a listener that counts two AtomDeactivatedEvents and then tells the
        // framework that the bot's work is done
        this.workDoneSignaller = new ActionOnceAfterNEventsListener(ctx, 2, new SignalWorkDoneAction(ctx));
        bus.subscribe(AtomDeactivatedEvent.class, this.workDoneSignaller);
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

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
import won.bot.framework.eventbot.action.EventBotAction;
import won.bot.framework.eventbot.action.impl.MultipleActions;
import won.bot.framework.eventbot.action.impl.RandomDelayedAction;
import won.bot.framework.eventbot.action.impl.debugbot.*;
import won.bot.framework.eventbot.action.impl.matcher.RegisterMatcherAction;
import won.bot.framework.eventbot.action.impl.wonmessage.ConnectWithAssociatedAtomAction;
import won.bot.framework.eventbot.action.impl.wonmessage.HintAssociatedAtomAction;
import won.bot.framework.eventbot.action.impl.wonmessage.SendMultipleMessagesAction;
import won.bot.framework.eventbot.behaviour.BotBehaviour;
import won.bot.framework.eventbot.behaviour.EagerlyPopulateCacheBehaviour;
import won.bot.framework.eventbot.behaviour.ExecuteWonMessageCommandBehaviour;
import won.bot.framework.eventbot.bus.EventBus;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.command.close.CloseCommandSuccessEvent;
import won.bot.framework.eventbot.event.impl.debugbot.*;
import won.bot.framework.eventbot.event.impl.lifecycle.ActEvent;
import won.bot.framework.eventbot.event.impl.matcher.AtomCreatedEventForMatcher;
import won.bot.framework.eventbot.event.impl.matcher.MatcherRegisterFailedEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.*;
import won.bot.framework.eventbot.filter.impl.AtomUriInNamedListFilter;
import won.bot.framework.eventbot.filter.impl.NotFilter;
import won.bot.framework.eventbot.listener.BaseEventListener;
import won.bot.framework.eventbot.listener.EventListener;
import won.bot.framework.eventbot.listener.impl.ActionOnEventListener;
import won.protocol.model.SocketType;

import java.net.URI;

/**
 * Bot that reacts to each new atom that is created in the system by creating
 * two atoms, it sends a connect message from one of these atoms, and a hint
 * message for original atom offering match to another of these atoms.
 * Additionally, it reacts to certain commands send via text messages on the
 * connections with the created by the bot atoms.
 */
public class DebugBot extends EventBot {
    private static final long CONNECT_DELAY_MILLIS = 0;
    private static final long DELAY_BETWEEN_N_MESSAGES = 1000;
    private static final double CHATTY_MESSAGE_PROBABILITY = 0.1;
    private BaseEventListener matcherRegistrator;
    protected BaseEventListener atomCreator;
    protected BaseEventListener atomConnector;
    protected BaseEventListener atomHinter;
    protected BaseEventListener autoOpener;
    protected BaseEventListener atomCloser;
    protected BaseEventListener messageFromOtherAtomListener;
    protected BaseEventListener usageMessageSender;
    private int registrationMatcherRetryInterval = 30000;

    public void setRegistrationMatcherRetryInterval(final int registrationMatcherRetryInterval) {
        this.registrationMatcherRetryInterval = registrationMatcherRetryInterval;
    }

    private URI matcherUri;

    public void setMatcherUri(final URI matcherUri) {
        this.matcherUri = matcherUri;
    }

    @Override
    protected void initializeEventListeners() {
        String welcomeMessage = "Greetings! I am the DebugBot. I "
                        + "can simulate multiple other users so you can test things. I understand a few commands. To see which ones, "
                        + "type 'usage'.";
        String welcomeHelpMessage = "When connecting with me, you can say 'ignore', or 'deny' to make me ignore or deny requests, and 'wait N' to make me wait N seconds (max 99) before reacting.";
        EventListenerContext ctx = getEventListenerContext();
        EventBus bus = getEventBus();
        // eagerly cache RDF data
        BotBehaviour eagerlyCacheBehaviour = new EagerlyPopulateCacheBehaviour(ctx);
        eagerlyCacheBehaviour.activate();
        // react to a bot command activating/deactivating eager caching
        bus.subscribe(SetCacheEagernessCommandEvent.class, new ActionOnEventListener(ctx, new BaseEventBotAction(ctx) {
            @Override
            protected void doRun(Event event, EventListener executingListener) throws Exception {
                if (event instanceof SetCacheEagernessCommandEvent) {
                    if (((SetCacheEagernessCommandEvent) event).isEager()) {
                        eagerlyCacheBehaviour.activate();
                    } else {
                        eagerlyCacheBehaviour.deactivate();
                    }
                }
            }
        }));
        // register listeners for event.impl.command events used to tell the bot to send
        // messages
        ExecuteWonMessageCommandBehaviour wonMessageCommandBehaviour = new ExecuteWonMessageCommandBehaviour(ctx);
        wonMessageCommandBehaviour.activate();
        // register with WoN nodes, be notified when new atoms are created
        RegisterMatcherAction registerMatcherAction = new RegisterMatcherAction(ctx);
        this.matcherRegistrator = new ActionOnEventListener(ctx, registerMatcherAction, 1);
        bus.subscribe(ActEvent.class, this.matcherRegistrator);
        RandomDelayedAction delayedRegistration = new RandomDelayedAction(ctx, registrationMatcherRetryInterval,
                        registrationMatcherRetryInterval, 0, registerMatcherAction);
        ActionOnEventListener matcherRetryRegistrator = new ActionOnEventListener(ctx, delayedRegistration);
        bus.subscribe(MatcherRegisterFailedEvent.class, matcherRetryRegistrator);
        // create the echo atom for debug initial connect - if we're not reacting to the
        // creation of our own echo atom.
        CreateDebugAtomWithSocketsAction atomForInitialConnectAction = new CreateDebugAtomWithSocketsAction(ctx, true,
                        true, SocketType.ChatSocket.getURI(), SocketType.HoldableSocket.getURI(),
                        SocketType.BuddySocket.getURI());
        atomForInitialConnectAction.setIsInitialForConnect(true);
        ActionOnEventListener initialConnector = new ActionOnEventListener(ctx,
                        new NotFilter(new AtomUriInNamedListFilter(ctx,
                                        ctx.getBotContextWrapper().getAtomCreateListName())),
                        atomForInitialConnectAction);
        bus.subscribe(AtomCreatedEventForMatcher.class, initialConnector);
        // create the echo atom for debug initial hint - if we're not reacting to the
        // creation of our own echo atom.
        CreateDebugAtomWithSocketsAction initialHinter = new CreateDebugAtomWithSocketsAction(ctx, true, true,
                        SocketType.ChatSocket.getURI(), SocketType.HoldableSocket.getURI(),
                        SocketType.BuddySocket.getURI());
        initialHinter.setIsInitialForHint(true);
        ActionOnEventListener atomForInitialHintListener = new ActionOnEventListener(ctx, new NotFilter(
                        new AtomUriInNamedListFilter(ctx, ctx.getBotContextWrapper().getAtomCreateListName())),
                        initialHinter);
        bus.subscribe(AtomCreatedEventForMatcher.class, atomForInitialHintListener);
        // as soon as the echo atom triggered by debug connect created, connect to
        // original
        this.atomConnector = new ActionOnEventListener(ctx, "atomConnector",
                        new RandomDelayedAction(ctx, CONNECT_DELAY_MILLIS, CONNECT_DELAY_MILLIS, 1,
                                        new ConnectWithAssociatedAtomAction(ctx, SocketType.ChatSocket.getURI(),
                                                        SocketType.ChatSocket.getURI(),
                                                        welcomeMessage + " " + welcomeHelpMessage)));
        bus.subscribe(AtomCreatedEventForDebugConnect.class, this.atomConnector);
        // as soon as the echo atom triggered by debug hint command created, hint to
        // original
        this.atomHinter = new ActionOnEventListener(ctx, "atomHinter", new RandomDelayedAction(ctx,
                        CONNECT_DELAY_MILLIS, CONNECT_DELAY_MILLIS, 1, new HintAssociatedAtomAction(ctx,
                                        SocketType.ChatSocket.getURI(), SocketType.ChatSocket.getURI(),
                                        matcherUri)));
        bus.subscribe(AtomCreatedEventForDebugHint.class, this.atomHinter);
        // if the original atom wants to connect - always open
        this.autoOpener = new ActionOnEventListener(ctx,
                        new MultipleActions(ctx, new OpenConnectionDebugAction(ctx, welcomeMessage, welcomeHelpMessage),
                                        new PublishSetChattinessEventAction(ctx, true)));
        bus.subscribe(ConnectFromOtherAtomEvent.class, this.autoOpener);
        EventBotAction userCommandAction = new DebugBotIncomingMessageToEventMappingAction(ctx);
        // if the remote side opens, send a greeting and set to chatty.
        bus.subscribe(OpenFromOtherAtomEvent.class, new ActionOnEventListener(ctx,
                        new MultipleActions(ctx, userCommandAction, new PublishSetChattinessEventAction(ctx, true))));
        // if the bot receives a text message - try to map the command of the text
        // message to a DebugEvent
        messageFromOtherAtomListener = new ActionOnEventListener(ctx, userCommandAction);
        bus.subscribe(MessageFromOtherAtomEvent.class, messageFromOtherAtomListener);
        // react to usage command event
        this.usageMessageSender = new ActionOnEventListener(ctx, new SendMultipleMessagesAction(ctx,
                        DebugBotIncomingMessageToEventMappingAction.USAGE_MESSAGES));
        bus.subscribe(UsageDebugCommandEvent.class, usageMessageSender);
        bus.subscribe(CloseCommandSuccessEvent.class, new ActionOnEventListener(ctx, "chattiness off",
                        new PublishSetChattinessEventAction(ctx, false)));
        // react to close event: set connection to not chatty
        bus.subscribe(CloseFromOtherAtomEvent.class,
                        new ActionOnEventListener(ctx, new PublishSetChattinessEventAction(ctx, false)));
        // react to the hint and connect commands by creating an atom (it will fire
        // correct atom created for connect/hint
        // events)
        atomCreator = new ActionOnEventListener(ctx, new CreateDebugAtomWithSocketsAction(ctx, true, true));
        bus.subscribe(HintDebugCommandEvent.class, atomCreator);
        bus.subscribe(ConnectDebugCommandEvent.class, atomCreator);
        bus.subscribe(SendNDebugCommandEvent.class, new ActionOnEventListener(ctx, new SendNDebugMessagesAction(ctx,
                        DELAY_BETWEEN_N_MESSAGES, DebugBotIncomingMessageToEventMappingAction.N_MESSAGES)));
        MessageTimingManager timingManager = new MessageTimingManager(ctx);
        // on every actEvent there is a chance we send a chatty message
        bus.subscribe(ActEvent.class,
                        new ActionOnEventListener(ctx,
                                        new SendChattyMessageAction(ctx, CHATTY_MESSAGE_PROBABILITY, timingManager,
                                                        DebugBotIncomingMessageToEventMappingAction.RANDOM_MESSAGES,
                                                        DebugBotIncomingMessageToEventMappingAction.LAST_MESSAGES)));
        // set the chattiness of the connection
        bus.subscribe(SetChattinessDebugCommandEvent.class,
                        new ActionOnEventListener(ctx, new SetChattinessAction(ctx)));
        // process eliza messages with eliza
        bus.subscribe(MessageToElizaEvent.class, new ActionOnEventListener(ctx, new AnswerWithElizaAction(ctx, 20)));
        // remember when we sent the last message
        bus.subscribe(WonMessageSentOnConnectionEvent.class,
                        new ActionOnEventListener(ctx, new RecordMessageSentTimeAction(ctx, timingManager)));
        // remember when we got the last message
        bus.subscribe(WonMessageReceivedOnConnectionEvent.class,
                        new ActionOnEventListener(ctx, new RecordMessageReceivedTimeAction(ctx, timingManager)));
        // initialize the sent timestamp when the open message is received
        bus.subscribe(OpenFromOtherAtomEvent.class,
                        new ActionOnEventListener(ctx, new RecordMessageSentTimeAction(ctx, timingManager)));
        // initialize the sent timestamp when the connect message is received
        bus.subscribe(ConnectFromOtherAtomEvent.class,
                        new ActionOnEventListener(ctx, new RecordMessageSentTimeAction(ctx, timingManager)));
        bus.subscribe(ReplaceDebugAtomContentCommandEvent.class,
                        new ActionOnEventListener(ctx, new ReplaceDebugAtomContentAction(ctx)));
    }
}

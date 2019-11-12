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
package won.bot.framework.bot.base;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executor;

import org.apache.jena.query.Dataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;

import won.bot.framework.bot.BotLifecyclePhase;
import won.bot.framework.bot.context.BotContext;
import won.bot.framework.bot.context.BotContextWrapper;
import won.bot.framework.component.atomproducer.AtomProducer;
import won.bot.framework.component.nodeurisource.NodeURISource;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.bus.EventBus;
import won.bot.framework.eventbot.bus.impl.AsyncEventBusImpl;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.atomlifecycle.AtomCreatedEvent;
import won.bot.framework.eventbot.event.impl.lifecycle.ActEvent;
import won.bot.framework.eventbot.event.impl.lifecycle.InitializeEvent;
import won.bot.framework.eventbot.event.impl.lifecycle.ShutdownEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.AtomHintFromMatcherEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.CloseFromOtherAtomEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.ConnectFromOtherAtomEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.FailureResponseEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.MessageFromOtherAtomEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.SocketHintFromMatcherEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.SuccessResponseEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.WonMessageSentEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.WonMessageSentOnConnectionEvent;
import won.bot.framework.eventbot.filter.impl.AtomUriInNamedListFilter;
import won.bot.framework.eventbot.filter.impl.NotFilter;
import won.matcher.component.MatcherNodeURISource;
import won.matcher.protocol.impl.MatcherProtocolMatcherServiceImplJMSBased;
import won.protocol.matcher.MatcherProtocolAtomServiceClientSide;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageType;
import won.protocol.message.sender.WonMessageSender;
import won.protocol.message.sender.exception.WonMessageSenderException;
import won.protocol.model.Connection;
import won.protocol.model.SocketType;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.linkeddata.LinkedDataSource;
import won.protocol.util.linkeddata.WonLinkedDataUtils;

/**
 * Base class for bots that define their behaviour through event listeners. Once
 * the bot's work is done, the workIsDone() method should be called to allow the
 * framework to shut down the bot gracefully. All methods from the Bot interface
 * are converted to Events by the EventBot. Subclasses should implement the
 * initializeEventListeners() method to register their event listeners. The
 * corresponding shutdownEventListeners() is called when the bot's shutdown
 * method is called an may be used to perform shutdown work for listeners. An
 * event bot implementation should take care not to do long or blocking work in
 * event listeners. Whenever a long computation or a blocking call is done, it
 * should be done in a Runnable, executed in the Executor (see below). The bot's
 * services and other environment can be accessed in listeners through the
 * instance of EventListenerContext. these services include:
 * <ul>
 * <li>the event bus - for subscribing for/unsubscribing for/publishing
 * event</li>
 * <li>the task scheduler - for scheduling tasks (Runnables) to be run after a
 * timeout</li>
 * <li>the executor - for running tasks (Runnables) immediately in the task
 * scheduler's thread pool</li>
 * <li>the node uri source - for obtaining a won node URI for atom creation</li>
 * <li>the atom producer - for obtaining a valid atom content in RDF that can be
 * used to create an atom</li>
 * <li>the owner service - for sending messages to won nodes (create atoms,
 * connect, open, message, close)</li>
 * <li>shutting down the trigger - an event bot may have a trigger that calls
 * the bot's act() method regularly.</li>
 * </ul>
 * The bot will only react to onXX methods (i.e., create events and publish them
 * on the internal event bus) if it is in lifecycle phase ACTIVE.
 */
public abstract class EventBot extends ScheduledTriggerBot {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private EventBus eventBus;
    private EventListenerContext eventListenerContext = new MyEventListenerContext();
    private EventGeneratingWonMessageSenderWrapper wonMessageSenderWrapper;

    @Override
    public final void act() throws Exception {
        if (getLifecyclePhase().isActive()) {
            eventBus.publish(new ActEvent());
        } else {
            logger.info("not publishing event for call to act() as the bot is not in state {} but {}",
                            BotLifecyclePhase.ACTIVE, getLifecyclePhase());
        }
    }

    private void logMessage(WonMessage wonMessage) {
        if (logger.isDebugEnabled()) {
            logger.debug("Received message: " + wonMessage.toStringForDebug(true));
        }
    }

    @Override
    public final void onMessageFromOtherAtom(final Connection con, final WonMessage wonMessage) {
        logMessage(wonMessage);
        if (getLifecyclePhase().isActive()) {
            eventBus.publish(new MessageFromOtherAtomEvent(con, wonMessage));
        } else {
            logger.info(
                            "not publishing event for call to onMessageFromOtherAtom() as the bot is not in state {} but {}",
                            BotLifecyclePhase.ACTIVE, getLifecyclePhase());
        }
    }

    @Override
    public final void onAtomHintFromMatcher(final WonMessage wonMessage) {
        logMessage(wonMessage);
        if (getLifecyclePhase().isActive()) {
            eventBus.publish(new AtomHintFromMatcherEvent(wonMessage));
        } else {
            logger.info("not publishing event for call to onAtomHintFromMatcher() as the bot is not in state {} but {}",
                            BotLifecyclePhase.ACTIVE, getLifecyclePhase());
        }
    }

    @Override
    public final void onSocketHintFromMatcher(final WonMessage wonMessage) {
        logMessage(wonMessage);
        if (getLifecyclePhase().isActive()) {
            eventBus.publish(new SocketHintFromMatcherEvent(wonMessage));
        } else {
            logger.info(
                            "not publishing event for call to onSocketHintFromMatcher() as the bot is not in state {} but {}",
                            BotLifecyclePhase.ACTIVE, getLifecyclePhase());
        }
    }

    @Override
    public final void onCloseFromOtherAtom(final Connection con, final WonMessage wonMessage) {
        logMessage(wonMessage);
        if (getLifecyclePhase().isActive()) {
            eventBus.publish(new CloseFromOtherAtomEvent(con, wonMessage));
        } else {
            logger.info("not publishing event for call to onClose() as the bot is not in state {} but {}",
                            BotLifecyclePhase.ACTIVE, getLifecyclePhase());
        }
    }

    @Override
    public void onConnectFromOtherAtom(final Connection con, final WonMessage wonMessage) {
        logMessage(wonMessage);
        if (getLifecyclePhase().isActive()) {
            eventBus.publish(new ConnectFromOtherAtomEvent(con, wonMessage));
        } else {
            logger.info(
                            "not publishing event for call to onConnectFromOtherAtom() as the bot is not in state {} but {}",
                            BotLifecyclePhase.ACTIVE, getLifecyclePhase());
        }
    }

    @Override
    public final void onNewAtomCreated(final URI atomUri, final URI wonNodeUri, final Dataset atomDataset)
                    throws Exception {
        if (getLifecyclePhase().isActive()) {
            eventBus.publish(new AtomCreatedEvent(atomUri, wonNodeUri, atomDataset, SocketType.ChatSocket));
        } else {
            logger.info("not publishing event for call to onNewAtomCreated() as the bot is not in state {} but {}",
                            BotLifecyclePhase.ACTIVE, getLifecyclePhase());
        }
    }

    @Override
    public final void onFailureResponse(final URI failedMessageUri, final WonMessage wonMessage,
                    Optional<Connection> con) {
        logMessage(wonMessage);
        if (getLifecyclePhase().isActive()) {
            eventBus.publish(new FailureResponseEvent(failedMessageUri, wonMessage, con));
        } else {
            logger.info("not publishing event for call to onFailureResponse() as the bot is not in state {} but {}",
                            BotLifecyclePhase.ACTIVE, getLifecyclePhase());
        }
    }

    @Override
    public final void onSuccessResponse(final URI successfulMessageUri, final WonMessage wonMessage,
                    Optional<Connection> con) {
        logMessage(wonMessage);
        if (getLifecyclePhase().isActive()) {
            eventBus.publish(new SuccessResponseEvent(successfulMessageUri, wonMessage, con));
        } else {
            logger.info("not publishing event for call to onSuccessResponse() as the bot is not in state {} but {}",
                            BotLifecyclePhase.ACTIVE, getLifecyclePhase());
        }
    }

    /*
     * Override this method to initialize your event listeners. Will be called
     * before the first event is published.
     */
    protected abstract void initializeEventListeners();

    /*
     * Override this method to shut down your event listeners. Will be called after
     * the last event is published. Event listeners may receive delayed events after
     * this method is called.
     */
    protected void shutdownEventListeners() {
        logger.info("shutdownEventListeners was not overridden by the subclass");
    }

    /**
     * Init method used to create the event bus and allow event listeners to
     * register. Do not override! It not final to allow for CGLIB autoproxying.
     */
    @Override
    public synchronized void initialize() throws Exception {
        super.initialize();
        eventBus = new AsyncEventBusImpl(getExecutor());
        // add an eventHandler that reacts to errors
        initializeEventListeners();
        eventBus.publish(new InitializeEvent());
    }

    /**
     * Init method used to shut down the event bus and allow event listeners shut
     * down. Do not override! It not final to allow for CGLIB autoproxying.
     */
    @Override
    public synchronized void shutdown() throws Exception {
        eventBus.publish(new ShutdownEvent());
        shutdownEventListeners();
        super.shutdown();
    }

    protected EventBus getEventBus() {
        return eventBus;
    }

    protected EventListenerContext getEventListenerContext() {
        return eventListenerContext;
    }

    /**
     * Class holding references to all important services that EventListeners inside
     * bots need to access.
     */
    public class MyEventListenerContext implements EventListenerContext {
        public TaskScheduler getTaskScheduler() {
            return EventBot.this.getTaskScheduler();
        }

        public NodeURISource getNodeURISource() {
            return EventBot.this.getNodeURISource();
        }

        @Override
        public MatcherNodeURISource getMatcherNodeURISource() {
            return EventBot.this.getMatcherNodeURISource();
        }

        public WonMessageSender getWonMessageSender() {
            return getWonMessageSenderWrapperLazily();
        }

        public MatcherProtocolAtomServiceClientSide getMatcherProtocolAtomServiceClient() {
            return EventBot.this.getMatcherProtocolAtomServiceClient();
        }

        @Override
        public MatcherProtocolMatcherServiceImplJMSBased getMatcherProtocolMatcherService() {
            return EventBot.this.getMatcherProtocolMatcherService();
        }

        public AtomProducer getAtomProducer() {
            return EventBot.this.getAtomProducer();
        }

        public void cancelTrigger() {
            EventBot.this.cancelTrigger();
        }

        @Override
        public void workIsDone() {
            EventBot.this.workIsDone();
        }

        public EventBus getEventBus() {
            return EventBot.this.getEventBus();
        }

        @Override
        public BotContext getBotContext() {
            return EventBot.this.getBotContextWrapper().getBotContext();
        }

        @Override
        public BotContextWrapper getBotContextWrapper() {
            return EventBot.this.getBotContextWrapper();
        }

        /**
         * Returns an executor that passes the tasks to the TaskScheduler for immediate
         * execution.
         * 
         * @return
         */
        public Executor getExecutor() {
            return EventBot.this.getExecutor();
        }

        @Override
        public LinkedDataSource getLinkedDataSource() {
            return EventBot.this.getLinkedDataSource();
        }

        @Override
        public WonNodeInformationService getWonNodeInformationService() {
            return EventBot.this.getWonNodeInformationService();
        }
    }

    private EventGeneratingWonMessageSenderWrapper getWonMessageSenderWrapperLazily() {
        if (wonMessageSenderWrapper == null) {
            wonMessageSenderWrapper = new EventGeneratingWonMessageSenderWrapper(getWonMessageSender());
        }
        return wonMessageSenderWrapper;
    }

    /**
     * Wraps a WonMessageSender so a WonMessageSentEvent can be published after a
     * message has been sent.
     */
    private class EventGeneratingWonMessageSenderWrapper implements WonMessageSender {
        private final WonMessageSender delegate;

        public EventGeneratingWonMessageSenderWrapper(final WonMessageSender delegate) {
            this.delegate = delegate;
        }

        @Override
        public void sendWonMessage(final WonMessage message) throws WonMessageSenderException {
            if (logger.isDebugEnabled()) {
                logger.debug("sending message " + message.toStringForDebug(true));
            }
            delegate.sendWonMessage(message);
            // publish the WonMessageSent event if no exception was raised
            WonMessageType type = message.getMessageType();
            Event event;
            // if the event is connection specific, raise a more specialized event
            switch (type) {
                case CLOSE:
                case CONNECT:
                case CONNECTION_MESSAGE:
                    event = new WonMessageSentOnConnectionEvent(
                                    WonLinkedDataUtils.getConnectionForOutgoingMessage(message, getLinkedDataSource())
                                                    .orElseThrow(() -> new IllegalStateException(
                                                                    "Could not obtain connection data for message "
                                                                                    + message.toShortStringForDebug())),
                                    message);
                    break;
                default:
                    event = new WonMessageSentEvent(message);
            }
            getEventBus().publish(event);
        }
    }

    /**
     * Initializes and returns a NotFilter that can be used to exclude Events that
     * are caused by Atoms created by this bot
     * 
     * @return NotFilter that excludes all created Atoms by this Bot
     * @throws IllegalStateException if EventListenerContext or BotContextWrapper
     * are null, and therefore a Filter cant be Created
     */
    protected NotFilter getNoOwnAtomsFilter() throws IllegalStateException {
        if (Objects.nonNull(eventListenerContext) && Objects.nonNull(eventListenerContext.getBotContextWrapper())) {
            return new NotFilter(new AtomUriInNamedListFilter(eventListenerContext,
                            eventListenerContext.getBotContextWrapper().getAtomCreateListName()));
        } else {
            throw new IllegalStateException("Can't create Filter, EventListenerContext or BotContextWrapper are null");
        }
    }
}

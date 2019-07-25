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

import java.net.URI;
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
import won.bot.framework.eventbot.event.impl.lifecycle.ErrorEvent;
import won.bot.framework.eventbot.event.impl.lifecycle.InitializeEvent;
import won.bot.framework.eventbot.event.impl.lifecycle.ShutdownEvent;
import won.bot.framework.eventbot.event.impl.matcher.AtomActivatedEventForMatcher;
import won.bot.framework.eventbot.event.impl.matcher.AtomCreatedEventForMatcher;
import won.bot.framework.eventbot.event.impl.matcher.AtomDeactivatedEventForMatcher;
import won.bot.framework.eventbot.event.impl.matcher.AtomModifiedEventForMatcher;
import won.bot.framework.eventbot.event.impl.matcher.MatcherRegisteredEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.AtomHintFromMatcherEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.CloseFromOtherAtomEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.ConnectFromOtherAtomEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.FailureResponseEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.MessageFromOtherAtomEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.OpenFromOtherAtomEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.SocketHintFromMatcherEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.SuccessResponseEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.WonMessageSentEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.WonMessageSentOnConnectionEvent;
import won.bot.framework.eventbot.listener.BaseEventListener;
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
public abstract class EventBot extends TriggeredBot {
    private final Logger logger = LoggerFactory.getLogger(getClass());
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

    @Override
    public final void onMessageFromOtherAtom(final Connection con, final WonMessage wonMessage) {
        if (getLifecyclePhase().isActive()) {
            eventBus.publish(new MessageFromOtherAtomEvent(con, wonMessage));
        } else {
            logger.info("not publishing event for call to onMessageFromOtherAtom() as the bot is not in state {} but {}",
                            BotLifecyclePhase.ACTIVE, getLifecyclePhase());
        }
    }

    @Override
    public final void onAtomHintFromMatcher(final WonMessage wonMessage) {
        if (getLifecyclePhase().isActive()) {
            eventBus.publish(new AtomHintFromMatcherEvent(wonMessage));
        } else {
            logger.info("not publishing event for call to onAtomHintFromMatcher() as the bot is not in state {} but {}",
                            BotLifecyclePhase.ACTIVE, getLifecyclePhase());
        }
    }

    @Override
    public final void onSocketHintFromMatcher(final WonMessage wonMessage) {
        if (getLifecyclePhase().isActive()) {
            eventBus.publish(new SocketHintFromMatcherEvent(wonMessage));
        } else {
            logger.info("not publishing event for call to onSocketHintFromMatcher() as the bot is not in state {} but {}",
                            BotLifecyclePhase.ACTIVE, getLifecyclePhase());
        }
    }

    @Override
    public final void onCloseFromOtherAtom(final Connection con, final WonMessage wonMessage) {
        if (getLifecyclePhase().isActive()) {
            eventBus.publish(new CloseFromOtherAtomEvent(con, wonMessage));
        } else {
            logger.info("not publishing event for call to onClose() as the bot is not in state {} but {}",
                            BotLifecyclePhase.ACTIVE, getLifecyclePhase());
        }
    }

    @Override
    public final void onOpenFromOtherAtom(final Connection con, final WonMessage wonMessage) {
        if (getLifecyclePhase().isActive()) {
            eventBus.publish(new OpenFromOtherAtomEvent(con, wonMessage));
        } else {
            logger.info("not publishing event for call to onOpenFromOtherAtom() as the bot is not in state {} but {}",
                            BotLifecyclePhase.ACTIVE, getLifecyclePhase());
        }
    }

    @Override
    public void onConnectFromOtherAtom(final Connection con, final WonMessage wonMessage) {
        if (getLifecyclePhase().isActive()) {
            eventBus.publish(new ConnectFromOtherAtomEvent(con, wonMessage));
        } else {
            logger.info("not publishing event for call to onConnectFromOtherAtom() as the bot is not in state {} but {}",
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
    public final void onNewAtomCreatedNotificationForMatcher(final URI wonNodeURI, final URI atomUri,
                    final Dataset wonMessageDataset) {
        if (getLifecyclePhase().isActive()) {
            Dataset dataset = getEventListenerContext().getLinkedDataSource().getDataForResource(atomUri);
            eventBus.publish(new AtomCreatedEventForMatcher(atomUri, dataset));
        } else {
            logger.info("not publishing event for call to onNewAtomCreated() as the bot is not in state {} but {}",
                            BotLifecyclePhase.ACTIVE, getLifecyclePhase());
        }
    }

    @Override
    public final void onMatcherRegistered(final URI wonNodeUri) {
        if (getLifecyclePhase().isActive()) {
            // EventBotActionUtils.rememberInNodeListIfNamePresent(getEventListenerContext(),wonNodeUri);
            eventBus.publish(new MatcherRegisteredEvent(wonNodeUri));
        } else {
            logger.info("not publishing event for call to onNewAtomCreated() as the bot is not in state {} but {}",
                            BotLifecyclePhase.ACTIVE, getLifecyclePhase());
        }
    }

    @Override
    public final void onAtomModifiedNotificationForMatcher(final URI wonNodeURI, final URI atomURI) {
        if (getLifecyclePhase().isActive()) {
            eventBus.publish(new AtomModifiedEventForMatcher(atomURI));
        } else {
            logger.info("not publishing event for call to onNewAtomCreated() as the bot is not in state {} but {}",
                            BotLifecyclePhase.ACTIVE, getLifecyclePhase());
        }
    }

    @Override
    public final void onAtomActivatedNotificationForMatcher(final URI wonNodeURI, final URI atomURI) {
        if (getLifecyclePhase().isActive()) {
            eventBus.publish(new AtomActivatedEventForMatcher(atomURI));
        } else {
            logger.info("not publishing event for call to onNewAtomCreated() as the bot is not in state {} but {}",
                            BotLifecyclePhase.ACTIVE, getLifecyclePhase());
        }
    }

    @Override
    public final void onAtomDeactivatedNotificationForMatcher(final URI wonNodeURI, final URI atomURI) {
        if (getLifecyclePhase().isActive()) {
            eventBus.publish(new AtomDeactivatedEventForMatcher(atomURI));
        } else {
            logger.info("not publishing event for call to onNewAtomCreated() as the bot is not in state {} but {}",
                            BotLifecyclePhase.ACTIVE, getLifecyclePhase());
        }
    }

    @Override
    public final void onFailureResponse(final URI failedMessageUri, final WonMessage wonMessage) {
        if (getLifecyclePhase().isActive()) {
            eventBus.publish(new FailureResponseEvent(failedMessageUri, wonMessage));
        } else {
            logger.info("not publishing event for call to onFailureResponse() as the bot is not in state {} but {}",
                            BotLifecyclePhase.ACTIVE, getLifecyclePhase());
        }
    }

    @Override
    public final void onSuccessResponse(final URI successfulMessageUri, final WonMessage wonMessage) {
        if (getLifecyclePhase().isActive()) {
            eventBus.publish(new SuccessResponseEvent(successfulMessageUri, wonMessage));
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
    };

    /**
     * Init method used to create the event bus and allow event listeners to
     * register. Do not override! It not final to allow for CGLIB autoproxying.
     */
    @Override
    protected void doInitializeCustom() {
        this.eventBus = new AsyncEventBusImpl(getExecutor());
        // add an eventhandler that reacts to errors
        this.getEventBus().subscribe(ErrorEvent.class, new ErrorEventListener(getEventListenerContext()));
        initializeEventListeners();
        this.eventBus.publish(new InitializeEvent());
    }

    /**
     * Init method used to shut down the event bus and allow event listeners shut
     * down. Do not override! It not final to allow for CGLIB autoproxying.
     */
    @Override
    protected void doShutdownCustom() {
        eventBus.publish(new ShutdownEvent());
        shutdownEventListeners();
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

        public URI getSolrServerURI() {
            return EventBot.this.getSolrServerURI();
        }

        @Override
        public MatcherNodeURISource getMatcherNodeURISource() {
            return EventBot.this.getMatcheNodeURISource();
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
        if (this.wonMessageSenderWrapper == null) {
            this.wonMessageSenderWrapper = new EventGeneratingWonMessageSenderWrapper(
                            EventBot.this.getWonMessageSender());
        }
        return wonMessageSenderWrapper;
    }

    /**
     * Event listener that will stop the bot by publishing a WorkDoneEvent if an
     * ErrorEvent is seen. Expects to be registered for WorkDoneEvents and
     * ErrorEvents and will not react to a WorkDoneEvent.
     */
    private class ErrorEventListener extends BaseEventListener {
        private final Logger logger = LoggerFactory.getLogger(getClass());

        public ErrorEventListener(EventListenerContext context) {
            super(context);
        }

        @Override
        protected void doOnEvent(final won.bot.framework.eventbot.event.Event event) throws Exception {
            if (event instanceof ErrorEvent) {
                Throwable t = ((ErrorEvent) event).getThrowable();
                logger.info("Encountered an error:", t);
            }
        }
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
            delegate.sendWonMessage(message);
            // publish the WonMessageSent event if no exception was raised
            WonMessageType type = message.getMessageType();
            Event event = null;
            // if the event is connection specific, raise a more specialized event
            switch (type) {
                case CLOSE:
                case CONNECT:
                case CONNECTION_MESSAGE:
                case OPEN:
                    event = new WonMessageSentOnConnectionEvent(message);
                    break;
                default:
                    event = new WonMessageSentEvent(message);
            }
            getEventBus().publish(event);
        }
    }
}

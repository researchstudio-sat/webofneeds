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

package won.bot.framework.bot.base;

import java.net.URI;
import java.util.concurrent.Executor;

import org.apache.jena.query.Dataset;
import org.springframework.scheduling.TaskScheduler;

import won.bot.framework.bot.BotLifecyclePhase;
import won.bot.framework.bot.context.BotContext;
import won.bot.framework.bot.context.BotContextWrapper;
import won.bot.framework.component.needproducer.NeedProducer;
import won.bot.framework.component.nodeurisource.NodeURISource;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.bus.EventBus;
import won.bot.framework.eventbot.bus.impl.AsyncEventBusImpl;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.lifecycle.ActEvent;
import won.bot.framework.eventbot.event.impl.lifecycle.ErrorEvent;
import won.bot.framework.eventbot.event.impl.lifecycle.InitializeEvent;
import won.bot.framework.eventbot.event.impl.lifecycle.ShutdownEvent;
import won.bot.framework.eventbot.event.impl.matcher.MatcherRegisteredEvent;
import won.bot.framework.eventbot.event.impl.matcher.NeedActivatedEventForMatcher;
import won.bot.framework.eventbot.event.impl.matcher.NeedCreatedEventForMatcher;
import won.bot.framework.eventbot.event.impl.matcher.NeedDeactivatedEventForMatcher;
import won.bot.framework.eventbot.event.impl.needlifecycle.NeedCreatedEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.CloseFromOtherNeedEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.ConnectFromOtherNeedEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.FailureResponseEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.HintFromMatcherEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.MessageFromOtherNeedEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.OpenFromOtherNeedEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.SuccessResponseEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.WonMessageSentEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.WonMessageSentOnConnectionEvent;
import won.bot.framework.eventbot.listener.BaseEventListener;
import won.matcher.component.MatcherNodeURISource;
import won.matcher.protocol.impl.MatcherProtocolMatcherServiceImplJMSBased;
import won.protocol.matcher.MatcherProtocolNeedServiceClientSide;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageType;
import won.protocol.message.sender.WonMessageSender;
import won.protocol.message.sender.exception.WonMessageSenderException;
import won.protocol.model.Connection;
import won.protocol.model.FacetType;
import won.protocol.model.Match;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.linkeddata.LinkedDataSource;

/**
 * Base class for bots that define their behaviour through event listeners.
 *
 * Once the bot's work is done, the workIsDone() method should be called to
 * allow the framework to shut down the bot gracefully.
 *
 * All methods from the Bot interface are converted to Events by the EventBot.
 * Subclasses should implement the initializeEventListeners() method to register
 * their event listeners. The corresponding shutdownEventListeners() is called
 * when the bot's shutdown method is called an may be used to perform shutdown
 * work for listeners.
 *
 * An event bot implementation should take care not to do long or blocking work
 * in event listeners. Whenever a long computation or a blocking call is done,
 * it should be done in a Runnable, executed in the Executor (see below).
 *
 * The bot's services and other environment can be accessed in listeners through
 * the instance of EventListenerContext.
 *
 * these services include:
 *
 * <ul>
 * <li>the event bus - for subscribing for/unsubscribing for/publishing
 * event</li>
 * <li>the task scheduler - for scheduling tasks (Runnables) to be run after a
 * timeout</li>
 * <li>the executor - for running tasks (Runnables) immediately in the task
 * scheduler's thread pool</li>
 * <li>the node uri source - for obtaining a won node URI for need creation</li>
 * <li>the need producer - for obtaining a valid need content in RDF that can be
 * used to create a need</li>
 * <li>the owner service - for sending messages to won nodes (create needs,
 * connect, open, message, close)</li>
 * <li>shutting down the trigger - an event bot may have a trigger that calls
 * the bot's act() method regularly.</li>
 * </ul>
 *
 * The bot will only react to onXX methods (i.e., create events and publish them
 * on the internal event bus) if it is in lifecycle phase ACTIVE.
 *
 */
public abstract class EventBot extends TriggeredBot {
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
  public final void onMessageFromOtherNeed(final Connection con, final WonMessage wonMessage) {
    if (getLifecyclePhase().isActive()) {
      eventBus.publish(new MessageFromOtherNeedEvent(con, wonMessage));
    } else {
      logger.info("not publishing event for call to onMessageFromOtherNeed() as the bot is not in state {} but {}",
          BotLifecyclePhase.ACTIVE, getLifecyclePhase());
    }
  }

  @Override
  public final void onHintFromMatcher(final Match match, final WonMessage wonMessage) {
    if (getLifecyclePhase().isActive()) {
      eventBus.publish(new HintFromMatcherEvent(match, wonMessage));
    } else {
      logger.info("not publishing event for call to onHintFromMatcher() as the bot is not in state {} but {}",
          BotLifecyclePhase.ACTIVE, getLifecyclePhase());
    }
  }

  @Override
  public final void onCloseFromOtherNeed(final Connection con, final WonMessage wonMessage) {
    if (getLifecyclePhase().isActive()) {
      eventBus.publish(new CloseFromOtherNeedEvent(con, wonMessage));
    } else {
      logger.info("not publishing event for call to onClose() as the bot is not in state {} but {}",
          BotLifecyclePhase.ACTIVE, getLifecyclePhase());
    }
  }

  @Override
  public final void onOpenFromOtherNeed(final Connection con, final WonMessage wonMessage) {
    if (getLifecyclePhase().isActive()) {
      eventBus.publish(new OpenFromOtherNeedEvent(con, wonMessage));
    } else {
      logger.info("not publishing event for call to onOpenFromOtherNeed() as the bot is not in state {} but {}",
          BotLifecyclePhase.ACTIVE, getLifecyclePhase());
    }
  }

  @Override
  public void onConnectFromOtherNeed(final Connection con, final WonMessage wonMessage) {
    if (getLifecyclePhase().isActive()) {
      eventBus.publish(new ConnectFromOtherNeedEvent(con, wonMessage));
    } else {
      logger.info("not publishing event for call to onConnectFromOtherNeed() as the bot is not in state {} but {}",
          BotLifecyclePhase.ACTIVE, getLifecyclePhase());
    }
  }

  @Override
  public final void onNewNeedCreated(final URI needUri, final URI wonNodeUri, final Dataset needDataset)
      throws Exception {
    if (getLifecyclePhase().isActive()) {
      eventBus.publish(new NeedCreatedEvent(needUri, wonNodeUri, needDataset, FacetType.ChatFacet));
    } else {
      logger.info("not publishing event for call to onNewNeedCreated() as the bot is not in state {} but {}",
          BotLifecyclePhase.ACTIVE, getLifecyclePhase());
    }
  }

  @Override
  public final void onNewNeedCreatedNotificationForMatcher(final URI wonNodeURI, final URI needUri,
      final Dataset wonMessageDataset) {
    if (getLifecyclePhase().isActive()) {
      Dataset dataset = getEventListenerContext().getLinkedDataSource().getDataForResource(needUri);
      eventBus.publish(new NeedCreatedEventForMatcher(needUri, dataset));
    } else {
      logger.info("not publishing event for call to onNewNeedCreated() as the bot is not in state {} but {}",
          BotLifecyclePhase.ACTIVE, getLifecyclePhase());
    }
  }

  @Override
  public final void onMatcherRegistered(final URI wonNodeUri) {
    if (getLifecyclePhase().isActive()) {
      // EventBotActionUtils.rememberInNodeListIfNamePresent(getEventListenerContext(),wonNodeUri);
      eventBus.publish(new MatcherRegisteredEvent(wonNodeUri));
    } else {
      logger.info("not publishing event for call to onNewNeedCreated() as the bot is not in state {} but {}",
          BotLifecyclePhase.ACTIVE, getLifecyclePhase());
    }
  }

  @Override
  public final void onNeedActivatedNotificationForMatcher(final URI wonNodeURI, final URI needURI) {
    if (getLifecyclePhase().isActive()) {
      eventBus.publish(new NeedActivatedEventForMatcher(needURI));
    } else {
      logger.info("not publishing event for call to onNewNeedCreated() as the bot is not in state {} but {}",
          BotLifecyclePhase.ACTIVE, getLifecyclePhase());
    }
  }

  @Override
  public final void onNeedDeactivatedNotificationForMatcher(final URI wonNodeURI, final URI needURI) {
    if (getLifecyclePhase().isActive()) {
      eventBus.publish(new NeedDeactivatedEventForMatcher(needURI));
    } else {
      logger.info("not publishing event for call to onNewNeedCreated() as the bot is not in state {} but {}",
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

    public MatcherProtocolNeedServiceClientSide getMatcherProtocolNeedServiceClient() {
      return EventBot.this.getMatcherProtocolNeedServiceClient();
    }

    @Override
    public MatcherProtocolMatcherServiceImplJMSBased getMatcherProtocolMatcherService() {
      return EventBot.this.getMatcherProtocolMatcherService();
    }

    public NeedProducer getNeedProducer() {
      return EventBot.this.getNeedProducer();
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
      this.wonMessageSenderWrapper = new EventGeneratingWonMessageSenderWrapper(EventBot.this.getWonMessageSender());
    }
    return wonMessageSenderWrapper;
  }

  /**
   * Event listener that will stop the bot by publishing a WorkDoneEvent if an
   * ErrorEvent is seen. Expects to be registered for WorkDoneEvents and
   * ErrorEvents and will not react to a WorkDoneEvent.
   */
  private class ErrorEventListener extends BaseEventListener {
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

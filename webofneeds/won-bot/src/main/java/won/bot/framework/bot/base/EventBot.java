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

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;
import org.springframework.scheduling.TaskScheduler;
import won.bot.framework.bot.BotContext;
import won.bot.framework.bot.BotLifecyclePhase;
import won.bot.framework.component.needproducer.NeedProducer;
import won.bot.framework.component.nodeurisource.NodeURISource;
import won.bot.framework.events.EventListenerContext;
import won.bot.framework.events.bus.EventBus;
import won.bot.framework.events.bus.impl.AsyncEventBusImpl;
import won.bot.framework.events.event.Event;
import won.bot.framework.events.event.impl.*;
import won.bot.framework.events.listener.BaseEventListener;
import won.matcher.component.MatcherNodeURISource;
import won.matcher.protocol.impl.MatcherProtocolMatcherServiceImplJMSBased;
import won.protocol.matcher.MatcherProtocolNeedServiceClientSide;
import won.protocol.message.WonMessage;
import won.protocol.message.sender.WonMessageSender;
import won.protocol.model.Connection;
import won.protocol.model.FacetType;
import won.protocol.model.Match;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.linkeddata.LinkedDataSource;

import java.net.URI;
import java.util.concurrent.Executor;

/**
 * Base class for bots that define their behaviour through event listeners.
 *
 * Once the bot's work is done, the workIsDone() method should be called to allow the
 * framework to shut down the bot gracefully.
 *
 * All methods from the Bot interface are converted to Events by the EventBot.
 * Subclasses should implement the initializeEventListeners() method to register their
 * event listeners. The corresponding shutdownEventListeners() is called when the bot's
 * shutdown method is called an may be used to perform shutdown work for listeners.
 *
 * An event bot implementation should take care not to do long or blocking work in event
 * listeners. Whenever a long computation or a blocking call is done, it should be
 * done in a Runnable, executed in the Executor (see below).
 *
 * The bot's services and other environment can be accessed in listeners through the
 * instance of EventListenerContext.
 *
 * these services include:
 *
 * <ul>
 *   <li>the event bus - for subscribing for/unsubscribing for/publishing event</li>
 *   <li>the task scheduler - for scheduling tasks (Runnables) to be run after a timeout</li>
 *   <li>the executor - for running tasks (Runnables) immediately in the task scheduler's thread pool</li>
 *   <li>the node uri source - for obtaining a won node URI for need creation</li>
 *   <li>the need producer - for obtaining a valid need content in RDF that can be used to create a need</li>
 *   <li>the owner service - for sending messages to won nodes (create needs, connect, open, message, close) </li>
 *   <li>shutting down the trigger - an event bot may have a trigger that calls the bot's act() method regularly.</li>
 * </ul>
 *
 * The bot will only react to onXX methods (i.e., create events and publish them on the internal event bus) if it is in lifecycle phase ACTIVE.
 *
 */
public class EventBot extends TriggeredBot
{
  private EventBus eventBus;
  private EventListenerContext eventListenerContext = new MyEventListenerContext();



  @Override
  public void act() throws Exception
  {
    if (getLifecyclePhase().isActive()){
      eventBus.publish(new ActEvent());
    } else {
      logger.info("not publishing event for call to act() as the bot is not in state {} but {}", BotLifecyclePhase.ACTIVE, getLifecyclePhase());
    }
  }

  @Override
  public void onMessageFromOtherNeed(final Connection con, final WonMessage wonMessage)
  {
    if (getLifecyclePhase().isActive()){
      eventBus.publish(new MessageFromOtherNeedEvent(con,  wonMessage));
    } else {
      logger.info("not publishing event for call to onMessageFromOtherNeed() as the bot is not in state {} but {}", BotLifecyclePhase.ACTIVE, getLifecyclePhase());
    }
  }

  @Override
  public void onHintFromMatcher(final Match match, final WonMessage wonMessage)
  {
    if (getLifecyclePhase().isActive()){
      eventBus.publish(new HintFromMatcherEvent(match, wonMessage));
    } else {
      logger.info("not publishing event for call to onHintFromMatcher() as the bot is not in state {} but {}", BotLifecyclePhase.ACTIVE, getLifecyclePhase());
    }
  }

  @Override
  public void onCloseFromOtherNeed(final Connection con, final WonMessage wonMessage)
  {
    if (getLifecyclePhase().isActive()){
      eventBus.publish(new CloseFromOtherNeedEvent(con, wonMessage));
    } else {
      logger.info("not publishing event for call to onClose() as the bot is not in state {} but {}", BotLifecyclePhase.ACTIVE, getLifecyclePhase());
    }
  }

  @Override
  public void onOpenFromOtherNeed(final Connection con, final WonMessage wonMessage)
  {
    if (getLifecyclePhase().isActive()){
      eventBus.publish(new OpenFromOtherNeedEvent(con, wonMessage));
    } else {
      logger.info("not publishing event for call to onOpenFromOtherNeed() as the bot is not in state {} but {}", BotLifecyclePhase.ACTIVE, getLifecyclePhase());
    }
  }

  @Override
  public void onConnectFromOtherNeed(final Connection con, final WonMessage wonMessage)
  {
    if (getLifecyclePhase().isActive()){
      eventBus.publish(new ConnectFromOtherNeedEvent(con, wonMessage));
    } else {
      logger.info("not publishing event for call to onConnectFromOtherNeed() as the bot is not in state {} but {}", BotLifecyclePhase.ACTIVE, getLifecyclePhase());
    }
  }

  @Override
  public void onNewNeedCreated(final URI needUri, final URI wonNodeUri, final Model needModel) throws Exception
  {
    if (getLifecyclePhase().isActive()){
      eventBus.publish(new NeedCreatedEvent(needUri, wonNodeUri, needModel, FacetType.OwnerFacet));
    } else {
      logger.info("not publishing event for call to onNewNeedCreated() as the bot is not in state {} but {}", BotLifecyclePhase.ACTIVE, getLifecyclePhase());
    }
  }
  @Override
  public void onNewNeedCreatedNotificationForMatcher(final URI wonNodeURI, final URI needUri, final Dataset wonMessageDataset)
  {
    if (getLifecyclePhase().isActive()){
      eventBus.publish(new NeedCreatedEventForMatcher(needUri, wonMessageDataset));
    } else {
      logger.info("not publishing event for call to onNewNeedCreated() as the bot is not in state {} but {}", BotLifecyclePhase.ACTIVE, getLifecyclePhase());
    }
  }
  @Override
  public void onMatcherRegistered(final URI wonNodeUri)
  {
    if (getLifecyclePhase().isActive()){
      //EventBotActionUtils.rememberInNodeListIfNamePresent(getEventListenerContext(),wonNodeUri);
      eventBus.publish(new MatcherRegisteredEvent(wonNodeUri));
    } else {
      logger.info("not publishing event for call to onNewNeedCreated() as the bot is not in state {} but {}", BotLifecyclePhase.ACTIVE, getLifecyclePhase());
    }
  }
  @Override
  public void onNeedActivatedNotificationForMatcher(final URI wonNodeURI, final URI needURI){
    if (getLifecyclePhase().isActive()){
      eventBus.publish(new NeedActivatedEventForMatcher(needURI));
    } else {
      logger.info("not publishing event for call to onNewNeedCreated() as the bot is not in state {} but {}", BotLifecyclePhase.ACTIVE, getLifecyclePhase());
    }
  }
  @Override
  public void onNeedDeactivatedNotificationForMatcher(final URI wonNodeURI, final URI needURI){
    if (getLifecyclePhase().isActive()){
      eventBus.publish(new NeedDeactivatedEventForMatcher(needURI));
    } else {
      logger.info("not publishing event for call to onNewNeedCreated() as the bot is not in state {} but {}", BotLifecyclePhase.ACTIVE, getLifecyclePhase());
    }
  }

  @Override
  public void onFailureResponse(final URI failedMessageUri, final WonMessage wonMessage) {
    if (getLifecyclePhase().isActive()){
      eventBus.publish(new FailureResponseEvent(failedMessageUri, wonMessage));
    } else {
      logger.info("not publishing event for call to onFailureResponse() as the bot is not in state {} but {}",
        BotLifecyclePhase.ACTIVE, getLifecyclePhase());
    }
  }

  @Override
  public void onSuccessResponse(final URI successfulMessageUri, final WonMessage wonMessage) {
    if (getLifecyclePhase().isActive()){
      eventBus.publish(new SuccessResponseEvent(successfulMessageUri, wonMessage));
    } else {
      logger.info("not publishing event for call to onSuccessResponse() as the bot is not in state {} but {}",
        BotLifecyclePhase.ACTIVE, getLifecyclePhase());
    }
  }

  /*
     * Override this method to initialize your event listeners. Will be called before
     * the first event is published.
     */
  protected void initializeEventListeners(){

  }

  /*
   * Override this method to shut down your event listeners. Will be called after
   * the last event is published. Event listeners may receive delayed events after
   * this method is called.
   */
  protected void shutdownEventListeners(){

  }


  /**
   * Init method used to create the event bus and allow event listeners to register. Do not override!
   * It not final to allow for CGLIB autoproxying.
   */
  @Override
  protected void doInitializeCustom()
  {
    this.eventBus = new AsyncEventBusImpl(getExecutor());
    //add an eventhandler that reacts to errors
    this.getEventBus().subscribe(ErrorEvent.class,new ErrorEventListener(getEventListenerContext()));
    initializeEventListeners();
    this.eventBus.publish(new InitializeEvent());
  }

  /**
   * Init method used to shut down the event bus and allow event listeners shut down. Do not override!
   * It not final to allow for CGLIB autoproxying.
   */
  @Override
  protected void doShutdownCustom()
  {
    eventBus.publish(new ShutdownEvent());
    shutdownEventListeners();
  }

  protected EventBus getEventBus(){
    return eventBus;
  }

  protected EventListenerContext getEventListenerContext()
  {
    return eventListenerContext;
  }

  /**
   * Class holding references to all important services that EventListeners inside bots need to
   * access.
   */
  public class MyEventListenerContext implements EventListenerContext
  {

    public TaskScheduler getTaskScheduler() {
      return EventBot.this.getTaskScheduler();
    }

    public NodeURISource getNodeURISource()
    {
      return EventBot.this.getNodeURISource();
    }

    public URI getSolrServerURI(){
      return EventBot.this.getSolrServerURI();
    }

    @Override
    public MatcherNodeURISource getMatcherNodeURISource() {
      return EventBot.this.getMatcheNodeURISource();
    }

    public WonMessageSender getWonMessageSender()
    {
      return EventBot.this.getWonMessageSender();
    }

    public MatcherProtocolNeedServiceClientSide getMatcherProtocolNeedServiceClient(){
        return EventBot.this.getMatcherProtocolNeedServiceClient();
    }

    @Override
    public MatcherProtocolMatcherServiceImplJMSBased getMatcherProtocolMatcherService() {
      return EventBot.this.getMatcherProtocolMatcherService();
    }

    public NeedProducer getNeedProducer()
    {
      return EventBot.this.getNeedProducer();
    }


    public void cancelTrigger(){
      EventBot.this.cancelTrigger();
    }

    @Override
    public void workIsDone()
    {
      EventBot.this.workIsDone();
    }

    public EventBus getEventBus() {
      return EventBot.this.getEventBus();
    }

    public BotContext getBotContext() {
      return EventBot.this.getBotContext();
    }

    /**
     * Returns an executor that passes the tasks to the TaskScheduler for immediate execution.
     * @return
     */
    public Executor getExecutor(){
      return EventBot.this.getExecutor();
    }

    @Override
    public LinkedDataSource getLinkedDataSource()
    {
      return EventBot.this.getLinkedDataSource();
    }

    @Override
    public WonNodeInformationService getWonNodeInformationService() {
      return EventBot.this.getWonNodeInformationService();
    }
  }

  /**
   * Event listener that will stop the bot by publishing a WorkDoneEvent if an ErrorEvent is seen.
   * Expects to be registered for WorkDoneEvents and ErrorEvents and will not react to a WorkDoneEvent.
   */
  private class ErrorEventListener extends BaseEventListener {

    private boolean done = false;
    private Object monitor = new Object();
    public ErrorEventListener(EventListenerContext context) {
      super(context);
    }

    @Override
    protected void doOnEvent(final Event event) throws Exception {
      synchronized (monitor) {
        if (event instanceof ErrorEvent) {
          //only react to an ErrorEvent
          if (! done) {
            //make sure we send only one WorkDoneEvent
            logger.warn("saw an ErrorEvent, stopping the bot by publishing a WorkDoneEvent", ((ErrorEvent) event).getThrowable());
            getEventListenerContext().getEventBus().publish(new WorkDoneEvent(EventBot.this));
          }
          setDoneAndUnsubscribe();
        }
        if (event instanceof WorkDoneEvent) {
          setDoneAndUnsubscribe();
        }
      }
    }

    private void setDoneAndUnsubscribe() {
      done = true;
      getEventBus().unsubscribe(this);
    }
  }

}

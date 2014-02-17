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

import com.hp.hpl.jena.rdf.model.Model;
import org.springframework.scheduling.TaskScheduler;
import won.bot.framework.bot.BotContext;
import won.bot.framework.events.event.*;
import won.bot.framework.events.listener.EventListenerContext;
import won.bot.framework.events.EventBus;
import won.bot.framework.events.bus.SchedulerEventBusImpl;
import won.bot.framework.component.needproducer.NeedProducer;
import won.bot.framework.component.nodeurisource.NodeURISource;
import won.protocol.model.ChatMessage;
import won.protocol.model.Connection;
import won.protocol.model.FacetType;
import won.protocol.model.Match;
import won.protocol.owner.OwnerProtocolNeedServiceClientSide;

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
 *
 */
public class EventBot extends TriggeredBot
{
  private EventBus eventBus;
  private EventListenerContext eventListenerContext = new MyEventListenerContext();



  @Override
  public final void act() throws Exception
  {
    eventBus.publish(new ActEvent());
  }

  @Override
  public final void onMessageFromOtherNeed(final Connection con, final ChatMessage message, final Model content) throws Exception
  {
    eventBus.publish(new MessageFromOtherNeedEvent(con, message, content));
  }

  @Override
  public final void onHintFromMatcher(final Match match, final Model content) throws Exception
  {
    eventBus.publish(new HintFromMatcherEvent(match, content));
  }

  @Override
  public final void onCloseFromOtherNeed(final Connection con, final Model content) throws Exception
  {
    logger.info("ON CLOSE");
    eventBus.publish(new CloseFromOtherNeedEvent(con, content));
  }

  @Override
  public final void onOpenFromOtherNeed(final Connection con, final Model content) throws Exception
  {
    eventBus.publish(new OpenFromOtherNeedEvent(con, content));
  }

  @Override
  public final void onConnectFromOtherNeed(final Connection con, final Model content) throws Exception
  {
    eventBus.publish(new ConnectFromOtherNeedEvent(con, content));
  }

  @Override
  public final void onNewNeedCreated(final URI needUri, final URI wonNodeUri, final Model needModel) throws Exception
  {
    eventBus.publish(new NeedCreatedEvent(needUri, wonNodeUri, needModel, FacetType.OwnerFacet));
  }
  @Override
  public void onNewGroupCreated(final URI groupURI, final URI wonNodeURI, final Model groupModel)
  {
    eventBus.publish(new GroupFacetCreatedEvent(groupURI, wonNodeURI, groupModel));
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


  @Override
  protected final void doInitializeCustom()
  {
    this.eventBus = new SchedulerEventBusImpl(getTaskScheduler());
    initializeEventListeners();
    eventBus.publish(new InitializeEvent());
  }

  @Override
  protected final void doShutdownCustom()
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

    public OwnerProtocolNeedServiceClientSide getOwnerService()
    {
      return EventBot.this.getOwnerService();
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
  }

}

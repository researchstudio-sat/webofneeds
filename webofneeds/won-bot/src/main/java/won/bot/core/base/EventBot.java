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

package won.bot.core.base;

import com.hp.hpl.jena.rdf.model.Model;
import org.springframework.scheduling.TaskScheduler;
import won.bot.context.BotContext;
import won.bot.core.event.*;
import won.bot.core.eventlistener.EventListenerContext;
import won.bot.events.EventBus;
import won.bot.events.impl.SchedulerEventBusImpl;
import won.bot.needproducer.NeedProducer;
import won.bot.nodeurisource.NodeURISource;
import won.protocol.model.ChatMessage;
import won.protocol.model.Connection;
import won.protocol.model.Match;
import won.protocol.owner.OwnerProtocolNeedServiceClientSide;

import java.net.URI;
import java.util.concurrent.Executor;

/**
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
    eventBus.publish(new NeedCreatedEvent(needUri, wonNodeUri, needModel));
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

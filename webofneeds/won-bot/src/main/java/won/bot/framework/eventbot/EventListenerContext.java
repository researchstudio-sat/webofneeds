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

package won.bot.framework.eventbot;

import org.springframework.scheduling.TaskScheduler;
import won.bot.framework.bot.context.BotContext;
import won.bot.framework.bot.context.BotContextWrapper;
import won.bot.framework.component.needproducer.NeedProducer;
import won.bot.framework.component.nodeurisource.NodeURISource;
import won.bot.framework.eventbot.bus.EventBus;
import won.matcher.component.MatcherNodeURISource;
import won.matcher.protocol.impl.MatcherProtocolMatcherServiceImplJMSBased;
import won.protocol.matcher.MatcherProtocolNeedServiceClientSide;
import won.protocol.message.sender.WonMessageSender;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.linkeddata.LinkedDataSource;

import java.net.URI;
import java.util.concurrent.Executor;

/**
 * Class holding references to all important services that EventListeners inside bots need to
 * access.
 */
public interface EventListenerContext
{
  /**
   * Returns the bot's taskScheduler. Used to schedule actions later without blocking other work.
   * @return
   */
  public TaskScheduler getTaskScheduler();

  public URI getSolrServerURI();
  /**
   * Returns the bot's NodeURISource. Used to obtain WON_BA node URIs.
   * @return
   */
  public NodeURISource getNodeURISource();

  public MatcherNodeURISource getMatcherNodeURISource();
  /**
   * Returns the bot's wonMessageSender.
   */
  public WonMessageSender getWonMessageSender();

  /**
   * Returns the bot's matcher service
   */
  public MatcherProtocolNeedServiceClientSide getMatcherProtocolNeedServiceClient();

  //TODO: change this to an interface
  public MatcherProtocolMatcherServiceImplJMSBased getMatcherProtocolMatcherService();


  /**
   * Returns the bot's needProducer. Used to obtain an RDF model that can be sent to a WON_BA node to create a new need.
   * @return
   */
  public NeedProducer getNeedProducer();


  /**
   * The bot may have a trigger attached that causes ActEvents to be created regularly. This call stops the trigger.
   */
  public void cancelTrigger();

  /**
   * Signals to the framework that the bot's work is done and it may be shut down.
   */
  public void workIsDone();

  /**
   * Returns the bot's event bus. Used to publish events and subscribe for them.
   * @return
   */
  public EventBus getEventBus();

  /**
   * Returns the BotContext of the bot. Used to access the known need URIs.
   * @return
   */
  public BotContext getBotContext();

  /**
   * Returns the BotContextWrapper of the bot. Used to access the known need URISs
   * @return
   */
  public BotContextWrapper getBotContextWrapper();
  /**
   * Returns an executor that passes the tasks to the TaskScheduler for immediate execution.
   * @return
   */
  public Executor getExecutor();


  /**
   * Returns a linked data source.
   * @return
   */
  public LinkedDataSource getLinkedDataSource();

  /**
   * Returns a WonNodeInformationService.
   * @return
   */
  public WonNodeInformationService getWonNodeInformationService();

}

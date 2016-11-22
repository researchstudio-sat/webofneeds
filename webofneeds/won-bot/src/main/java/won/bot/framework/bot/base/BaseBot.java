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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import won.bot.framework.bot.Bot;
import won.bot.framework.bot.BotLifecyclePhase;
import won.bot.framework.bot.context.BotContext;
import won.protocol.message.WonMessage;
import won.protocol.model.Connection;
import won.protocol.model.Match;

import java.net.URI;

/**
 * Basic Bot implementation intended to be extended. Does nothing.
 */
public abstract class BaseBot implements Bot
{
  protected final Logger logger = LoggerFactory.getLogger(getClass());
  private BotLifecyclePhase lifecyclePhase = BotLifecyclePhase.DOWN;
  private boolean workDone = false;

  @Autowired
  private BotContext botContext;

  @Override
  public boolean knowsNeedURI(final URI needURI)
  {
    return this.botContext.isNeedKnown(needURI);
  }

  @Override
  public boolean knowsNodeURI(final URI wonNodeURI) {
    return this.botContext.isNodeKnown(wonNodeURI);
  }

  @Override
  public synchronized void initialize() throws Exception
  {
    if (!this.lifecyclePhase.isDown()) return;
    this.lifecyclePhase = BotLifecyclePhase.STARTING_UP;

    // try the connection with the bot context
    try {
      botContext.saveToObjectMap("temp", "temp", "temp");
      Object o = botContext.loadFromObjectMap("temp", "temp");
      Assert.isTrue(o.equals("temp"));
    } catch (Exception e) {
      logger.error("Bot cannot establish connection with bot context");
      throw e;
    }

    doInitialize();
    this.lifecyclePhase = BotLifecyclePhase.ACTIVE;
  }

  @Override
  public synchronized void shutdown() throws Exception
  {
    if (!this.lifecyclePhase.isActive()) return;
    this.lifecyclePhase = BotLifecyclePhase.SHUTTING_DOWN;
    doShutdown();
    this.lifecyclePhase = BotLifecyclePhase.DOWN;
  }

  /**
   * Override this method to do free resources during shutdown.
   */
  protected abstract void doShutdown();

  /**
   * Override this method to do initialization work.
   */
  protected abstract void doInitialize();

  /**
   * Sets the workDone flag to true.
   */
  protected void workIsDone(){
    this.workDone = true;
  }

  @Override
  public boolean isWorkDone()
  {
    return this.workDone;
  }

  @Override
  public BotLifecyclePhase getLifecyclePhase()
  {
    return this.lifecyclePhase;
  }

  public void setBotContext(final BotContext botContext)
  {
    this.botContext = botContext;
  }

  protected BotContext getBotContext()
  {
    return botContext;
  }

  @Override
  public abstract void onNewNeedCreated(final URI needUri, final URI wonNodeUri, final Model needModel) throws Exception;

  @Override
  public abstract void onConnectFromOtherNeed(Connection con, final WonMessage wonMessage);

  @Override
  public abstract void onOpenFromOtherNeed(Connection con, final WonMessage wonMessage);

  @Override
  public abstract void onCloseFromOtherNeed(Connection con, final WonMessage wonMessage);

  @Override
  public abstract void onHintFromMatcher(Match match, final WonMessage wonMessage);

  @Override
  public abstract void onMessageFromOtherNeed(Connection con, final WonMessage wonMessage);

  @Override
  public abstract void onFailureResponse(final URI failedMessageUri, final WonMessage wonMessage);

  @Override
  public abstract void onSuccessResponse(final URI successfulMessageUri, final WonMessage wonMessage);

  @Override
  public abstract void onMatcherRegistered(final URI wonNodeUri);

  @Override
  public abstract void onNewNeedCreatedNotificationForMatcher(final URI wonNodeURI, final URI needURI, final Dataset needModel);

  @Override
  public abstract void onNeedActivatedNotificationForMatcher(final URI wonNodeURI, final URI needURI);

  @Override
  public abstract void onNeedDeactivatedNotificationForMatcher(final URI wonNodeURI, final URI needURI);

  @Override
  public abstract void act() throws Exception;
}

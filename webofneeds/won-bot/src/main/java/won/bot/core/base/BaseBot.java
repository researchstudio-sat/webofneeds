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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.bot.context.BotContext;
import won.bot.core.Bot;
import won.bot.core.BotLifecyclePhase;
import won.bot.core.BotState;
import won.protocol.model.ChatMessage;
import won.protocol.model.Connection;
import won.protocol.model.Match;

import java.net.URI;

/**
 * Basic Bot implementation intended to be extended. Does nothing.
 */
public class BaseBot implements Bot
{
  protected final Logger logger = LoggerFactory.getLogger(getClass());
  private BotContext botContext;
  private BotLifecyclePhase lifecyclePhase = BotLifecyclePhase.DOWN;
  private BotState state = BotState.IDLE;

  @Override
  public final boolean knowsNeedURI(final URI needURI)
  {
    return this.botContext.isNeedKnown(needURI);
  }

  @Override
  public final synchronized void initialize() throws Exception
  {
    if (!this.lifecyclePhase.isDown()) return;
    this.lifecyclePhase = BotLifecyclePhase.STARTING_UP;
    doInitialize();
    this.lifecyclePhase = BotLifecyclePhase.ACTIVE;
  }

  @Override
  public final synchronized void shutdown() throws Exception
  {
    if (!this.lifecyclePhase.isActive()) return;
    this.lifecyclePhase = BotLifecyclePhase.SHUTTING_DOWN;
    doShutdown();
    this.lifecyclePhase = BotLifecyclePhase.DOWN;
  }

  /**
   * Override this method to do free resources during shutdown.
   */
  protected void doShutdown() {}

  /**
   * Override this method to do initialization work.
   */
  protected void doInitialize() {};

  @Override
  public BotLifecyclePhase getLifecyclePhase()
  {
    return this.lifecyclePhase;
  }

  @Override
  public BotState getState()
  {
    return this.state;
  }

  public final void setBotContext(final BotContext botContext)
  {
    this.botContext = botContext;
  }

  protected final BotContext getBotContext()
  {
    return botContext;
  }

  @Override public void onNewNeedCreated(final URI needUri, final URI wonNodeUri, final Model needModel) throws Exception{}

  @Override public void onConnectFromOtherNeed(Connection con, final Model content) throws Exception {}

  @Override public void onOpenFromOtherNeed(Connection con, final Model content) throws Exception {}

  @Override public void onCloseFromOtherNeed(Connection con, final Model content) throws Exception {}

  @Override public void onHintFromMatcher(Match match, final Model content) throws Exception {}

  @Override public void onMessageFromOtherNeed(Connection con, ChatMessage message, final Model content) throws Exception {}

  @Override public void act() throws Exception
  {}
}

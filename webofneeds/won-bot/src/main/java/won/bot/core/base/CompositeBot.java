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

import won.bot.core.Bot;
import won.protocol.model.ChatMessage;
import won.protocol.model.Connection;
import won.protocol.model.Match;

import java.util.Collections;
import java.util.List;

/**
 * Bot that takes other (delegate) bots when it is created and provides the logic to decide when to delegate to which.
 * Always delegates initialize and shutdown.
 * All event handler methods (on*) are delegated synchronously to all bots in the order they are specified in the delegate list.
 */
public class CompositeBot extends BasicServiceBot
{
  List<BasicServiceBot> delegates= null;


  @Override
  public void onConnectFromOtherNeed(Connection con) throws Exception
  {
    for(Bot delegate: delegates){
      try {
        delegate.onConnectFromOtherNeed(con);
      } catch (Exception e){
        logger.warn("caught exception during onConnectFromOtherNeed({}): ", con.getConnectionURI(), e);
      }
    }
  }

  @Override
  public void onOpenFromOtherNeed(Connection con) throws Exception
  {
    for(Bot delegate: delegates){
      try {
        delegate.onOpenFromOtherNeed(con);
      } catch (Exception e){
        logger.warn("caught exception during onOpenFromOtherNeed({}): ", con.getConnectionURI(), e);
      }
    }
  }

  @Override
  public void onCloseFromOtherNeed(Connection con) throws Exception
  {
    for(Bot delegate: delegates){
      try {
        delegate.onCloseFromOtherNeed(con);
      } catch (Exception e){
        logger.warn("caught exception during onCloseFromOtherNeed({}): ", con.getConnectionURI(), e);
      }
    }
  }

  @Override
  public void onHintFromMatcher(Match match) throws Exception
  {
    for(Bot delegate: delegates){
      try {
        delegate.onHintFromMatcher(match);
      } catch (Exception e){
        logger.warn("caught exception during onHintFromMatcher({}): ", match, e);
      }
    }
  }

  @Override
  public void onMessageFromOtherNeed(Connection con, ChatMessage message) throws Exception
  {
    for(Bot delegate: delegates){
      try {
        delegate.onMessageFromOtherNeed(con, message);
      } catch (Exception e){
        logger.warn("caught exception during onMessageFromOtherNeed({}): ", con.getConnectionURI(), e);
      }
    }
  }

  @Override
  public void act() throws Exception
  {
    super.act();
  }

  @Override
  protected final void doInitialize()
  {
    if (this.delegates != null){
      for (Bot delegate : this.delegates){
        if (delegate.getLifecyclePhase().isDown()) try {
          if (delegate instanceof BaseBot){
            ((BaseBot)delegate).setBotContext(getBotContext());
          }
          delegate.initialize();
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
    doInitializeCustom();
  }

  /**
   * Override this method to do free resources during shutdown. Can't be overriden. If you
   * have custom initialization work to do, override doInitializeCustom.
   */
  protected void doShutdownCustom() {}


  /**
   * Delegates shutdown to all delegate bots. Can't be overriden. If you
   * have custom shutdown work to do, override doShutdownCustom.
   */
  @Override
  protected final void doShutdown()
  {
    if (this.delegates != null){
      for (Bot delegate : this.delegates){
        if (delegate.getLifecyclePhase().isActive()) try {
          delegate.shutdown();
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
    doShutdownCustom();
  }

  /**
   * Override this method to do initialization work.
   */
  protected void doInitializeCustom() {};

  protected List<BasicServiceBot> getDelegates()
  {
    return delegates;
  }

  public void setDelegates(final List<BasicServiceBot> delegates)
  {
    this.delegates = Collections.unmodifiableList(delegates);
  }
}

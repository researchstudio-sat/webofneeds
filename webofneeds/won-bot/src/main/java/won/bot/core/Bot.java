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

package won.bot.core;

import java.net.URI;

/**
 * A bot that manipulates needs.
 *
 * Note: Methods may throw runtime exceptions, which will be handled by the execution framework.
 */
public interface Bot
{
  public boolean knowsNeedURI(URI needURI);
  public void onConnectFromOtherNeed();
  public void onOpenFromOtherNeed();
  public void onCloseFromOtherNeed();
  public void onHintFromMatcher();
  public void onMessageFromOtherNeed();

  /**
   * Init method, called exactly once by the framework before any other method is invoked.
   * The callee must make sure this call is thread-safe, e.g. by explicit synchronizing.
   */
  public void initialize() throws Exception;
  /**
   * Called by the framework to on-reactive tasks.
   * The callee must make sure this call is thread-safe, but explicit synchronization is strongly discouraged.
   */
  public void act() throws Exception;

  /**
   * Shutdown method called exactly once by the framework to allow the bot to free resources.
   * The callee must make sure this call is thread-safe, e.g. by explicit synchronizing.
   */
  public void shutdown() throws Exception;

  /**
   * The lifecycle phase the bot is currently in.
   * @return
   */
  public BotLifecyclePhase getLifecyclePhase();

  /**
   * The state of the bot.
   * @return
   */
  public BotState getState();
}

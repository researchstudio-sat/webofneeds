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

package won.bot.framework.eventbot.action.impl;

import java.util.Random;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.EventBotAction;

/**
 * Action that delegates to another action after a delay that is chosen at random between a specified minimum and
 * maximum.
 */
public class RandomDelayedAction extends DelayedDelegatingAction
{
  private long minDelay;
  private long maxDelay;
  private long intervalLength;
  private long salt;
  private Random random;

  public RandomDelayedAction(final EventListenerContext eventListenerContext, final long minDelay, final long maxDelay, final long salt, final EventBotAction delegate) {
    super(eventListenerContext, delegate);
    this.minDelay = minDelay;
    this.maxDelay = maxDelay;
    this.salt = salt;
    assert minDelay >= 0 : "minDelay must be >= 0";
    assert maxDelay >= 0 : "maxDelay must be >= 0";
    assert minDelay <= maxDelay : "minDelay must be <= maxDelay";
    this.random = new Random(System.currentTimeMillis() + salt);
    this.intervalLength = maxDelay - minDelay;
  }

  @Override
  protected long getDelay() {
    double outcome = random.nextDouble();
    return minDelay + ((long) ((double)intervalLength * outcome));
  }
}

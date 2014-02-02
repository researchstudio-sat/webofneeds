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

package won.bot.framework.events.listener;

import won.bot.framework.events.Event;

/**
 * Executes a task when an event is seen.
 */
public class ExecuteOnEventListener extends BaseEventListener
{
  private Runnable task;
  private int timesRun = 0;
  private int timesToRun = 0;
  private Object monitor = new Object();

  /**
   *
   * @param context
   * @param task
   * @param timesToRun if > 0, listener will unsubscribe from any events after the specified number of executions.
   */
  public ExecuteOnEventListener(final EventListenerContext context, Runnable task, int timesToRun)
  {
    super(context);
    this.task = task;
    this.timesToRun = timesToRun;
  }

  @Override
  public void doOnEvent(final Event event) throws Exception
  {
    synchronized (monitor){
      timesRun++;
      if (timesToRun <= 0){
        getEventListenerContext().getExecutor().execute(task);
      } else if (timesRun < timesToRun) {
        logger.debug("scheduling task, execution no {} ", timesRun);
        getEventListenerContext().getExecutor().execute(task);
      } else if (timesRun == timesToRun) {
        logger.debug("scheduling task, execution no {} (last time)", timesRun);
        getEventListenerContext().getEventBus().unsubscribe(this);
        getEventListenerContext().getExecutor().execute(task);
      }
    }
  }
}

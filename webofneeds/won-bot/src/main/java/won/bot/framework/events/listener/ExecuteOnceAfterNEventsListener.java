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
import won.bot.framework.events.event.NeedCreatedEvent;

/**
 * Counts how often it is called, offers to call a callback when a certain number is reached.
 */
public class ExecuteOnceAfterNEventsListener extends BaseEventListener
{
  private int targetCount;
  private int count = 0;
  private Object monitor = new Object();
  private boolean executed = false;
  private Runnable task;

  public ExecuteOnceAfterNEventsListener(final EventListenerContext context, Runnable task, int count)
  {
    super(context);
    this.targetCount = count;
    this.task = task;
  }

  @Override
  public void doOnEvent(final Event event) throws Exception
  {
    if (executed){
        getEventListenerContext().getEventBus().unsubscribe(NeedCreatedEvent.class,this);
        return;
    }
    synchronized (monitor){
      count++;
      logger.debug("processing event {} of {}", count, targetCount);
      if (!executed && count >= targetCount) {
        logger.debug("scheduling task ");
        getEventListenerContext().getExecutor().execute(task);
        executed = true;
      }
    }
  }

}

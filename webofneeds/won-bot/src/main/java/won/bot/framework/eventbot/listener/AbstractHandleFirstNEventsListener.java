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

package won.bot.framework.eventbot.listener;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.filter.EventFilter;

/**
 * Counts how often it is called, offers to call a callback when a certain number is reached.
 */
public abstract class AbstractHandleFirstNEventsListener extends BaseEventListener implements CountingListener
{
  private int targetCount;
  private int count = 0;
  private Object monitor = new Object();
  private boolean finished = false;

  public AbstractHandleFirstNEventsListener(final EventListenerContext context, int targetCount)
  {
    super(context);
    this.targetCount = targetCount;
  }

  protected AbstractHandleFirstNEventsListener(final EventListenerContext context, final EventFilter eventFilter, final int targetCount)
  {
    super(context, eventFilter);
    this.targetCount = targetCount;
  }

  protected AbstractHandleFirstNEventsListener(final EventListenerContext context, final String name, final int targetCount)
  {
    super(context, name);
    this.targetCount = targetCount;
  }

  protected AbstractHandleFirstNEventsListener(final EventListenerContext context, final String name, final EventFilter eventFilter, final int targetCount)
  {
    super(context, name, eventFilter);
    this.targetCount = targetCount;
  }

  @Override
  public void doOnEvent(final Event event) throws Exception
  {
    boolean doRun = false;
    synchronized (monitor){
      if (finished) {
        return;
      }
      count++;
      if (count <= targetCount) {
        logger.debug("processing event {} of {} (event: {})", new Object[]{count, targetCount, event});
        logger.debug("calling handleFirstNTimes");
        doRun = true;
      }
    }
    if (doRun) {
      handleFirstNTimes(event);
      synchronized (monitor) {
        if (! finished && count >= targetCount) {
          logger.debug("unsubscribing from event");
          unsubscribe();
          publishFinishedEvent();
          finished = true;
        }
      }
    } else {
      //make sure we decrement the count if we didn't get to run - we only want to count the first N events here.
      synchronized (monitor) {
        count--;
      }
    }
  }

  /**
   * Implementations should unsubscribe here.
   */
  protected abstract void unsubscribe();

  /**
   * Implementation handle the event here.
   * @param event
   * @throws Exception
   */
  protected abstract void handleFirstNTimes(final Event event) throws Exception;


  @Override
  public int getTargetCount()
  {
    return targetCount;
  }

  @Override
  public int getCount()
  {
    return count;
  }

  @Override
  public boolean isFinished()
  {
    return finished;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() +
        "{name='" + name +
        ", count=" + count +
        ",targetCount=" + targetCount +
        ", finished=" + finished +
        '}';
  }
}

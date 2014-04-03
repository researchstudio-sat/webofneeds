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

package won.bot.framework.events.listener.action;

import org.javasimon.SimonManager;
import org.javasimon.Split;
import org.javasimon.Stopwatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.bot.framework.events.listener.EventListenerContext;

/**
 *
 */
public abstract class EventBotAction implements Runnable
{
  protected final Logger logger = LoggerFactory.getLogger(getClass());
  private EventListenerContext eventListenerContext;
  private static final String EXCEPTION_TAG = "failed";


  EventBotAction()
  {
  }

  EventBotAction(final EventListenerContext eventListenerContext)
  {
    this.eventListenerContext = eventListenerContext;
  }

  @Override
  public void run()
  {
    Stopwatch stopwatch = SimonManager.getStopwatch(getClass().getName());
    Split split = stopwatch.start();
    try {
      doRun();
      split.stop();
    } catch (Exception e) {
      logger.warn("could not run action {}", getClass().getName(), e);
      split.stop(EXCEPTION_TAG);
    }
  }

  protected EventListenerContext getEventListenerContext()
  {
    return eventListenerContext;
  }

  protected abstract void doRun() throws Exception;


}

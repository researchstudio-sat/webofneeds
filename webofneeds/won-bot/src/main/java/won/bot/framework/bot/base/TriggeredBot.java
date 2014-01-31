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

import org.springframework.scheduling.Trigger;

import java.util.concurrent.ScheduledFuture;

/**
 * Bot base class that expects a trigger to be injected that will cause the act() method to be called according to the trigger's specification.
 */
public class TriggeredBot extends ScheduledActionBot
{
  private Trigger trigger;
  private ScheduledFuture scheduledExecution;

  @Override
  protected final void doInitialize()
  {
    doInitializeCustom();
    this.scheduledExecution = getTaskScheduler().schedule(new Runnable()
    {
      @Override
      public void run()
      {
        try {
          TriggeredBot.this.act();
        } catch (Exception e) {
          logger.warn("caught exception during triggered execution of act()", e);
        }
      }
    }, trigger);
  }

  /**
   * Returns true if the trigger won't cause any more executions (and none are currently running).
   * @return
   */
  protected boolean isTriggerDone(){
    return this.scheduledExecution.isDone();
  }

  /**
   * Override this method to do initialization work.
   */
  protected void doInitializeCustom() {};

  @Override
  protected final void doShutdown()
  {
    this.scheduledExecution.cancel(true);
    doShutdownCustom();
  }

  /**
   * Override this method to do shutdown work.
   */
  protected void doShutdownCustom() {};


  protected void cancelTrigger()
  {
    scheduledExecution.cancel(true);
  }

  protected ScheduledFuture getScheduledExecution()
  {
    return scheduledExecution;
  }

  public void setTrigger(final Trigger trigger)
  {
    this.trigger = trigger;
  }
}

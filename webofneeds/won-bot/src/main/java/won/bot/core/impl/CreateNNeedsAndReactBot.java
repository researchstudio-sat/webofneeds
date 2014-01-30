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

package won.bot.core.impl;

import com.hp.hpl.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

/**
 * Bot that creates needs based on the trigger it gets at creation time.
 * When it creates a need, it is notified. The bot waits until there are two needs,
 * then it deactivates the trigger and calls the callback method onNNeedsCreated().
 */
public abstract class CreateNNeedsAndReactBot extends SchedulableNeedCreator
{
  private final Logger logger = LoggerFactory.getLogger(getClass());
  private int numberOfneeds;
  private int numberOfNeedsCreated = 0;
  private boolean callbackCalled = false;
  private final Object monitor = new Object();

  protected CreateNNeedsAndReactBot(final int numberOfneeds)
  {
    this.numberOfneeds = numberOfneeds;
  }

  @Override
  public void onNewNeedCreated(final URI needUri, final URI wonNodeUri, final Model needModel) throws Exception
  {
    logger.debug("new need created");
    if (callbackCalled) {
      logger.debug("ignoring event as callback was already called.");
      return;
    }
    synchronized (monitor){
      numberOfNeedsCreated++;
      logger.debug("{} of {} needs created", this.numberOfNeedsCreated, this.numberOfneeds);
      if (numberOfneeds > numberOfNeedsCreated) return;
      callbackCalled = true;
      logger.debug("stopping the trigger and executing the callback");
      //stop the trigger
      getScheduledExecution().cancel(true);
      //call the callback
      this.onNNeedsCreated();
    }
  }

  protected abstract void onNNeedsCreated() throws Exception;

}

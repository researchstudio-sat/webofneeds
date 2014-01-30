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

package won.bot.core.eventlistener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.bot.events.EventListener;

/**
 * Base class for event listeners
 */
public abstract class BaseEventListener implements EventListener
{
  protected final Logger logger = LoggerFactory.getLogger(getClass());
  private EventListenerContext context;

  /**
   * Constructor is private so that subclasses must implement the one-arg constructor.
   */
  private BaseEventListener(){}

  protected BaseEventListener(final EventListenerContext context)
  {
    this.context = context;
  }

  protected EventListenerContext getEventListenerContext(){
    return context;
  }
}

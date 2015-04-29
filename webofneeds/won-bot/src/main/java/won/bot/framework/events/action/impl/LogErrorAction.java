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

package won.bot.framework.events.action.impl;

import won.bot.framework.events.EventListenerContext;
import won.bot.framework.events.event.Event;

/**
 * Outputs a message via the configured logging system.
 */
public class LogErrorAction extends LogAction
{

  public LogErrorAction(EventListenerContext eventListenerContext) {
    super(eventListenerContext);
  }

  public LogErrorAction(EventListenerContext eventListenerContext, String message) {
    super(eventListenerContext, message);
  }

  @Override
  protected void doRun(Event event) throws Exception
  {
    logger.error(message);
  }

}

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
import won.bot.framework.events.action.BaseEventBotAction;
import won.bot.framework.events.action.EventBotAction;
import won.bot.framework.events.event.Event;

/**
 * Executes the specified actions.
 */
public class MultipleActions extends BaseEventBotAction
{
  private EventBotAction[] actions;

  public MultipleActions(final EventListenerContext eventListenerContext, final EventBotAction... actions) {
    super(eventListenerContext);
    this.actions = actions;
  }

  @Override
  protected void doRun(final Event event) throws Exception {
    for(int i = 0; i < actions.length; i++) {
      getEventListenerContext().getExecutor().execute(actions[i].getActionTask(event));
    }
  }
}

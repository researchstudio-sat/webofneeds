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

package won.bot.framework.eventbot.action.impl.listener;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.listener.EventListener;

/**
 * Action that unsubscribes the specified listener from all events.
 */
public class UnsubscribeListenerAction extends BaseEventBotAction
{
  private EventListener listener;

  public UnsubscribeListenerAction(final EventListenerContext eventListenerContext, final EventListener listener) {
    super(eventListenerContext);
    this.listener = listener;
  }

  @Override
  protected void doRun(final Event event, EventListener executingListener) throws Exception {
    getEventListenerContext().getEventBus().unsubscribe(listener);
  }
}

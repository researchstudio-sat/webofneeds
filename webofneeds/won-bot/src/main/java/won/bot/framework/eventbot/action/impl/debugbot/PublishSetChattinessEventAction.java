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

package won.bot.framework.eventbot.action.impl.debugbot;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.event.BaseNeedAndConnectionSpecificEvent;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.debugbot.SetChattinessDebugCommandEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.protocol.model.Connection;

/**
 * Action to set the chattiness of the debug bot. Executing the action will
 * publish a SetChattinessDebugCommandEvent.
 */
public class PublishSetChattinessEventAction extends BaseEventBotAction {
  private boolean isChatty;

  public PublishSetChattinessEventAction(final EventListenerContext eventListenerContext, final boolean isChatty) {
    super(eventListenerContext);
    this.isChatty = isChatty;
  }

  @Override
  protected void doRun(final Event event, EventListener executingListener) throws Exception {
    if (event instanceof BaseNeedAndConnectionSpecificEvent) {
      Connection con = ((BaseNeedAndConnectionSpecificEvent) event).getCon();
      getEventListenerContext().getEventBus().publish(new SetChattinessDebugCommandEvent(con, isChatty));
    }
  }
}

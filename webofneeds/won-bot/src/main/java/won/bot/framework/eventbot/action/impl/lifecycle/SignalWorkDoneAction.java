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

package won.bot.framework.eventbot.action.impl.lifecycle;

import won.bot.framework.bot.Bot;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.lifecycle.WorkDoneEvent;
import won.bot.framework.eventbot.listener.EventListener;

/**
 * BaseEventBotAction telling the framework that the bot's work  is done.
 */
public class SignalWorkDoneAction extends BaseEventBotAction {
  private Bot bot;

  public SignalWorkDoneAction(EventListenerContext eventListenerContext, Bot bot) {
    super(eventListenerContext);
    this.bot = bot;
  }

  public SignalWorkDoneAction(final EventListenerContext eventListenerContext) {
    super(eventListenerContext);
  }

  @Override protected void doRun(Event event, EventListener executingListener) throws Exception {
    logger.info("signaling that the bot's work is done");
    getEventListenerContext().workIsDone();
    getEventListenerContext().getEventBus().publish(new WorkDoneEvent(this.bot));
  }
}

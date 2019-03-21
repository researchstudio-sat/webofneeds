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

package won.bot.framework.eventbot.action.impl.wonmessage.execCommand;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.command.MessageCommandFailureEvent;
import won.bot.framework.eventbot.listener.EventListener;

/**
 * Logs type and optional message of a MessageCommandFailureEvent.
 */
public class LogMessageCommandFailureAction extends BaseEventBotAction {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  public LogMessageCommandFailureAction(EventListenerContext eventListenerContext) {
    super(eventListenerContext);
  }

  @Override
  protected void doRun(Event event, EventListener executingListener) throws Exception {
    MessageCommandFailureEvent failureEvent = (MessageCommandFailureEvent) event;
    String command = failureEvent.getOriginalCommandEvent().getClass().getSimpleName();
    String message = failureEvent.getMessage();
    if (message != null) {
      logger.error("Message command {} failed", command);
    } else {
      logger.error("Message command {} failed. Message: ", command, message);
    }
  }
}

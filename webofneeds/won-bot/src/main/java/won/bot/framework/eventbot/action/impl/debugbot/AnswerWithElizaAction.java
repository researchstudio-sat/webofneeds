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

import codeanticode.eliza.Eliza;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.command.SendTextMessageOnConnectionEvent;
import won.bot.framework.eventbot.event.impl.debugbot.MessageToElizaEvent;
import won.protocol.model.Connection;


/**
 * Expects a MessageToElizaEvent, passes the message to a per-connection Eliza instance and sends eliza's response
 * over the connection.
 */
public class AnswerWithElizaAction extends BaseEventBotAction
{
  public static final String KEY_ELIZA_INSTANCES = "elizaInstances";
  private int maxElizaInstances = 20;

  public AnswerWithElizaAction(final EventListenerContext eventListenerContext, final int maxElizaInstances) {
    super(eventListenerContext);
    this.maxElizaInstances = maxElizaInstances;
  }

  @Override
  protected void doRun(final Event event) throws Exception {
    if (event instanceof MessageToElizaEvent) {
      MessageToElizaEvent messageToElizaEvent = (MessageToElizaEvent) event;
      Eliza elizaInstance = getElizaInstanceForConnection(messageToElizaEvent.getCon());
      String elizasResponse = elizaInstance.processInput(((MessageToElizaEvent) event).getMessage());
      getEventListenerContext().getEventBus().publish(
        new SendTextMessageOnConnectionEvent(
          elizasResponse,
          messageToElizaEvent.getCon().getConnectionURI()));
    }
  }

  private Eliza getElizaInstanceForConnection(final Connection con) {

    Eliza elizaInstance = (Eliza) getEventListenerContext().getBotContext().getGeneric(
      KEY_ELIZA_INSTANCES, con.getConnectionURI().toString());

    if (elizaInstance == null) {
      elizaInstance = new Eliza();
      getEventListenerContext().getBotContext().putGeneric(KEY_ELIZA_INSTANCES, con.getConnectionURI().toString(),
                                                           elizaInstance);
    }

    return elizaInstance;
  }

}

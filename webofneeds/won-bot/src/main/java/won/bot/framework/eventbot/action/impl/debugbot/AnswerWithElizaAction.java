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

import org.apache.jena.rdf.model.Model;

import codeanticode.eliza.Eliza;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.command.connectionmessage.ConnectionMessageCommandEvent;
import won.bot.framework.eventbot.event.impl.debugbot.MessageToElizaEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.protocol.util.WonRdfUtils;

/**
 * Expects a MessageToElizaEvent, passes the message to a Eliza instance and
 * sends eliza's response over the connection.
 */
public class AnswerWithElizaAction extends BaseEventBotAction {
  // use only one eliza instance for all connections
  private Eliza eliza = new Eliza();

  public AnswerWithElizaAction(final EventListenerContext eventListenerContext, final int maxElizaInstances) {
    super(eventListenerContext);
  }

  @Override
  protected void doRun(final Event event, EventListener executingListener) throws Exception {
    if (event instanceof MessageToElizaEvent) {
      MessageToElizaEvent messageToElizaEvent = (MessageToElizaEvent) event;

      String elizaResponse;
      synchronized (eliza) {
        elizaResponse = eliza.processInput(((MessageToElizaEvent) event).getMessage());
      }

      Model messageModel = WonRdfUtils.MessageUtils.textMessage(elizaResponse);
      getEventListenerContext().getEventBus()
          .publish(new ConnectionMessageCommandEvent(messageToElizaEvent.getCon(), messageModel));
    }
  }

}

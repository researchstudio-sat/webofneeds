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

package won.bot.framework.events.action.impl.debugbot;

import won.bot.framework.events.EventListenerContext;
import won.bot.framework.events.action.BaseEventBotAction;
import won.bot.framework.events.action.BotActionUtils;
import won.bot.framework.events.event.Event;
import won.protocol.message.WonMessage;

import java.net.URI;
import java.util.*;

/**
 * Action to perform when the debug bot is set to be 'chatty' - that is,
 * sends messages via its connections spontaneously.
 */
public class SendChattyMessageAction extends BaseEventBotAction
{
  private double probabilityOfSendingMessage = 0.1;
  private String[] messagesForShortInactivity;
  public static final String KEY_CHATTY_CONNECTIONS = "chattyConnections";
  private Random random;
  MessageTimingManager messageTimingManager;
  private String[] messagesForLongInactivity;

  public SendChattyMessageAction(final EventListenerContext eventListenerContext, final double
    probabilityOfSendingMessage, MessageTimingManager messageTimingManager, final String[]
    messagesForShortInactivity, final String[] messagesForLongInactivity) {
    super(eventListenerContext);
    this.probabilityOfSendingMessage = probabilityOfSendingMessage;
    this.messagesForShortInactivity = messagesForShortInactivity;
    this.random = new Random(System.currentTimeMillis());
    this.messageTimingManager = messageTimingManager;
    this.messagesForLongInactivity = messagesForLongInactivity;
  }

  @Override
  protected void doRun(final Event event) throws Exception {
    Set<URI> toRemove = null;
    Set<URI> chattyConnections = (Set<URI>) getEventListenerContext().getBotContext().get(KEY_CHATTY_CONNECTIONS);
    if (chattyConnections == null) return;
    theloop:
    for (URI con : chattyConnections) {
      if (random.nextDouble() > probabilityOfSendingMessage) {
        continue;
      }

      //don't send a chatty message when we just sent one
      if (messageTimingManager.getInactivityPeriodOfSelf(con) == MessageTimingManager.InactivityPeriod.ACTIVE) {
        continue;
      }

      //determine which kind of message to send depending on inactivity of partner.
      MessageTimingManager.InactivityPeriod inactivityPeriod = messageTimingManager
        .getInactivityPeriodOfPartner(con);
      String message = null;
      switch (inactivityPeriod) {
        case ACTIVE:
          //do not send a message
          continue theloop;
        case SHORT:
          message = getRandomMessage(this.messagesForShortInactivity);
          break;
        case LONG:
          message = getRandomMessage(this.messagesForLongInactivity);
          break;
        case TOO_LONG:
          if (toRemove == null) toRemove = new HashSet<URI>();
          toRemove.add(con);
          message = "Ok, you've been absent for a while now. I will stop bugging you. If you want me to resume " +
            "doing that, say 'chatty on'";
          break;
      }
      WonMessage wonMessage = BotActionUtils.createWonMessage(getEventListenerContext(), con, message);
      getEventListenerContext().getWonMessageSender().sendWonMessage(wonMessage);

    }
    if (toRemove != null) chattyConnections.removeAll(toRemove);
  }

  private String getRandomMessage(String[] fromMessages){
    return fromMessages[random.nextInt(fromMessages.length)];
  }
}

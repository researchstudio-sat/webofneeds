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
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.command.SendTextMessageOnConnectionEvent;

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
    Collection<Object> chattyConnections = getEventListenerContext().getBotContext().loadObjectMap(
      KEY_CHATTY_CONNECTIONS).values();

    if (chattyConnections == null) return;
    theloop:
    for (Object o : chattyConnections) {
      URI con = (URI) o;
      if (random.nextDouble() > probabilityOfSendingMessage) {
        continue;
      }
      //determine which kind of message to send depending on inactivity of partner.
      MessageTimingManager.InactivityPeriod inactivityPeriod = messageTimingManager
        .getInactivityPeriodOfPartner(con);

      //don't send a chatty message when we just sent one
      if (!this.messageTimingManager.isWaitedLongEnough(con)) {
        continue;
      }


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
            "doing that, say 'chatty on'. For more information, say 'usage'";
          break;
      }
      //publish an event that causes the message to be sent
      getEventListenerContext().getEventBus().publish(new SendTextMessageOnConnectionEvent(message, con));
    }
    if (toRemove != null) {
      for (URI uri : toRemove) {
        getEventListenerContext().getBotContext().removeFromObjectMap(KEY_CHATTY_CONNECTIONS, uri.toString());
      }
    }
  }

  private String getRandomMessage(String[] fromMessages){
    return fromMessages[random.nextInt(fromMessages.length)];
  }
}

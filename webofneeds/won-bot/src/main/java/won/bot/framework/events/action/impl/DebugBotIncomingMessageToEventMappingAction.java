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
import won.bot.framework.events.event.BaseNeedAndConnectionSpecificEvent;
import won.bot.framework.events.event.ConnectionSpecificEvent;
import won.bot.framework.events.event.Event;
import won.bot.framework.events.event.MessageEvent;
import won.bot.framework.events.event.impl.debugbot.*;
import won.protocol.message.WonMessage;
import won.protocol.model.Connection;
import won.protocol.util.WonRdfUtils;

import java.util.regex.Pattern;

/**
 * Listener that reacts to incoming messages, creating internal bot events for them
 */
public class DebugBotIncomingMessageToEventMappingAction extends BaseEventBotAction
{

  Pattern PATTERN_USAGE =Pattern.compile("^usage$", Pattern.CASE_INSENSITIVE);
  Pattern PATTERN_HINT =Pattern.compile("^hint$", Pattern.CASE_INSENSITIVE);
  Pattern PATTERN_CLOSE =Pattern.compile("^close$", Pattern.CASE_INSENSITIVE);
  Pattern PATTERN_CONNECT =Pattern.compile("^connect$", Pattern.CASE_INSENSITIVE);
  Pattern PATTERN_DEACTIVATE =Pattern.compile("^deactivate$", Pattern.CASE_INSENSITIVE);


  public static final String USAGE_MESSAGE =
    "You are connected to the debug bot. You can issue commands that will cause interactions with your need.\n" +
    "Usage: \n " +
    "    'hint':        create a new need and send hint to it;\n" +
    "    'connect':     create a new need and send connection request to it;\n" +
    "    'close':       close the current connection;\n" +
    "    'deactivate':  deactivate remote need of the current connection;\n" +
    "    'usage':       display this message.\n";

  public DebugBotIncomingMessageToEventMappingAction(EventListenerContext eventListenerContext) {
    super(eventListenerContext);
  }

  @Override
  protected void doRun(final Event event) throws Exception
  {
    if (event instanceof BaseNeedAndConnectionSpecificEvent){
      handleTextMessageEvent((ConnectionSpecificEvent) event);
    }
  }

  private void handleTextMessageEvent(final ConnectionSpecificEvent messageEvent){

      if (messageEvent instanceof  MessageEvent) {
        Connection con = ((BaseNeedAndConnectionSpecificEvent) messageEvent).getCon();
        WonMessage msg = ((MessageEvent) messageEvent).getWonMessage();
        String message = extractTextMessageFromWonMessage(msg);
        if (PATTERN_USAGE.matcher(message).matches()){
          getEventListenerContext().getEventBus().publish(new UsageDebugCommandEvent(con));
        }
        else if (PATTERN_HINT.matcher(message).matches()){
          getEventListenerContext().getEventBus().publish(new HintDebugCommandEvent(con));
        }
        else if (PATTERN_CONNECT.matcher(message).matches()){
          getEventListenerContext().getEventBus().publish(new ConnectDebugCommandEvent(con));
        }
        else if (PATTERN_CLOSE.matcher(message).matches()){
          getEventListenerContext().getEventBus().publish(new CloseDebugCommandEvent(con));
        }
        else if (PATTERN_DEACTIVATE.matcher(message).matches()){
          getEventListenerContext().getEventBus().publish(new DeactivateDebugCommandEvent(con));
        }
        //todo .. more

      }
  }

  private String extractTextMessageFromWonMessage(WonMessage wonMessage){
    if (wonMessage == null) return null;
    return WonRdfUtils.MessageUtils.getTextMessage(wonMessage);
  }


}

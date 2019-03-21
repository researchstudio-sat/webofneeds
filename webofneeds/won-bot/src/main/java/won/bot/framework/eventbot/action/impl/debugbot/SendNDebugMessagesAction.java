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

import org.apache.jena.query.Dataset;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.debugbot.SendNDebugCommandEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.protocol.exception.WonMessageBuilderException;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.WonRdfUtils;

import java.net.URI;
import java.util.Date;

/**
 * Created by fkleedorfer on 09.06.2016.
 */
public class SendNDebugMessagesAction extends BaseEventBotAction {
  String[] messages = { "one", "two" };
  private long delayBetweenMessages = 1000;

  public SendNDebugMessagesAction(final EventListenerContext eventListenerContext, long delayBetweenMessages,
      String... messages) {
    super(eventListenerContext);
    this.delayBetweenMessages = delayBetweenMessages;
    this.messages = messages;
  }

  @Override
  protected void doRun(final Event event, EventListener executingListener) throws Exception {
    int n = this.messages.length;
    if (event instanceof SendNDebugCommandEvent) {
      SendNDebugCommandEvent sendNDebugCommandEvent = (SendNDebugCommandEvent) event;
      n = Math.min(n, ((SendNDebugCommandEvent) event).getNumberOfMessagesToSend());
      long delay = 0;
      URI connUri = sendNDebugCommandEvent.getConnectionURI();
      for (int i = 0; i < n; i++) {
        delay += delayBetweenMessages;
        String messageText = this.messages[i];
        getEventListenerContext().getTaskScheduler().schedule(createMessageTask(connUri, messageText),
            new Date(System.currentTimeMillis() + delay));
      }
    }
  }

  private Runnable createMessageTask(final URI connectionURI, final String messageText) {
    return new Runnable() {
      @Override
      public void run() {
        getEventListenerContext().getWonMessageSender().sendWonMessage(createWonMessage(connectionURI, messageText));
      }
    };
  }

  private WonMessage createWonMessage(URI connectionURI, String message) throws WonMessageBuilderException {

    WonNodeInformationService wonNodeInformationService = getEventListenerContext().getWonNodeInformationService();

    Dataset connectionRDF = getEventListenerContext().getLinkedDataSource().getDataForResource(connectionURI);
    URI remoteNeed = WonRdfUtils.ConnectionUtils.getRemoteNeedURIFromConnection(connectionRDF, connectionURI);
    URI localNeed = WonRdfUtils.ConnectionUtils.getLocalNeedURIFromConnection(connectionRDF, connectionURI);
    URI wonNode = WonRdfUtils.ConnectionUtils.getWonNodeURIFromConnection(connectionRDF, connectionURI);
    Dataset remoteNeedRDF = getEventListenerContext().getLinkedDataSource().getDataForResource(remoteNeed);

    URI messageURI = wonNodeInformationService.generateEventURI(wonNode);

    return WonMessageBuilder.setMessagePropertiesForConnectionMessage(messageURI, connectionURI, localNeed, wonNode,
        WonRdfUtils.ConnectionUtils.getRemoteConnectionURIFromConnection(connectionRDF, connectionURI), remoteNeed,
        WonRdfUtils.NeedUtils.getWonNodeURIFromNeed(remoteNeedRDF, remoteNeed), message).build();
  }
}

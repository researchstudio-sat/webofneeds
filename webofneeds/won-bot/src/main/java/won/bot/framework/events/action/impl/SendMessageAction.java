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

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;
import won.bot.framework.events.EventListenerContext;
import won.bot.framework.events.action.BaseEventBotAction;
import won.bot.framework.events.event.ConnectionSpecificEvent;
import won.bot.framework.events.event.Event;
import won.protocol.exception.WonMessageBuilderException;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.RdfUtils;
import won.protocol.util.WonRdfUtils;

import java.net.URI;

/**
 * Action that sends a generic message.
 */
public class SendMessageAction extends BaseEventBotAction
{

  public SendMessageAction(final EventListenerContext eventListenerContext) {
    super(eventListenerContext);
  }

  @Override
  protected void doRun(final Event event) throws Exception {
    if (event instanceof ConnectionSpecificEvent) {
      sendMessage((ConnectionSpecificEvent) event);
    }
  }

  /**
   * The default implementation creates a generic text message.
   *
   * @return
   */
  protected Model getMessageContent() {
    return WonRdfUtils.MessageUtils.textMessage(createTextMessage());
  }

  protected String createTextMessage() {
    return "hello world";
  }

  private void sendMessage(final ConnectionSpecificEvent messageEvent) {
    URI connectionUri = messageEvent.getConnectionURI();
    logger.debug("sending message ");
    try {
      getEventListenerContext().getOwnerService().sendConnectionMessage(
        connectionUri, getMessageContent(), createWonMessage(connectionUri, getMessageContent()));
    } catch (Exception e) {
      logger.warn("could not send message via connection {}", connectionUri, e);
    }
  }

  private WonMessage createWonMessage(URI connectionURI, Model content) throws WonMessageBuilderException {

    WonNodeInformationService wonNodeInformationService =
      getEventListenerContext().getWonNodeInformationService();

    Dataset connectionRDF =
      getEventListenerContext().getLinkedDataSource().getDataForResource(connectionURI);
    URI remoteNeed = WonRdfUtils.NeedUtils.getRemoteNeedURIFromConnection(connectionRDF, connectionURI);
    URI localNeed = WonRdfUtils.NeedUtils.getLocalNeedURIFromConnection(connectionRDF, connectionURI);
    URI wonNode = WonRdfUtils.NeedUtils.getWonNodeURIFromConnection(connectionRDF, connectionURI);
    Dataset remoteNeedRDF =
      getEventListenerContext().getLinkedDataSource().getDataForResource(remoteNeed);

    URI messageURI = wonNodeInformationService.generateEventURI(wonNode);
    RdfUtils.replaceBaseURI(content, messageURI.toString());

    WonMessageBuilder builder = new WonMessageBuilder();
    return builder
      .setMessagePropertiesForConnectionMessage(
        messageURI,
        connectionURI,
        localNeed,
        wonNode,
        WonRdfUtils.NeedUtils.getRemoteConnectionURIFromConnection(connectionRDF, connectionURI),
        remoteNeed,
        WonRdfUtils.NeedUtils.getWonNodeURIFromNeed(remoteNeedRDF, remoteNeed),
        content)
      .build();
  }

}

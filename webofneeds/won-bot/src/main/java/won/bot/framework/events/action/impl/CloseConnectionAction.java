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
import won.bot.framework.events.EventListenerContext;
import won.bot.framework.events.action.BaseEventBotAction;
import won.bot.framework.events.event.ConnectionSpecificEvent;
import won.bot.framework.events.event.Event;
import won.protocol.exception.WonMessageBuilderException;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.WonRdfUtils;

import java.net.URI;

/**
 * Listener that will try to obtain a connectionURI from any event
 * passed to it and close that connection.
 */
public class CloseConnectionAction extends BaseEventBotAction
{
  public CloseConnectionAction(final EventListenerContext context)
  {
    super(context);
  }

  @Override
  protected void doRun(final Event event) throws Exception {
    logger.debug("trying to close connection related to event {}", event);
    try {
      URI connectionURI = null;
      if (event instanceof ConnectionSpecificEvent){
        connectionURI = ((ConnectionSpecificEvent)event).getConnectionURI();
      }
      logger.debug("Extracted connection uri {}", connectionURI);
      if (connectionURI != null) {
        logger.debug("closing connection {}", connectionURI);

        getEventListenerContext().getOwnerService().close(connectionURI, null, createWonMessage(connectionURI));
      }
    } catch (Exception e){
      logger.warn("error trying to close connection", e);
    }
  }

  private WonMessage createWonMessage(URI connectionURI) throws WonMessageBuilderException {

    WonNodeInformationService wonNodeInformationService =
      getEventListenerContext().getWonNodeInformationService();

    Dataset connectionRDF =
      getEventListenerContext().getLinkedDataSource().getDataForResource(connectionURI);
    URI remoteNeed = WonRdfUtils.NeedUtils.getRemoteNeedURIFromConnection(connectionRDF, connectionURI);
    URI localNeed = WonRdfUtils.NeedUtils.getLocalNeedURIFromConnection(connectionRDF, connectionURI);
    URI wonNode = WonRdfUtils.NeedUtils.getWonNodeURIFromConnection(connectionRDF, connectionURI);
    Dataset remoteNeedRDF =
      getEventListenerContext().getLinkedDataSource().getDataForResource(remoteNeed);

    WonMessageBuilder builder = new WonMessageBuilder();
    return builder
      .setMessagePropertiesForClose(
        wonNodeInformationService.generateMessageEventURI(
          localNeed, wonNode),
        connectionURI,
        localNeed,
        wonNode,
        WonRdfUtils.NeedUtils.getRemoteConnectionURIFromConnection(connectionRDF, connectionURI),
        remoteNeed,
        WonRdfUtils.NeedUtils.getWonNodeURIFromNeed(remoteNeedRDF, remoteNeed)
      )
      .build();
  }
}

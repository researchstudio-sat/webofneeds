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

package won.bot.framework.eventbot.action.impl.wonmessage;

import org.apache.jena.query.Dataset;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.event.ConnectionSpecificEvent;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.wonmessage.ConnectFromOtherNeedEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.HintFromMatcherEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.OpenFromOtherNeedEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.protocol.exception.WonMessageBuilderException;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.model.ConnectionState;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.WonRdfUtils;
import won.protocol.util.linkeddata.WonLinkedDataUtils;

import java.net.URI;
import java.util.Optional;

/**
 * User: fkleedorfer
 * Date: 30.01.14
 */
public class OpenConnectionAction extends BaseEventBotAction {

  private String welcomeMessage;

  public OpenConnectionAction(final EventListenerContext context, final String welcomeMessage) {
    super(context);
    this.welcomeMessage = welcomeMessage;
  }

  @Override public void doRun(final Event event, EventListener executingListener) throws Exception {
    if (event instanceof ConnectFromOtherNeedEvent) {
      ConnectionSpecificEvent connectEvent = (ConnectionSpecificEvent) event;
      logger.debug("auto-replying to connect for connection {}", connectEvent.getConnectionURI());
      getEventListenerContext().getWonMessageSender()
          .sendWonMessage(createOpenWonMessage(connectEvent.getConnectionURI()));
      return;
    } else if (event instanceof OpenFromOtherNeedEvent) {
      ConnectionSpecificEvent connectEvent = (ConnectionSpecificEvent) event;

      URI connectionState = WonLinkedDataUtils.getConnectionStateforConnectionURI(connectEvent.getConnectionURI(),
          getEventListenerContext().getLinkedDataSource());
      if (ConnectionState.REQUEST_RECEIVED.getURI().equals(connectionState)) {
        logger.debug("auto-replying to open(REQUEST_RECEIVED) with open for connection {}",
            connectEvent.getConnectionURI());
        getEventListenerContext().getWonMessageSender()
            .sendWonMessage(createOpenWonMessage(connectEvent.getConnectionURI()));
      } else {
        // else do not respond - we assume the connection is now established.
      }
      return;
    } else if (event instanceof HintFromMatcherEvent) {
      //TODO: the hint with a match object is not really suitable here. Would be better to
      // use connection object instead
      HintFromMatcherEvent hintEvent = (HintFromMatcherEvent) event;
      logger.debug("opening connection based on hint {}", event);
      getEventListenerContext().getWonMessageSender().sendWonMessage(
          createConnectWonMessage(hintEvent.getMatch().getFromNeed(), hintEvent.getMatch().getToNeed(),
              Optional.empty(), Optional.empty()));
    }
  }

  private WonMessage createOpenWonMessage(URI connectionURI) throws WonMessageBuilderException {

    WonNodeInformationService wonNodeInformationService = getEventListenerContext().getWonNodeInformationService();

    Dataset connectionRDF = getEventListenerContext().getLinkedDataSource().getDataForResource(connectionURI);
    URI remoteNeed = WonRdfUtils.ConnectionUtils.getRemoteNeedURIFromConnection(connectionRDF, connectionURI);
    URI localNeed = WonRdfUtils.ConnectionUtils.getLocalNeedURIFromConnection(connectionRDF, connectionURI);
    URI wonNode = WonRdfUtils.ConnectionUtils.getWonNodeURIFromConnection(connectionRDF, connectionURI);
    Dataset remoteNeedRDF = getEventListenerContext().getLinkedDataSource().getDataForResource(remoteNeed);

    return WonMessageBuilder
        .setMessagePropertiesForOpen(wonNodeInformationService.generateEventURI(wonNode), connectionURI, localNeed,
            wonNode, WonRdfUtils.ConnectionUtils.getRemoteConnectionURIFromConnection(connectionRDF, connectionURI),
            remoteNeed, WonRdfUtils.NeedUtils.getWonNodeURIFromNeed(remoteNeedRDF, remoteNeed), welcomeMessage).build();
  }

  private WonMessage createConnectWonMessage(URI fromUri, URI toUri, Optional<URI> localFacet,
      Optional<URI> remoteFacet) throws WonMessageBuilderException {

    WonNodeInformationService wonNodeInformationService = getEventListenerContext().getWonNodeInformationService();

    Dataset localNeedRDF = getEventListenerContext().getLinkedDataSource().getDataForResource(fromUri);
    Dataset remoteNeedRDF = getEventListenerContext().getLinkedDataSource().getDataForResource(toUri);

    URI localWonNode = WonRdfUtils.NeedUtils.getWonNodeURIFromNeed(localNeedRDF, fromUri);
    URI remoteWonNode = WonRdfUtils.NeedUtils.getWonNodeURIFromNeed(remoteNeedRDF, toUri);

    return WonMessageBuilder
        .setMessagePropertiesForConnect(wonNodeInformationService.generateEventURI(localWonNode), localFacet, fromUri,
            localWonNode, remoteFacet, toUri, remoteWonNode, welcomeMessage).build();
  }
}

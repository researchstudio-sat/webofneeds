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
import won.bot.framework.events.event.impl.ConnectFromOtherNeedEvent;
import won.bot.framework.events.event.impl.HintFromMatcherEvent;
import won.bot.framework.events.event.impl.OpenFromOtherNeedEvent;
import won.protocol.exception.WonMessageBuilderException;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.model.ConnectionState;
import won.protocol.model.FacetType;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.WonRdfUtils;

import java.net.URI;

/**
 * User: fkleedorfer
 * Date: 30.01.14
 */
public class OpenConnectionAction extends BaseEventBotAction
{

  public OpenConnectionAction(final EventListenerContext context)
  {
    super(context);
  }

  @Override
  public void doRun(final Event event) throws Exception {
    if (event instanceof ConnectFromOtherNeedEvent) {
      ConnectionSpecificEvent connectEvent = (ConnectionSpecificEvent) event;
      logger.debug("auto-replying to connect for connection {}", connectEvent.getConnectionURI() );
      getEventListenerContext().getOwnerService().sendWonMessage(createOpenWonMessage(connectEvent.getConnectionURI()));
      return;
    } else if (event instanceof OpenFromOtherNeedEvent){
      ConnectionSpecificEvent connectEvent = (ConnectionSpecificEvent) event;
      if (((OpenFromOtherNeedEvent) event).getCon().getState() == ConnectionState.REQUEST_RECEIVED) {
        logger.debug("auto-replying to open(REQUEST_RECEIVED) with open for connection {}",
          connectEvent.getConnectionURI());
        getEventListenerContext().getOwnerService().sendWonMessage(createOpenWonMessage(connectEvent.getConnectionURI()));
      }
      return;
    } else if (event instanceof HintFromMatcherEvent) {
      //TODO: the hint with a match object is not really suitable here. Would be better to
      // use connection object instead
      HintFromMatcherEvent hintEvent = (HintFromMatcherEvent) event;
      logger.debug("opening connection based on hint {}", event);
      getEventListenerContext().getOwnerService().sendWonMessage(
        createConnectWonMessage(
          hintEvent.getMatch().getFromNeed(), hintEvent.getMatch().getToNeed(),
          FacetType.OwnerFacet.getURI(), FacetType.OwnerFacet.getURI()));
    }
  }

  private WonMessage createOpenWonMessage(URI connectionURI) throws WonMessageBuilderException {

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
      .setMessagePropertiesForOpen(
        wonNodeInformationService.generateEventURI(
          wonNode),
        connectionURI,
        localNeed,
        wonNode,
        WonRdfUtils.NeedUtils.getRemoteConnectionURIFromConnection(connectionRDF, connectionURI),
        remoteNeed,
        WonRdfUtils.NeedUtils.getWonNodeURIFromNeed(remoteNeedRDF, remoteNeed)
      )
      .build();
  }

  private WonMessage createConnectWonMessage(URI fromUri, URI toUri, URI localFacet, URI remoteFacet)
    throws WonMessageBuilderException {

    WonNodeInformationService wonNodeInformationService =
      getEventListenerContext().getWonNodeInformationService();

    Dataset localNeedRDF =
      getEventListenerContext().getLinkedDataSource().getDataForResource(fromUri);
    Dataset remoteNeedRDF =
      getEventListenerContext().getLinkedDataSource().getDataForResource(toUri);

    URI localWonNode = WonRdfUtils.NeedUtils.getWonNodeURIFromNeed(localNeedRDF, fromUri);
    URI remoteWonNode = WonRdfUtils.NeedUtils.getWonNodeURIFromNeed(remoteNeedRDF, toUri);

    WonMessageBuilder builder = new WonMessageBuilder();
    return  builder
      .setMessagePropertiesForConnect(
        wonNodeInformationService.generateEventURI(
          localWonNode),
        localFacet,
        fromUri,
        localWonNode,
        remoteFacet,
        toUri,
        remoteWonNode)
      .build();
  }
}

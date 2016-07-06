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

import com.hp.hpl.jena.query.Dataset;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.EventListenerContext;
import won.protocol.exception.WonMessageBuilderException;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.WonRdfUtils;

import java.net.URI;
import java.util.List;

/**
 * BaseEventBotAction connecting two needs on the specified facets. The need's URIs are obtained from
 * the bot context. The first two URIs found there are used.
 */
public class ConnectTwoNeedsAction extends BaseEventBotAction
{
  private URI remoteFacet;
  private URI localFacet;
  private String welcomeMessage;

  public ConnectTwoNeedsAction(final EventListenerContext eventListenerContext, final URI remoteFacet, final URI
    localFacet, final String welcomeMessage)
  {
    super(eventListenerContext);
    this.remoteFacet = remoteFacet;
    this.localFacet = localFacet;
    this.welcomeMessage = welcomeMessage;
  }

  @Override
  public void doRun(Event event)
  {
    List<URI> needs = getEventListenerContext().getBotContext().listNeedUris();
    try {
      getEventListenerContext().getWonMessageSender().sendWonMessage(
        createWonMessage(needs.get(0), needs.get(1)));
    } catch (Exception e) {
      logger.warn("could not connect {} and {}", new Object[]{needs.get(0), needs.get(1)}, e);
    }
  }

  private WonMessage createWonMessage(URI fromUri, URI toUri)
    throws WonMessageBuilderException {

    WonNodeInformationService wonNodeInformationService =
      getEventListenerContext().getWonNodeInformationService();

    Dataset localNeedRDF =
      getEventListenerContext().getLinkedDataSource().getDataForResource(fromUri);
    Dataset remoteNeedRDF =
      getEventListenerContext().getLinkedDataSource().getDataForResource(toUri);

    URI localWonNode = WonRdfUtils.NeedUtils.getWonNodeURIFromNeed(localNeedRDF, fromUri);
    URI remoteWonNode = WonRdfUtils.NeedUtils.getWonNodeURIFromNeed(remoteNeedRDF, toUri);



    return
      WonMessageBuilder.setMessagePropertiesForConnect(
        wonNodeInformationService.generateEventURI(
          localWonNode),
        localFacet,
        fromUri,
        localWonNode,
        remoteFacet,
        toUri,
        remoteWonNode,
        welcomeMessage)
      .build();
  }

}

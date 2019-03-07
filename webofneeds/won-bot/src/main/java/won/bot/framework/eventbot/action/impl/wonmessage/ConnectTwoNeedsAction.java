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

import java.net.URI;
import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;

import org.apache.jena.query.Dataset;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.listener.EventListener;
import won.protocol.exception.WonMessageBuilderException;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.WonRdfUtils;
import won.protocol.util.linkeddata.WonLinkedDataUtils;

/**
 * BaseEventBotAction connecting two needs on the specified facets. The need's URIs are obtained from
 * the bot context. The first two URIs found there are used.
 */
public class ConnectTwoNeedsAction extends BaseEventBotAction
{
  private Optional<URI> remoteFacetType = Optional.empty();
  private Optional<URI> localFacetType = Optional.empty();
  private String welcomeMessage;

  public ConnectTwoNeedsAction(final EventListenerContext eventListenerContext, final URI remoteFacetType, final URI
    localFacetType, final String welcomeMessage)
  {
    super(eventListenerContext);
    this.remoteFacetType = Optional.of(remoteFacetType);
    this.localFacetType = Optional.of(localFacetType);
    this.welcomeMessage = welcomeMessage;
  }

  @Override
  public void doRun(Event event, EventListener executingListener)
  {
    Collection<URI> needs = getEventListenerContext().getBotContext().retrieveAllNeedUris();
    try {
      Iterator iter = needs.iterator();
      getEventListenerContext().getWonMessageSender().sendWonMessage(
        createWonMessage((URI) iter.next(), (URI) iter.next()));
    } catch (Exception e) {
      logger.warn("could not connect two need objects, exception was: ", e);
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
        localFacetType.map(facetType -> WonLinkedDataUtils.getFacetsOfType(fromUri, facetType, getEventListenerContext().getLinkedDataSource()).stream().findFirst().orElse(null)),
        fromUri,
        localWonNode,
        remoteFacetType.map(facetType -> WonLinkedDataUtils.getFacetsOfType(toUri, facetType, getEventListenerContext().getLinkedDataSource()).stream().findFirst().orElse(null)),
        toUri,
        remoteWonNode,
        welcomeMessage)
      .build();
  }

}

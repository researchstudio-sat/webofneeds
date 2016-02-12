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
import won.bot.framework.events.event.Event;
import won.bot.framework.events.event.NeedSpecificEvent;
import won.protocol.exception.WonMessageBuilderException;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.WonRdfUtils;

import java.net.URI;
import java.util.List;

/**
 * BaseEventBotAction connecting two needs on the specified facets.
 * Requires a NeedSpecificEvent to run and expeects the needURI from the event
 * to be associated with another need URI via the botContext.put method.
 */
public class ConnectWithAssociatedNeedAction extends BaseEventBotAction
{
  private URI remoteFacet;
  private URI localFacet;

  public ConnectWithAssociatedNeedAction(final EventListenerContext eventListenerContext, final URI remoteFacet, final URI localFacet)
  {
    super(eventListenerContext);
    this.remoteFacet = remoteFacet;
    this.localFacet = localFacet;
  }

  @Override
  public void doRun(Event event)
  {
    if (! (event instanceof NeedSpecificEvent)){
      logger.error("ConnectWithAssociatedNeedAction can only handle NeedSpecificEvents");
      return;
    }
    final URI myNeedUri = ((NeedSpecificEvent) event).getNeedURI();
    final URI remoteNeedUri = (URI) getEventListenerContext().getBotContext().get(myNeedUri);
    try {
      getEventListenerContext().getWonMessageSender().sendWonMessage(
        createWonMessage(myNeedUri
                , remoteNeedUri));
    } catch (Exception e) {
      logger.warn("could not connect {} and {}", new Object[]{myNeedUri, remoteNeedUri}, e);
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

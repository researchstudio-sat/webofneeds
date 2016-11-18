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
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.action.impl.needlifecycle.AbstractCreateNeedAction;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.NeedSpecificEvent;
import won.protocol.exception.WonMessageBuilderException;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.WonRdfUtils;

import java.net.URI;

/**
 * BaseEventBotAction connecting two needs on the specified facets.
 * Requires a NeedSpecificEvent to run and expeects the needURI from the event
 * to be associated with another need URI via the botContext.saveToObjectMap method.
 */
public class ConnectWithAssociatedNeedAction extends BaseEventBotAction
{
  private URI remoteFacet;
  private URI localFacet;
  private String welcomeMessage;

  public ConnectWithAssociatedNeedAction(final EventListenerContext eventListenerContext, final URI remoteFacet,
                                         final URI localFacet, String welcomeMessage)
  {
    super(eventListenerContext);
    this.remoteFacet = remoteFacet;
    this.localFacet = localFacet;
    this.welcomeMessage = welcomeMessage;
  }

  @Override
  public void doRun(Event event)
  {
    if (! (event instanceof NeedSpecificEvent)){
      logger.error("ConnectWithAssociatedNeedAction can only handle NeedSpecificEvents");
      return;
    }
    final URI myNeedUri = ((NeedSpecificEvent) event).getNeedURI();
    final URI remoteNeedUri = (URI) getEventListenerContext().getBotContext().loadFromObjectMap(
      AbstractCreateNeedAction.KEY_NEED_REMOTE_NEED_ASSOCIATION, myNeedUri.toString());
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


    return WonMessageBuilder
      .setMessagePropertiesForConnect(
        wonNodeInformationService.generateEventURI(
          localWonNode),
        localFacet,
        fromUri,
        localWonNode,
        remoteFacet,
        toUri,
        remoteWonNode, welcomeMessage)
      .build();
  }

}

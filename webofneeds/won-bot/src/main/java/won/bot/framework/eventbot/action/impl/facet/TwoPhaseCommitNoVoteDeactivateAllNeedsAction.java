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

package won.bot.framework.eventbot.action.impl.facet;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.NodeIterator;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.needlifecycle.NeedDeactivatedEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.CloseFromOtherNeedEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.node.facet.impl.WON_TX;
import won.protocol.exception.WonMessageBuilderException;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.RdfUtils;
import won.protocol.util.WonRdfUtils;

import java.net.URI;
import java.util.Collection;

/**
 * User: Danijel
 * Date: 21.5.14.
 */
public class TwoPhaseCommitNoVoteDeactivateAllNeedsAction extends BaseEventBotAction
{
  public TwoPhaseCommitNoVoteDeactivateAllNeedsAction(EventListenerContext eventListenerContext) {
    super(eventListenerContext);
  }

  @Override
  protected void doRun(Event event, EventListener executingListener) throws Exception {

    //check the global COORDINATION_MESSAGE (must be ABORT)
    if(event instanceof CloseFromOtherNeedEvent)
    {
      WonMessage wonMessage = ((CloseFromOtherNeedEvent) event).getWonMessage();
      NodeIterator ni = RdfUtils.visitFlattenedToNodeIterator(
        wonMessage.getMessageContent(),
        new RdfUtils.ModelVisitor<NodeIterator>()
        {
          @Override
          public NodeIterator visit(final Model model) {
            return model.listObjectsOfProperty(model.createProperty(WON_TX.COORDINATION_MESSAGE.getURI()));
          }
        });
      if(ni.hasNext())
      {
        String coordinationMessageUri = ni.toList().get(0).asResource().getURI().toString();
        if(coordinationMessageUri.equals(WON_TX.COORDINATION_MESSAGE_ABORT.getURI().toString()))
          logger.debug("Sent COORDINATION_MESSAGE: {}", coordinationMessageUri);
        else
          logger.error("Content of the COORDINATION_MESSAGE must be: {}. Currently it is: {}",
            WON_TX.COORDINATION_MESSAGE_ABORT.getURI(), coordinationMessageUri);
      }
    }
    Collection<URI> toDeactivate = getEventListenerContext().getBotContext().retrieveAllNeedUris();
    for (URI uri: toDeactivate){
      getEventListenerContext().getWonMessageSender().sendWonMessage(createWonMessage(uri));
      getEventListenerContext().getEventBus().publish(new NeedDeactivatedEvent(uri));
    }
  }

  private WonMessage createWonMessage(URI needURI) throws WonMessageBuilderException {

    WonNodeInformationService wonNodeInformationService =
      getEventListenerContext().getWonNodeInformationService();

    Dataset ds = getEventListenerContext().getLinkedDataSource().getDataForResource(needURI);
    URI localWonNode = WonRdfUtils.NeedUtils.getWonNodeURIFromNeed(ds, needURI);

    return WonMessageBuilder
      .setMessagePropertiesForDeactivateFromOwner(
        wonNodeInformationService.generateEventURI(
          localWonNode),
        needURI,
        localWonNode)
      .build();
  }

}

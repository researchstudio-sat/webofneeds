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

package won.bot.framework.eventbot.action.impl.needlifecycle;

import java.net.URI;
import java.util.Collection;

import org.apache.jena.query.Dataset;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.needlifecycle.NeedDeactivatedEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.protocol.exception.WonMessageBuilderException;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.WonRdfUtils;

/**
 * User: fkleedorfer
 * Date: 28.03.14
 */
public class DeactivateAllNeedsAction extends BaseEventBotAction
{
  public DeactivateAllNeedsAction(EventListenerContext eventListenerContext) {
    super(eventListenerContext);
  }

  @Override
  protected void doRun(Event event, EventListener executingListener) throws Exception {

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

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

import won.bot.framework.events.EventListenerContext;
import won.bot.framework.events.action.BaseEventBotAction;
import won.bot.framework.events.event.Event;
import won.bot.framework.events.event.NeedSpecificEvent;
import won.bot.framework.events.event.impl.NeedDeactivatedEvent;
import won.protocol.exception.WonMessageBuilderException;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.WonRdfUtils;

import java.net.URI;

/**
 * User: fkleedorfer
 * Date: 28.03.14
 */
public class DeactivateNeedAction extends BaseEventBotAction
{
  public DeactivateNeedAction(EventListenerContext eventListenerContext) {
    super(eventListenerContext);
  }

  @Override
  protected void doRun(Event event) throws Exception {

    assert (event instanceof NeedSpecificEvent) : "can handle only NeedSpecificEvent";

    URI uri = ((NeedSpecificEvent) event).getNeedURI();
    getEventListenerContext().getWonMessageSender().sendWonMessage(createWonMessage(uri));
    getEventListenerContext().getEventBus().publish(new NeedDeactivatedEvent(uri));
  }

  private WonMessage createWonMessage(URI needURI) throws WonMessageBuilderException {

    WonNodeInformationService wonNodeInformationService =
      getEventListenerContext().getWonNodeInformationService();

    URI localWonNode = WonRdfUtils.NeedUtils.queryWonNode(
      getEventListenerContext().getLinkedDataSource().getDataForResource(needURI));

    WonMessageBuilder builder = new WonMessageBuilder();
    return builder
      .setMessagePropertiesForDeactivate(
        wonNodeInformationService.generateEventURI(
          localWonNode),
        needURI,
        localWonNode)
      .build();
  }

}

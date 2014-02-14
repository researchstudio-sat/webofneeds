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

package won.bot.framework.events.listener;

import won.bot.framework.events.Event;
import won.bot.framework.events.event.CloseFromOtherNeedEvent;
import won.bot.framework.events.event.NeedDeactivatedEvent;
import won.protocol.model.Connection;

import java.net.URI;

/**
 * EventListener that tries to deactivate both needs when it receives a connection close event.
 * For each deactivated need, a NeedDeactivatedEvent is published.
 */
public class DeactivateNeedOnConnectionCloseListener extends BaseEventListener
{

  public DeactivateNeedOnConnectionCloseListener(final EventListenerContext context)
  {
    super(context);
  }

  @Override
  public void doOnEvent(final Event event) throws Exception
  {
    if (! (event instanceof CloseFromOtherNeedEvent)) return;
    Connection con = ((CloseFromOtherNeedEvent) event).getCon();
    logger.debug("received close on connection {}, deactivating needs", con.getConnectionURI());
    deactivateNeedIfKnown(con.getNeedURI());
    deactivateNeedIfKnown(con.getRemoteNeedURI());
  }

  private void deactivateNeedIfKnown(final URI needURI) throws Exception
  {
    if (getEventListenerContext().getBotContext().isNeedKnown(needURI)){
      logger.debug("deactivating need {}", needURI);
      getEventListenerContext().getOwnerService().deactivate(needURI);
      //publish an event so other listeners can react
      getEventListenerContext().getEventBus().publish(new NeedDeactivatedEvent(needURI));
    } else {
      logger.debug("need uri {} not controlled by this bot, not deactivating it.", needURI);
    }
  }


}

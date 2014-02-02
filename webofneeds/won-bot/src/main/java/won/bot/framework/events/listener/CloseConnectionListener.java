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
import won.bot.framework.events.event.ConnectFromOtherNeedEvent;
import won.bot.framework.events.event.MessageFromOtherNeedEvent;
import won.bot.framework.events.event.OpenFromOtherNeedEvent;

import java.net.URI;

/**
 * Listener that will try to obtain a connectionURI from any event
 * passed to it and close that connection.
 */
public class CloseConnectionListener extends BaseEventListener
{
  public CloseConnectionListener(final EventListenerContext context)
  {
    super(context);
  }

  @Override
  protected void doOnEvent(final Event event) throws Exception
  {
    logger.debug("trying to close connection related to event {}", event);
    try {
      URI connectionURI = null;
      if (event instanceof MessageFromOtherNeedEvent){
        connectionURI = ((MessageFromOtherNeedEvent)event).getCon().getConnectionURI();
      } else  if (event instanceof ConnectFromOtherNeedEvent){
        connectionURI = ((ConnectFromOtherNeedEvent)event).getCon().getConnectionURI();
      } else if (event instanceof OpenFromOtherNeedEvent) {
        connectionURI = ((OpenFromOtherNeedEvent)event).getCon().getConnectionURI();
      }
      logger.debug("Extracted connection uri {}", connectionURI);
      if (connectionURI != null) {
        logger.debug("closing connection {}", connectionURI);
        getEventListenerContext().getOwnerService().close(connectionURI, null);
      }
    } catch (Exception e){
      logger.warn("error trying to close connection", e);
    }
  }
}

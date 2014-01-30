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

package won.bot.core.eventlistener;

import won.bot.core.event.CloseFromOtherNeedEvent;
import won.bot.events.Event;
import won.protocol.model.Connection;

/**
 * EventListener that deactivates both needs when it receives a connection close event.
 */
public class DeactivateNeedsOnConnectionCloseListener extends BaseEventListener
{

  public DeactivateNeedsOnConnectionCloseListener(final EventListenerContext context)
  {
    super(context);
  }

  @Override
  public void onEvent(final Event event) throws Exception
  {
    if (! (event instanceof CloseFromOtherNeedEvent)) return;
    Connection con = ((CloseFromOtherNeedEvent) event).getCon();
    logger.debug("received close on connection {}, deactivating needs {} and {}", new Object[]{con.getConnectionURI(), con.getNeedURI(), con.getRemoteNeedURI()});
    getEventListenerContext().getOwnerService().deactivate(con.getNeedURI());
    getEventListenerContext().getOwnerService().deactivate(con.getRemoteNeedURI());
  }
}

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

import won.bot.framework.events.event.Event;
import won.bot.framework.events.action.BaseEventBotAction;
import won.bot.framework.events.event.impl.ConnectFromOtherNeedEvent;
import won.bot.framework.events.event.impl.OpenFromOtherNeedEvent;
import won.bot.framework.events.EventListenerContext;
import won.protocol.model.ConnectionState;

/**
 * User: fkleedorfer
 * Date: 30.01.14
 */
public class OpenConnectionAction extends BaseEventBotAction
{

  public OpenConnectionAction(final EventListenerContext context)
  {
    super(context);
  }

  @Override
  public void doRun(final Event event) throws Exception {
    if (event instanceof ConnectFromOtherNeedEvent) {
      ConnectFromOtherNeedEvent connectEvent = (ConnectFromOtherNeedEvent) event;
      logger.debug("auto-replying to connect for connection {}", connectEvent.getCon().getConnectionURI());
      getEventListenerContext().getOwnerService().open(connectEvent.getCon().getConnectionURI(), null);
      return;
    }

    if (event instanceof OpenFromOtherNeedEvent) {
      OpenFromOtherNeedEvent openEvent = (OpenFromOtherNeedEvent) event;
      if (openEvent.getCon().getState() == ConnectionState.REQUEST_RECEIVED) {
        logger.debug("auto-replying to open for connection {}", openEvent.getCon().getConnectionURI());
        getEventListenerContext().getOwnerService().open(openEvent.getCon().getConnectionURI(), null);
      } else {
        logger.debug("not auto-replying to open event with open as connection state is {}, not REQUEST_RECEIVED",openEvent.getCon().getState() );
      }
      return;
    }

  }
}

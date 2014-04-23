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
import won.bot.framework.events.event.ConnectionSpecificEvent;
import won.bot.framework.events.event.Event;
import won.bot.framework.events.event.impl.HintFromMatcherEvent;
import won.protocol.model.FacetType;
import won.protocol.util.WonRdfUtils;

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
    if (event instanceof ConnectionSpecificEvent) {
      ConnectionSpecificEvent connectEvent = (ConnectionSpecificEvent) event;
      logger.debug("auto-replying to connect for connection {}", connectEvent );
      getEventListenerContext().getOwnerService().open(connectEvent.getConnectionURI(), null);
      return;
    } else if (event instanceof HintFromMatcherEvent) {
      HintFromMatcherEvent hintEvent = (HintFromMatcherEvent) event;
      logger.debug("opening connection based on hint {}", event);
      getEventListenerContext().getOwnerService().connect(hintEvent.getMatch().getFromNeed(),
        hintEvent.getMatch().getToNeed(), WonRdfUtils.FacetUtils.createFacetModelForHintOrConnect(FacetType
        .OwnerFacet.getURI(), FacetType.OwnerFacet.getURI()));
    }
  }
}

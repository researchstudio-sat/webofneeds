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
import won.bot.framework.events.EventListenerContext;
import won.protocol.util.WonRdfUtils;

import java.net.URI;
import java.util.List;

/**
 * BaseEventBotAction connecting two needs on the specified facets. The need's URIs are obtained from
 * the bot context. The first two URIs found there are used.
 */
public class ConnectTwoNeedsAction extends BaseEventBotAction
{
  private URI remoteFacet;
  private URI localFacet;

  public ConnectTwoNeedsAction(final EventListenerContext eventListenerContext, final URI remoteFacet, final URI localFacet)
  {
    super(eventListenerContext);
    this.remoteFacet = remoteFacet;
    this.localFacet = localFacet;
  }

  @Override
  public void doRun(Event event)
  {
    List<URI> needs = getEventListenerContext().getBotContext().listNeedUris();
    try {
      getEventListenerContext().getOwnerService().connect(needs.get(0), needs.get(1), WonRdfUtils.FacetUtils.createFacetModelForHintOrConnect(localFacet, remoteFacet), null);
    } catch (Exception e) {
      logger.warn("could not connect {} and {}", new Object[]{needs.get(0), needs.get(1)}, e);
    }
  }
}

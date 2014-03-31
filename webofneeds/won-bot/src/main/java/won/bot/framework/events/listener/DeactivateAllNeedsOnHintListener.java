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
import won.bot.framework.events.event.HintFromMatcherEvent;
import won.bot.framework.events.event.NeedDeactivatedEvent;
import won.protocol.model.Connection;
import won.protocol.model.FacetType;
import won.protocol.model.Match;

import java.net.URI;
import java.util.List;

/**
 * EventListener that tries to deactivate both needs when it receives a connection close event.
 * For each deactivated need, a NeedDeactivatedEvent is published.
 */
public class DeactivateAllNeedsOnHintListener extends BaseEventListener
{

  public DeactivateAllNeedsOnHintListener(final EventListenerContext context)
  {
    super(context);
  }

  @Override
  public void doOnEvent(final Event event) throws Exception {
    Match match = ((HintFromMatcherEvent) event).getMatch();
    deactivateNeed(match.getFromNeed());
    deactivateNeed(match.getToNeed());

    getEventListenerContext().getEventBus().unsubscribe(HintFromMatcherEvent.class, this);
  }

  private void deactivateNeed(final URI needURI) throws Exception {
      logger.debug("deactivating need {}", needURI);

            getEventListenerContext().getOwnerService().deactivate(needURI);
            //publish an event so other listeners can react
            getEventListenerContext().getEventBus().publish(new NeedDeactivatedEvent(needURI));

  }


}

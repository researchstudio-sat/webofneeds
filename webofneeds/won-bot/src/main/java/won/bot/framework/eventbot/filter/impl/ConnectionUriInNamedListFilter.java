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

package won.bot.framework.eventbot.filter.impl;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.event.ConnectionSpecificEvent;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.NeedSpecificEvent;

import java.net.URI;

/**
 * Filter that accepts ConnectionSpecificEvents if their needURI is in the specified named list.
 */
public class ConnectionUriInNamedListFilter extends AbstractNamedUriListFilter {
  public ConnectionUriInNamedListFilter(final EventListenerContext context, final String listname) {
    super(context, listname);
  }

  @Override protected URI getURIFromEvent(final Event event) {
    if (event instanceof NeedSpecificEvent) {
      return ((ConnectionSpecificEvent) event).getConnectionURI();
    }
    return null;
  }
}

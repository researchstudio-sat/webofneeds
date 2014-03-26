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

package won.bot.framework.events.listener.filter;

import won.bot.framework.events.Event;
import won.bot.framework.events.listener.EventListenerContext;

import java.net.URI;
import java.util.List;

/**
 * Event filter that accepts need specific events the URI of which is found in the specified named URI list.
 */
public abstract class AbstractNamedUriListFilter extends EventListenerContextAwareFilter
{
  private String listname;

  public AbstractNamedUriListFilter(final EventListenerContext context, final String listname)
  {
    super(context);
    this.listname = listname;
  }

  @Override
  public boolean accept(final Event event)
  {
    URI uri = getURIFromEvent(event);
    if (uri == null) return false;
    List<URI> uris = getContext().getBotContext().getNamedNeedUriList(listname);
    return uris.contains(uri);
  }

  protected abstract URI getURIFromEvent(final Event event);
}

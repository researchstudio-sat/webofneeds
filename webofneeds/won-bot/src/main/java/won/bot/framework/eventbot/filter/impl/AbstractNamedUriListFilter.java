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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.event.Event;

import java.net.URI;
import java.util.Collection;

/**
 * Event filter that accepts need specific events the URI of which is found in
 * the specified named URI list.
 */
public abstract class AbstractNamedUriListFilter extends EventListenerContextAwareFilter {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  private String listname;

  public AbstractNamedUriListFilter(final EventListenerContext context, final String listname) {
    super(context);
    this.listname = listname;
  }

  @Override
  public boolean accept(final Event event) {
    URI uri = getURIFromEvent(event);
    if (uri == null)
      return false;
    Collection<URI> uris = getContext().getBotContext().getNamedNeedUriList(listname);
    if (uris == null) {
      logger.debug("filtering by named need uri list, but no list found with name '{}'", listname);
      return false;
    }
    return uris.contains(uri);
  }

  protected abstract URI getURIFromEvent(final Event event);
}

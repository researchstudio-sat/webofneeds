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

import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.NeedSpecificEvent;
import won.bot.framework.eventbot.filter.EventFilter;

import java.net.URI;

/**
 * Abstract base class for filters that will only accept events (need specific or connection specific) with the specified need URI.
 */
public class NeedUriEventFilter implements EventFilter {
  private URI needURI;

  public NeedUriEventFilter(final URI needURI) {
    this.needURI = needURI;
  }

  /**
   * Factory method for creating a filter from an event by using its need URI.
   *
   * @param event
   * @return the filter or null if no need URI could be obtained from the event.
   */
  public static NeedUriEventFilter forEvent(Event event) {
    URI needUri = getNeedUriFromEvent(event);
    if (needUri == null)
      return null;
    return new NeedUriEventFilter(needUri);
  }

  @Override public boolean accept(final Event event) {
    URI needUriOfEvent = getNeedUriFromEvent(event);
    if (needUriOfEvent == null)
      return false;
    return needUriOfEvent.equals(this.needURI);
  }

  public URI getNeedURI() {
    return needURI;
  }

  private static URI getNeedUriFromEvent(final Event event) {
    if (event instanceof NeedSpecificEvent) {
      return ((NeedSpecificEvent) event).getNeedURI();
    }
    return null;
  }
}

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

import java.net.URI;

import won.bot.framework.eventbot.event.ConnectionSpecificEvent;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.filter.EventFilter;

/**
 * Abstract base class for filters that will only accept events (connection specific or connection specific) with the specified connection URI.
 */
public class ConnectionUriEventFilter implements EventFilter
{
  private URI connectionURI;

  public ConnectionUriEventFilter(final URI connectionURI)
  {
    this.connectionURI = connectionURI;
  }

  /**
   * Factory method for creating a filter from an event by using its connection URI.
   * @param event
   * @return the filter or null if no connection URI could be obtained from the event.
   */
  public static ConnectionUriEventFilter forEvent(Event event){
    URI connectionUri = getConnectionUriFromEvent(event);
    if (connectionUri == null) return null;
    return new ConnectionUriEventFilter(connectionUri);
  }

  @Override
  public boolean accept(final Event event)
  {
    URI connectionUriOfEvent = getConnectionUriFromEvent(event);
    if (connectionUriOfEvent == null) return false;
    return connectionUriOfEvent.equals(this.connectionURI);
  }

  public URI getConnectionURI()
  {
    return connectionURI;
  }

  private static URI getConnectionUriFromEvent(final Event event)
  {
    if (event instanceof ConnectionSpecificEvent){
      return ((ConnectionSpecificEvent)event).getConnectionURI();
    }
    return null;
  }
}

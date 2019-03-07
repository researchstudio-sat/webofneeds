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

import java.util.List;

import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.filter.AbstractCompositeFilter;
import won.bot.framework.eventbot.filter.EventFilter;

/**
 * User: fkleedorfer
 * Date: 25.03.14
 */
public class OrFilter extends AbstractCompositeFilter
{

    public OrFilter() {
    }

    public OrFilter(List<EventFilter> filters) {
        super(filters);
    }

    public OrFilter(EventFilter... filters) {
        super(filters);
    }

    @Override
  public synchronized boolean accept(final Event event)
  {
    for (EventFilter filter: getFilters()){
      if (filter.accept(event)) return true;
    }
    return false;
  }

}

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

package won.bot.framework.events.filter;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Composite filter.
 */
public abstract class AbstractCompositeFilter implements CompositeFilter
{
  private List<EventFilter> filters;

  public AbstractCompositeFilter()
  {
    this.filters = new LinkedList<EventFilter>();
  }

  public AbstractCompositeFilter(final List<EventFilter> filters)
  {
    this.filters = filters;
  }

  @Override
  public synchronized void addFilter(EventFilter filter) {
    LinkedList newList = new LinkedList<EventFilter>();
    newList.addAll(this.filters);
    newList.add(filter);
    this.filters = newList;
  }

  /**
   * Returns a shallow copy of the filters.
   */
  public List<EventFilter> getFilters()
  {
    ArrayList<EventFilter> copy = new ArrayList<>(filters.size());
    copy.addAll(filters);
    return copy;
  }

  @Override
  public synchronized void setFilters(final List<EventFilter> filters) {
    this.filters = filters;
  }
}

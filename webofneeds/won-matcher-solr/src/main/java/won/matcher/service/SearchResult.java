/*
 * Copyright 2012  Research Studios Austria Forschungsges.m.b.H.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package won.matcher.service;

import java.net.URI;
import java.util.Collections;
import java.util.List;

/**
 * User: fkleedorfer
 * Date: 12.09.13
 */
public class SearchResult
{
  private URI originator;
  private List<SearchResultItem> items;

  public SearchResult(URI originator, final List<SearchResultItem> items)
  {
    this.originator = originator;
    this.items = Collections.unmodifiableList(items);
  }

  public List<SearchResultItem> getItems()
  {
    return items;
  }

  public URI getOriginator()
  {
    return originator;
  }
}

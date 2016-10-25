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

package won.bot.framework.bot.context;

import won.bot.framework.bot.BotContext;

import java.net.URI;
import java.util.*;

/**
 * Straightforward BotContext implementation using a List and a Map.
 */
public class InMemoryBotContext implements BotContext
{
  private Set<URI> nodeUris = new HashSet<URI>();
  private Map<String, List<URI>> namedNeedUriLists = new HashMap<String, List<URI>>();
  private Map<Object, Object> genericContext = new HashMap<Object,Object>();

  @Override
  public synchronized List<URI> listNeedUris() {

    Set<URI> needUris = new HashSet<>();
    List ret = new ArrayList<URI>();

    Iterator<List<URI>> iter = namedNeedUriLists.values().iterator();
    while (iter.hasNext()) {
      needUris.addAll(iter.next());
    }

    ret.addAll(needUris);
    return ret;
  }

  @Override
  public synchronized boolean isNeedKnown(final URI needURI) {
    return listNeedUris().contains(needURI);
  }

  @Override
  public synchronized boolean isNodeKnown(final URI wonNodeURI) {
    return nodeUris.contains(wonNodeURI);
  }


  @Override
  public synchronized void rememberNodeUri(final URI uri) {
    nodeUris.add(uri);
  }

  @Override
  public synchronized void removeNeedUriFromNamedNeedUriList(URI uri, String name) {
    List<URI> uris = namedNeedUriLists.get(name);
    uris.remove(uri);
  }

  @Override
  public synchronized void appendToNamedNeedUriList(URI uri, String name) {
    List<URI> uris = this.namedNeedUriLists.get(name);
    if (uris == null) {
      uris = new ArrayList<>();
    }
    uris.add(uri);
    this.namedNeedUriLists.put(name, uris);
  }

  @Override
  public synchronized List<URI> getNamedNeedUriList(String name) {

    List<URI> namedList = this.namedNeedUriLists.get(name);
    if (namedList != null) {
      List<URI> ret = new LinkedList<>();
      ret.addAll(this.namedNeedUriLists.get(name));
      return ret;
    }
    return null;
  }

  @Override
  public synchronized void put(String collectionName, String key, Object value) {
    this.genericContext.put(key, value);
  }

  @Override
  public synchronized Object get(String collectionName, String key) {
    return this.genericContext.get(key);
  }

  @Override
  public synchronized Object remove(String collectionName, String key) {
    return this.genericContext.remove(key);
  }

  @Override
  public synchronized Collection<Object> values(String collectionName) {
    return genericContext.values();
  }
}

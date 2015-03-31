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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.bot.framework.bot.BotContext;

import java.net.URI;
import java.util.*;

/**
 * Straightforward BotContext implementation using a List and a Map.
 */
public class InMemoryBotContext implements BotContext
{
  private List<URI> needUris = new ArrayList<URI>();
  private List<URI> nodeUris = new ArrayList<URI>();
  private Map<String, URI> namedNeedUris = new HashMap<String, URI>();
  private Map<String, List<URI>> namedNeedUriLists = new HashMap<String, List<URI>>();
  private Logger logger = LoggerFactory.getLogger(this.getClass());

  @Override
  public synchronized List<URI> listNeedUris() {
    return Collections.unmodifiableList(needUris);
  }

  @Override
  public synchronized List<URI> listNodeUris() {
    return Collections.unmodifiableList(nodeUris);
  }

  @Override
  public synchronized boolean isNeedKnown(final URI needURI) {
    logger.debug("checking whether need {} is known in bot context ", needURI);
    return needUris.contains(needURI);
  }

  @Override
  public synchronized boolean isNodeKnown(final URI wonNodeURI) {
    return nodeUris.contains(wonNodeURI);
  }


  /**
   * Caution, this call is expensive as it uses List.contains(uri).
   *
   * @param uri
   * @param name
   */
  @Override
  public synchronized void rememberNeedUriWithName(final URI uri, final String name) {
    if (!needUris.contains(uri)) {
      needUris.add(uri);
    }
    namedNeedUris.put(name, uri);
  }

  /**
   * @param uri
   */
  @Override
  public synchronized void rememberNeedUri(final URI uri) {
    needUris.add(uri);
  }

  @Override
  public synchronized void rememberNodeUri(final URI uri) {
    nodeUris.add(uri);
  }

  @Override
  public synchronized void removeNeedUri(URI uri) {
    needUris.remove(uri);
  }

  @Override
  public synchronized void removeNamedNeedUri(String name) {
    URI uri = namedNeedUris.remove(name);
    if (uri != null) {
      needUris.remove(uri);
    }
  }

  @Override
  public synchronized void removeNeedUriFromNamedNeedUriList(URI uri, String name) {
    List<URI> uris = namedNeedUriLists.get(name);
    uris.remove(uri);
    needUris.remove(uri);
  }

  @Override
  public synchronized URI getNeedByName(final String name) {
    return namedNeedUris.get(name);
  }

  @Override
  public synchronized List<String> listNeedUriNames() {
    List ret = new ArrayList<URI>(namedNeedUris.size());
    ret.addAll(namedNeedUris.keySet());
    return ret;
  }

  @Override
  public synchronized void rememberNamedNeedUriList(List<URI> uris, String name) {
    this.namedNeedUriLists.put(name, uris);
    for (URI uri : uris) {
      if (!needUris.contains(uri)) {
        needUris.add(uri);
      }
    }
  }

  @Override
  public synchronized void appendToNamedNeedUriList(URI uri, String name) {
    if (!needUris.contains(uri)) {
      needUris.add(uri);
    }
    List<URI> uris = this.namedNeedUriLists.get(name);
    if (uris == null) {
      uris = new ArrayList<URI>();
    }
    uris.add(uri);
    this.namedNeedUriLists.put(name, uris);

  }

  @Override
  public synchronized List<URI> getNamedNeedUriList(String name) {
    return this.namedNeedUriLists.get(name);
  }
}

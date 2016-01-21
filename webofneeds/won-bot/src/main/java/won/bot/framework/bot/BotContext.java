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

package won.bot.framework.bot;

import com.sun.jndi.toolkit.url.Uri;

import java.net.URI;
import java.util.List;

/**
 * Used by a bot to remember which needs it knows.
 */
public interface BotContext
{
  /**
   * List all need URIs known in this memory.
   * @return
   */
  public List<URI> listNeedUris();

  /**
   * list all node URIs known in this memory.
   * @return
   */
  public List<URI> listNodeUris();
  /**
   * Check if this memory knows the specified need URI.
   * @param needURI
   * @return
   */
  public boolean isNeedKnown(URI needURI);

  /**
   * Check if this memory knows the specified node URI
   * @param wonNodeURI
   * @return
   */
  public boolean isNodeKnown(URI wonNodeURI);

  /**
   * Save the specified need URI in this memory under the specified name.
   * @param uri
   * @param name
   */
  public void rememberNeedUriWithName(URI uri, String name);

  /**
   * Save the specified need URI in this memory.
   * @param uri
   */
  public void rememberNeedUri(URI uri);
  public void rememberNodeUri(URI uri);
  public void rememberNamedNeedUriList(List<URI> uris, String name);
  public void appendToNamedNeedUriList(URI uri, String name);
  public List<URI> getNamedNeedUriList(String name);

  public void removeNeedUri(URI uri);
  public void removeNamedNeedUri(String name);
  public void removeNeedUriFromNamedNeedUriList(URI uri, String name);

  /**
   * Fetch a need URI by its name. The URI must have been given a name previously.
   * @param name
   * @return
   */
  public URI getNeedByName(String name);

  /**
   * List all need URI's names.
   */
  public List<String> listNeedUriNames();

  /**
   * Put an arbitrary object in the context.
   * @param key
   * @param value
   */
  public void put(Object key, Object value);

  /**
   * Retrieve an object previously  added using put().
   * @param key
   * @return
   */
  public Object get(Object key);

}

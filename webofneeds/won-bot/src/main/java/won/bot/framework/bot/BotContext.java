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

import java.net.URI;
import java.util.Collection;
import java.util.List;

/**
 * Used by a bot to remember which needs it knows.
 */
public interface BotContext
{
  /**
   * List all need URIs known (from all named need lists)
   * @return
   */
  public List<URI> listNeedUris();

  /**
   * Check if specified need URI is known (from all named need lists)
   * @param needURI
   * @return
   */
  public boolean isNeedKnown(URI needURI);

  /**
   * Add an URI to a named need list
   *
   * @param uri
   * @param name
   */
  public void appendToNamedNeedUriList(URI uri, String name);

  /**
   * List all need URIs from a specified named need list
   *
   * @param name
   * @return
   */
  public List<URI> getNamedNeedUriList(String name);

  /**
   * Remove a need URI from a named need list
   *
   * @param uri
   * @param name
   */
  public void removeNeedUriFromNamedNeedUriList(URI uri, String name);

  /**
   * Check if specified node URI is known
   * @param wonNodeURI
   * @return
   */
  public boolean isNodeKnown(URI wonNodeURI);

  /**
   * Save node uri
   * @param uri
   */
  public void rememberNodeUri(URI uri);

  /**
   * Put an arbitrary object in the context.
   *
   * @param collectionName
   * @param key
   * @param value
   */
  public void put(String collectionName, String key, final Object value);

  /**
   *
   * @param key
   * @return
   */

  /**
   * Retrieve an object object from a collection previously added using put().
   *
   * @param collectionName
   * @param key
   * @return
   */
  public Object get(String collectionName, String key);

  /**
   * Retrieve all objects from one collection
   *
   * @param collectionName
   * @return
   */
  public Collection<Object> values(String collectionName);

  /**
   * Remove an arbitrary object from the context
   *
   * @param collectionName
   * @param key
   * @return
   */
  public Object remove(String collectionName, String key);

}

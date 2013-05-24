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

package won.protocol.ws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.ws.Service;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Abstract Client Factory that implements the cache and caching methods.
 *
 * User: atus
 * Date: 14.05.13
 */
public abstract class AbstractClientFactory<T extends Service>
{
  private final Logger logger = LoggerFactory.getLogger(getClass());

  private Map<String, T> cache;

  protected AbstractClientFactory()
  {
    cache = new HashMap<String, T>();
  }

  /**
   * Call when you want to store an endpoint in the cache.
   *
   * @param uri endpoint URI
   * @param endpoint
   */
  protected void cacheClient(URI uri, T endpoint)
  {
    logger.info("Added endpoint for {} to cache. {}", uri.toString(), endpoint.toString());
    cache.put(uri.toString(), endpoint);
  }

  /**
   * Call when you want to retrieve an endpoint from the cache.
   *
   * @param uri endpoint URI
   * @return
   */
  protected T getCachedClient(URI uri)
  {
    T client = cache.get(uri.toString());
    if (client != null)
      logger.info("Loaded client for {} from cache", uri.toString());
    return client;
  }
}

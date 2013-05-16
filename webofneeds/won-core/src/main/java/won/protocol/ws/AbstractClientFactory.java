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
public class AbstractClientFactory<T extends Service>
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

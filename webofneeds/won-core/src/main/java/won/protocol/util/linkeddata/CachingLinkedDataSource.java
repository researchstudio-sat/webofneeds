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

package won.protocol.util.linkeddata;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.DatasetFactory;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.constructs.blocking.CacheEntryFactory;
import net.sf.ehcache.constructs.blocking.SelfPopulatingCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.ehcache.EhCacheCacheManager;

import java.net.URI;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;

/**
 * LinkedDataSource implementation that uses an ehcache for caching.
 */
@Qualifier("default")
public class CachingLinkedDataSource extends LinkedDataSourceBase implements LinkedDataSource, InitializingBean
{
  private static final String CACHE_NAME = "linkedDataCache";
  private final Logger logger = LoggerFactory.getLogger(getClass());
  @Autowired(required = true)
  private EhCacheCacheManager cacheManager;
  private Ehcache cache;
  private CrawlerCallback crawlerCallback = null;

  //In-memory dataset for caching linked data.

  /**
   * Removes the element associated with the
   * specified URI from the cache
   * @param resource
   */
  public void invalidate(URI resource) {
    assert resource != null : "resource must not be null";
    cache.remove(resource);
  }

  public void invalidate(URI resource, URI requesterWebID) {
    assert (resource != null && requesterWebID != null) : "resource and requester must not be null";
    cache.remove(resource);
    List<URI> key = Arrays.asList(new URI[]{resource, requesterWebID});
    cache.remove(key);
  }
  
  public void clear(){
    cache.removeAll();
  }

  public Dataset getDataForResource(URI resource){

    assert resource != null : "resource must not be null";
    Element element = null;

    try {
        element = cache.get(resource);
    }catch(CacheException e){
        //logging on warn level as not reporting errors here can make misconfiguration hard to detect
        logger.warn(String.format("Couldn't fetch resource %s", resource));
        logger.debug("Exception is:", e);
        return DatasetFactory.createMem();
    }

    Object dataset = element.getObjectValue();
    if (dataset instanceof Dataset) return (Dataset) dataset;
    throw new IllegalStateException(
        new MessageFormat("The underlying linkedDataCache should only contain Datasets, but we got a {0} for URI {1}")
            .format(new Object[]{dataset.getClass(), resource}));
  }

  /*@Override
  public Dataset getDataForResource(final URI resource, final URI requesterWebID) {
    assert (resource != null && requesterWebID != null) : "resource and requester must not be null";
    Element element = null;

    // first try without providing webid - can be a public resource
    try {
      element = cache.get(resource);
    }catch(CacheException e){
      element = null;
    }

    // if doesn't work - provide webid
    if (element == null) {
      List<URI> key = Arrays.asList(new URI[]{resource, requesterWebID});
      try {
        element = cache.get(key);
      }catch(CacheException e){
        logger.warn(String.format("Couldn't fetch resource %s", resource));
        logger.debug("Exception is:", e);
        return DatasetFactory.createMem();
      }
    }

    Object dataset = element.getObjectValue();
    if (dataset instanceof Dataset) return (Dataset) dataset;
    throw new IllegalStateException(
      new MessageFormat("The underlying linkedDataCache should only contain Datasets, but we got a {0} for URI {1}")
        .format(new Object[]{dataset.getClass(), resource}));
  }*/

  @Override
  public void afterPropertiesSet() throws Exception
  {
    Ehcache baseCache = cacheManager.getCacheManager().getCache(CACHE_NAME);
    if (baseCache == null) {
      throw new IllegalArgumentException(String.format("could not find a cache with name '%s' in ehcache config",
                                                    CACHE_NAME));
    }
    this.cache = new SelfPopulatingCache(baseCache, new LinkedDataCacheEntryFactory());
  }

  public void setCacheManager(final EhCacheCacheManager cacheManager)
  {
    this.cacheManager = cacheManager;
  }


  private class LinkedDataCacheEntryFactory implements CacheEntryFactory {
    private LinkedDataCacheEntryFactory() {
    }

    @Override
    public Object createEntry(final Object key) throws Exception {
      if (key instanceof URI) {
        logger.debug("fetching linked data for URI {}", key);
        Dataset dataset = linkedDataRestClient.readResourceData((URI) key);
        if (crawlerCallback != null){
          try {
            crawlerCallback.onDatasetCrawled((URI) key, dataset);
          } catch (Exception e ){
            logger.info(String.format("error during callback execution for dataset %s", key.toString()), e);
          }
        }
        return dataset;
      } else if (key instanceof List && ((List)key).size() == 2 && ((List)key).get(0) instanceof URI && ((List)key)
        .get(1) instanceof URI){
        logger.debug("fetching linked data for URI {} for requester {}", ((List) key).get(0), ((List) key).get(1));
        Dataset dataset = linkedDataRestClient.readResourceData((URI) ((List) key).get(0), (URI) ((List) key).get(1));
        if (crawlerCallback != null){
          try {
            crawlerCallback.onDatasetCrawled((URI) key, dataset);
          } catch (Exception e ){
            logger.info(String.format("error during callback execution for dataset %s", key.toString()), e);
          }
        }
        return dataset;
      } else {
        throw new IllegalArgumentException("this cache only resolves URIs and URIs with requester WebIDs to Models");
      }
    }
  }

  @Autowired(required = false)
  public void setCrawlerCallback(final CrawlerCallback crawlerCallback) {
    this.crawlerCallback = crawlerCallback;
  }
}

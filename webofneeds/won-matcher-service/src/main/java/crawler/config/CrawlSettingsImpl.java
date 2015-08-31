package crawler.config;

import akka.actor.Extension;
import com.typesafe.config.Config;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Settings configuration class of the crawler
 *
 * User: hfriedrich
 * Date: 24.04.2015
 */
public class CrawlSettingsImpl implements Extension
{
  public final List<String> PROPERTYPATHS_BASE;
  public final List<String> PROPERTYPATHS_NONBASE;
  public final FiniteDuration METADATA_UPDATE_DURATION;
  public final int METADATA_UPDATE_MAX_BULK_SIZE;
  public final int HTTP_CONNECTION_TIMEOUT;
  public final int HTTP_READ_TIMEOUT;

  public CrawlSettingsImpl(Config config) {

    PROPERTYPATHS_BASE = config.getStringList("crawler.propertyPaths.base");
    PROPERTYPATHS_NONBASE = config.getStringList("crawler.propertyPaths.nonBase");
    METADATA_UPDATE_DURATION = Duration.create(config.getDuration(
      "crawler.metaDataUpdate.maxDuration", TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS);
    METADATA_UPDATE_MAX_BULK_SIZE = config.getInt("crawler.metaDataUpdate.maxBulkSize");
    HTTP_CONNECTION_TIMEOUT = config.getInt("crawler.http.timeout.connection");
    HTTP_READ_TIMEOUT = config.getInt("crawler.http.timeout.read");
  }
}
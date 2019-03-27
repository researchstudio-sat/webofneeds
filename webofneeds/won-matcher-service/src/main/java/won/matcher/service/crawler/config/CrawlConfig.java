package won.matcher.service.crawler.config;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

/**
 * Configuration for the crawler Created by hfriedrich on 07.09.2015.
 */
@Configuration
@PropertySource("file:${WON_CONFIG_DIR}/matcher-service.properties")
public class CrawlConfig {
    @Value("#{'${crawler.propertyPaths.base}'.split(',')}")
    private List<String> crawlBasePropertyPaths;
    @Value("#{'${crawler.propertyPaths.nonBase}'.split(',')}")
    private List<String> crawlNonBasePropertyPaths;
    @Value("${crawler.http.timeout.connection}")
    private long httpConnectionTimeout;
    @Value("${crawler.http.timeout.read}")
    private long httpReadTimeout;
    @Value("${crawler.metaDataUpdate.maxDuration}")
    private long metaDataUpdateMaxDuration;
    @Value("${crawler.metaDataUpdate.maxBulkSize}")
    private long metaDataUpdateMaxBulkSize;
    @Value("${crawler.recrawl.interval.minutes}")
    private long recrawlIntervalMinutes;

    public List<String> getCrawlBasePropertyPaths() {
        return crawlBasePropertyPaths;
    }

    public List<String> getCrawlNonBasePropertyPaths() {
        return crawlNonBasePropertyPaths;
    }

    public long getHttpConnectionTimeout() {
        return httpConnectionTimeout;
    }

    public long getHttpReadTimeout() {
        return httpReadTimeout;
    }

    public FiniteDuration getRecrawlIntervalDuration() {
        return Duration.create(recrawlIntervalMinutes, TimeUnit.MINUTES);
    }

    public FiniteDuration getMetaDataUpdateMaxDuration() {
        return Duration.create(metaDataUpdateMaxDuration, TimeUnit.MILLISECONDS);
    }

    public long getMetaDataUpdateMaxBulkSize() {
        return metaDataUpdateMaxBulkSize;
    }
}

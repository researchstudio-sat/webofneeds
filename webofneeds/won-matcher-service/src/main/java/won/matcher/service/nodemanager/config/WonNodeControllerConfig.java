package won.matcher.service.nodemanager.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Configuration for the won node controller
 * <p>
 * Created by hfriedrich on 07.09.2015.
 */
@Configuration @PropertySource("file:${WON_CONFIG_DIR}/matcher-service.properties") public class WonNodeControllerConfig {
  @Value("#{'${wonNodeController.wonNode.crawl}'.split(',')}") private List<String> crawlWonNodes;

  @Value("#{'${wonNodeController.wonNode.skip}'.split(',')}") private List<String> skipWonNodes;

  @Value("${wonNodeController.wonNode.lifeCheckDuration}") private long lifeCheckDuration;

  public FiniteDuration getLifeCheckDuration() {
    return Duration.create(lifeCheckDuration, TimeUnit.MILLISECONDS);
  }

  public List<String> getSkipWonNodes() {
    return skipWonNodes;
  }

  public List<String> getCrawlWonNodes() {
    return crawlWonNodes;
  }

}

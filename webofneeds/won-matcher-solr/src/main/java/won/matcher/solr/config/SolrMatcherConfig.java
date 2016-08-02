package won.matcher.solr.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.PropertySource;

/**
 * Created by hfriedrich on 15.09.2015.
 */
@Configuration
@ImportResource({"classpath:/spring/component/monitoring/monitoring-recorder.xml", "classpath:/spring/component/scheduling/matcher-service-scheduling.xml"})
@PropertySource("file:${WON_CONFIG_DIR}/matcher-solr.properties")
public class SolrMatcherConfig
{
  @Value("${matcher.solr.uri.solr.server}")
  private String solrServerUri;

  @Value("${matcher.solr.uri.solr.server.public}")
  private String solrServerPublicUri;

  @Value("${matcher.solr.query.maxHints}")
  private int maxHints;

  @Value("${matcher.solr.monitoring}")
  private boolean monitoringEnabled;

  @Value("${matcher.solr.index.commit}")
  private boolean commitIndexedNeedImmediately;

  @Value("${matcher.solr.query.scoreThreshold}")
  private float scoreThreshold;

  public float getScoreThreshold() {
    return scoreThreshold;
  }

  public String getSolrServerUri() {
    return solrServerUri;
  }

  public long getMaxHints() {
    return maxHints;
  }

  public boolean isCommitIndexedNeedImmediately() {
    return commitIndexedNeedImmediately;
  }

  public String getSolrServerPublicUri() {
    return solrServerPublicUri;
  }

  public boolean isMonitoringEnabled() {
    return monitoringEnabled;
  }

}

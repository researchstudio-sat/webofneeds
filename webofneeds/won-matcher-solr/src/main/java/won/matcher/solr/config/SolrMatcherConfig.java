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

  @Value("${matcher.solr.query.score.threshold}")
  private float scoreThreshold;

  @Value("${matcher.solr.query.cutAfterIthElbowInScore}")
  private int cutAfterIthElbowInScore;

  @Value("${matcher.solr.createHintsForBothNeeds}")
  private boolean createHintsForBothNeeds;

  @Value("${matcher.solr.query.score.normalizationFactor}")
  private float scoreNormalizationFactor;

  public float getScoreThreshold() {
    return scoreThreshold;
  }

  public String getSolrServerUri() {
    return solrServerUri;
  }

  public int getMaxHints() {
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

  public boolean isCreateHintsForBothNeeds() {
    return createHintsForBothNeeds;
  }

  public int getCutAfterIthElbowInScore() {
    return cutAfterIthElbowInScore;
  }

  public float getScoreNormalizationFactor() {
    return scoreNormalizationFactor;
  }
}

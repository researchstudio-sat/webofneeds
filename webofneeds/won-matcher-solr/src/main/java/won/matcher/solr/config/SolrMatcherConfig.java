package won.matcher.solr.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.PropertySource;

/**
 * Created by hfriedrich on 15.09.2015.
 */
@Configuration @ImportResource({
    "classpath:/spring/component/scheduling/matcher-service-scheduling.xml" }) @PropertySource("file:${WON_CONFIG_DIR}/matcher-solr.properties") public class SolrMatcherConfig {
  @Value("${matcher.solr.uri.solr.server}") private String solrServerUri;

  @Value("${matcher.solr.core}") private String solrCore;

  @Value("${matcher.solr.test.core}") private String solrTestCore;

  @Value("${matcher.solr.uri.solr.server.public}") private String solrServerPublicUri;

  @Value("${matcher.solr.query.maxHints}") private int maxHints;

  @Value("${matcher.solr.query.maxHintsForCounterparts}") private int maxHintsForCounterparts;

  @Value("${matcher.solr.index.commit}") private boolean commitIndexedNeedImmediately;

  @Value("${matcher.solr.query.score.threshold}") private float scoreThreshold;

  @Value("${matcher.solr.query.cutAfterIthElbowInScore}") private int cutAfterIthElbowInScore;

  @Value("${matcher.solr.query.score.normalizationFactor}") private float scoreNormalizationFactor;

  public float getScoreThreshold() {
    return scoreThreshold;
  }

  public String getSolrServerUri() {
    return solrServerUri;
  }

  public int getMaxHints() {
    return maxHints;
  }

  public int getMaxHintsForCounterparts() {
    return maxHintsForCounterparts;
  }

  public boolean isCommitIndexedNeedImmediately() {
    return commitIndexedNeedImmediately;
  }

  public String getSolrServerPublicUri() {
    return solrServerPublicUri;
  }

  public int getCutAfterIthElbowInScore() {
    return cutAfterIthElbowInScore;
  }

  public float getScoreNormalizationFactor() {
    return scoreNormalizationFactor;
  }

  public String getSolrCore() {
    return solrCore;
  }

  public String getSolrTestCore() {
    return solrTestCore;
  }

  public String getSolrEndpointUri(boolean useTestCore) {
    String server = getSolrServerUri();
    if (!server.endsWith("/")) {
      server += "/";
    }

    String core = server + (useTestCore ? getSolrTestCore() : getSolrCore());
    if (!core.endsWith("/")) {
      core += "/";
    }

    return core;
  }

}

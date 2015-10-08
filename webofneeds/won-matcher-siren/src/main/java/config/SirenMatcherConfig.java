package config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.PropertySource;

/**
 * Created by hfriedrich on 15.09.2015.
 */
@Configuration
@ImportResource({"classpath:/spring/component/monitoring/monitoring-recorder.xml", "classpath:/spring/component/scheduling/matcher-service-scheduling.xml"})
@PropertySource("file:${WON_CONFIG_DIR}/matcher-siren.properties")
public class SirenMatcherConfig
{
  @Value("${matcher.siren.uri.solr.server}")
  private String solrServerUri;

  @Value("${matcher.siren.uri.solr.server.public}")
  private String solrServerPublicUri;

  @Value("${matcher.siren.query.maxHints}")
  private int maxHints;

  @Value("${matcher.siren.query.title}")
  private boolean useTitleQuery;

  @Value("${matcher.siren.query.description}")
  private boolean useDescriptionQuery;

  @Value("${matcher.siren.query.titleDescription}")
  private boolean useTitleDescriptionQuery;

  @Value("${matcher.siren.monitoring}")
  private boolean monitoring;

  @Value("${matcher.siren.query.consideredTokens}")
  private int consideredQueryTokens;

  @Value("${matcher.siren.query.titleDescriptionTag}")
  private boolean useTitleDescriptionTagQuery;

  @Value("${matcher.siren.index.commit}")
  private boolean commitIndexedNeedImmediately;

  @Value("${matcher.siren.query.scoreThreshold}")
  private float scoreThreshold;

  public float getScoreThreshold() {
    return scoreThreshold;
  }

  public void setScoreThreshold(final float scoreThreshold) {
    this.scoreThreshold = scoreThreshold;
  }

  public int getConsideredQueryTokens() {
    return consideredQueryTokens;
  }

  public void setConsideredQueryTokens(final int consideredQueryTokens) {
    this.consideredQueryTokens = consideredQueryTokens;
  }

  public boolean isUseTitleQuery() {
    return useTitleQuery;
  }

  public void setUseTitleQuery(final boolean useTitleQuery) {
    this.useTitleQuery = useTitleQuery;
  }

  public boolean isUseDescriptionQuery() {
    return useDescriptionQuery;
  }

  public void setUseDescriptionQuery(final boolean useDescriptionQuery) {
    this.useDescriptionQuery = useDescriptionQuery;
  }

  public boolean isUseTitleDescriptionQuery() {
    return useTitleDescriptionQuery;
  }

  public void setUseTitleDescriptionQuery(final boolean useTitleDescriptionQuery) {
    this.useTitleDescriptionQuery = useTitleDescriptionQuery;
  }

  public boolean isUseTitleDescriptionTagQuery() {
    return useTitleDescriptionTagQuery;
  }

  public void setUseTitleDescriptionTagQuery(final boolean useTitleDescriptionTagQuery) {
    this.useTitleDescriptionTagQuery = useTitleDescriptionTagQuery;
  }

  public String getSolrServerUri() {
    return solrServerUri;
  }

  public void setSolrServerUri(final String solrServerUri) {
    this.solrServerUri = solrServerUri;
  }

  public long getMaxHints() {
    return maxHints;
  }

  public void setMaxHints(final int maxHints) {
    this.maxHints = maxHints;
  }

  public boolean isCommitIndexedNeedImmediately() {
    return commitIndexedNeedImmediately;
  }

  public void setCommitIndexedNeedImmediately(final boolean commitIndexedNeedImmediately) {
    this.commitIndexedNeedImmediately = commitIndexedNeedImmediately;
  }

  public String getSolrServerPublicUri() {
    return solrServerPublicUri;
  }

  public void setSolrServerPublicUri(final String solrServerPublicUri) {
    this.solrServerPublicUri = solrServerPublicUri;
  }

  public boolean isMonitoring() {
    return monitoring;
  }

  public void setMonitoring(boolean monitoring) {
    this.monitoring = monitoring;
  }

}

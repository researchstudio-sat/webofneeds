package config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * Created by hfriedrich on 15.09.2015.
 */
@Configuration
@PropertySource("file:${WON_CONFIG_DIR}/matcher-siren.properties")
public class SirenMatcherConfig
{
  @Value("${matcher.siren.uri.solr.server}")
  private String solrServerUri;

  @Value("${matcher.siren.max.hints}")
  private int maxHints;

  @Value("${matcher.siren.query.title}")
  private boolean useTitleQuery;

  @Value("${matcher.siren.query.description}")
  private boolean useDescriptionQuery;

  @Value("${matcher.siren.query.titleDescription}")
  private boolean useTitleDescriptionQuery;

  @Value("${matcher.siren.query.consideredTokens}")
  private int consideredQueryTokens;

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

}

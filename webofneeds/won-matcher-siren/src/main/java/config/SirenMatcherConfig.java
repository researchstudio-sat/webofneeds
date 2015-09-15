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
  private long maxHints;

  @Value("${matcher.siren.nlpResourceDir}")
  private String nlpResourceDirectory;

  public String getSolrServerUri() {
    return solrServerUri;
  }

  public void setSolrServerUri(final String solrServerUri) {
    this.solrServerUri = solrServerUri;
  }

  public long getMaxHints() {
    return maxHints;
  }

  public void setMaxHints(final long maxHints) {
    this.maxHints = maxHints;
  }

  public String getNlpResourceDirectory() {
    return nlpResourceDirectory;
  }

  public void setNlpResourceDirectory(final String nlpResourceDirectory) {
    this.nlpResourceDirectory = nlpResourceDirectory;
  }

}

package won.matcher.sparql.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.PropertySource;

/**
 * Created by hfriedrich on 15.09.2015.
 */
@Configuration
@ImportResource({ "classpath:/spring/component/scheduling/matcher-service-scheduling.xml" })
@PropertySource("file:${WON_CONFIG_DIR}/matcher-sparql.properties")
public class SparqlMatcherConfig {
  @Value("${matcher.sparql.uri.sparql.endpoint}")
  private String sparqlEndpoint;

  @Value("${matcher.uri}")
  private String matcherUri;

  @Value("${matcher.sparql.limitResults}")
  private long limitResults;

  public long getLimitResults() {
    return limitResults;
  }

  public String getSparqlEndpoint() {
    return sparqlEndpoint;
  }

  public String getMatcherUri() {
    return matcherUri;
  }
}

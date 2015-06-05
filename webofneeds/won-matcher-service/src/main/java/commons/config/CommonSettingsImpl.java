package commons.config;

import akka.actor.Extension;
import com.typesafe.config.Config;

/**
 * Settings configuration class of the crawler
 *
 * User: hfriedrich
 * Date: 24.04.2015
 */
public class CommonSettingsImpl implements Extension
{

  public final String SPARQL_ENDPOINT;

  public CommonSettingsImpl(Config config) {
    SPARQL_ENDPOINT = config.getString("common.sparqlEndpoint");
  }
}
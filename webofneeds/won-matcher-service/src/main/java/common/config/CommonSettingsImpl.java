package common.config;

import akka.actor.Extension;
import com.typesafe.config.Config;

/**
 * Common settings configuration class
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
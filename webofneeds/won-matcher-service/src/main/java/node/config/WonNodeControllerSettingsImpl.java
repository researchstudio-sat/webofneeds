package node.config;

import akka.actor.Extension;
import com.typesafe.config.Config;

import java.util.List;

/**
 * Settings configuration class of the won node controller
 *
 * User: hfriedrich
 * Date: 24.04.2015
 */
public class WonNodeControllerSettingsImpl implements Extension
{

  public final String SPARQL_ENDPOINT;
  public final List<String> WON_NODES_CRAWL;
  public final List<String> WON_NODES_SKIP;

  public WonNodeControllerSettingsImpl(Config config) {

    SPARQL_ENDPOINT = config.getString("wonNodeController.sparqlEndpoint");
    WON_NODES_CRAWL = config.getStringList("wonNodeController.wonNode.crawl");
    WON_NODES_SKIP = config.getStringList("wonNodeController.wonNode.skip");
  }
}
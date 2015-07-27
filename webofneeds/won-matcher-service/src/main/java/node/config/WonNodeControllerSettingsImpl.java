package node.config;

import akka.actor.Extension;
import com.typesafe.config.Config;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.util.List;
import java.util.concurrent.TimeUnit;

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
  public final FiniteDuration WON_NODE_LIFE_CHECK_DURATION;

  public WonNodeControllerSettingsImpl(Config config) {

    SPARQL_ENDPOINT = config.getString("wonNodeController.sparqlEndpoint");
    WON_NODES_CRAWL = config.getStringList("wonNodeController.wonNode.crawl");
    WON_NODES_SKIP = config.getStringList("wonNodeController.wonNode.skip");
    WON_NODE_LIFE_CHECK_DURATION = Duration.create(config.getDuration(
      "wonNodeController.wonNode.lifeCheckDuration", TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS);
  }
}
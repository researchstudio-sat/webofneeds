package won.matcher.siren.spring;

import akka.actor.ActorSystem;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import won.matcher.service.common.config.ClusterConfig;
import won.matcher.service.common.spring.SpringExtension;
import won.matcher.siren.config.SirenMatcherConfig;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

/**
 * The main application configuration.
 */
@Configuration
@PropertySource({"file:${WON_CONFIG_DIR}/matcher-siren.properties",
                 "file:${WON_CONFIG_DIR}/cluster-node.properties"})
@ComponentScan({"won.matcher.siren.spring", "won.matcher.service.common.config", "won.matcher.service.common.service.http",
                "won.matcher.siren.actor", "won.matcher.siren.config", "won.matcher.siren.indexer", "won.matcher" +
                  ".siren.matcher"})
public class MatcherSirenAppConfiguration
{
  @Autowired
  private ApplicationContext applicationContext;

  @Autowired
  private ClusterConfig clusterConfig;

  @Autowired
  private SirenMatcherConfig matcherConfig;

  /**
   * Actor system singleton for this application.
   */
  @Bean
  public ActorSystem actorSystem() {

    // load the Akka configuration
    String seedNodes = "[\"akka.tcp://" + clusterConfig.getName() + "@" +
      clusterConfig.getSeedHost() + ":" + clusterConfig.getSeedPort() + "\"]";

    final Config applicationConf = ConfigFactory.load();
    final Config config = ConfigFactory.parseString("akka.cluster.seed-nodes=" + seedNodes).
      withFallback(ConfigFactory.parseString("akka.remote.netty.tcp.bind-port=" + clusterConfig.getLocalPort())).
      withFallback(ConfigFactory.parseString("akka.remote.netty.tcp.hostname=" + clusterConfig.getNodeHost())).
      withFallback(ConfigFactory.parseString("akka.remote.netty.tcp.port=" + clusterConfig.getLocalPort())).
      withFallback(ConfigFactory.parseString("akka.cluster.roles=[matcher]")).
      withFallback(ConfigFactory.load(applicationConf));

    ActorSystem system = ActorSystem.create(clusterConfig.getName(), config);
    LoggingAdapter log = Logging.getLogger(system, this);
    log.info("Using Akka system settings: " + system.settings().toString());

    // initialize the application context in the Akka Spring Extension
    SpringExtension.SpringExtProvider.get(system).initialize(applicationContext);
    return system;
  }


  /*
     HttpSolrServer is thread-safe and if you are using the following constructor,
     you *MUST* re-use the same instance for all requests.  If instances are created on
     the fly, it can cause a connection leak. The recommended practice is to keep a
     static instance of HttpSolrServer per solr server url and share it for all requests.
     See https://issues.apache.org/jira/browse/SOLR-861 for more details
 */
  @Bean
  public SolrServer getSolrServerInstance() {
    return new HttpSolrServer(matcherConfig.getSolrServerUri());
  }


  //To resolve ${} in @Value
  //found in http://www.mkyong.com/spring/spring-propertysources-example/
  @Bean
  public static PropertySourcesPlaceholderConfigurer propertyConfigInDev() {
    return new PropertySourcesPlaceholderConfigurer();
  }
}

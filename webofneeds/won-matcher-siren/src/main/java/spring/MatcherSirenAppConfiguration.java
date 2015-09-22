package spring;

import akka.actor.ActorSystem;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import common.config.ClusterConfig;
import common.spring.SpringExtension;
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
@ComponentScan({"spring", "common.config", "common.service.http", "actor", "config", "siren"})
public class MatcherSirenAppConfiguration
{
  @Autowired
  private ApplicationContext applicationContext;

  @Autowired
  private ClusterConfig clusterConfig;

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

  //To resolve ${} in @Value
  //found in http://www.mkyong.com/spring/spring-propertysources-example/
  @Bean
  public static PropertySourcesPlaceholderConfigurer propertyConfigInDev() {
    return new PropertySourcesPlaceholderConfigurer();
  }
}

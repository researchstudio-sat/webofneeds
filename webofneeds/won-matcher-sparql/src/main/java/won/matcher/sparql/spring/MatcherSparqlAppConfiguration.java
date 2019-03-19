package won.matcher.sparql.spring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import akka.actor.ActorSystem;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import won.matcher.service.common.config.ClusterConfig;
import won.matcher.service.common.spring.SpringExtension;
import won.matcher.sparql.config.SparqlMatcherConfig;

/**
 * The main application configuration.
 */
@Configuration
@PropertySource({ "file:${WON_CONFIG_DIR}/matcher-sparql.properties",
        "file:${WON_CONFIG_DIR}/cluster-node.properties" })
@ComponentScan({ "won.matcher.sparql.spring", "won.matcher.service.common.config",
        "won.matcher.service.common.service.http", "won.matcher.sparql.actor", "won.matcher.sparql.config",
        "won.matcher.sparql.index", "won.matcher.sparql" + ".query", "won.matcher.sparql.hints" })
@ImportResource({ "classpath:/spring/component/matcher-service/ehcache/spring-node-ehcache.xml",
        "classpath:/matcher-sparql-security.xml", "classpath:/spring/component/cryptographyServices.xml",
        "classpath:/spring/component/wonNodeInformationService.xml" })
public class MatcherSparqlAppConfiguration {
    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private ClusterConfig clusterConfig;

    @Autowired
    private SparqlMatcherConfig matcherConfig;

    /**
     * Actor system singleton for this application.
     */
    @Bean
    public ActorSystem actorSystem() {

        // load the Akka configuration
        String seedNodes = "[";
        for (String seed : clusterConfig.getSeedNodes()) {
            seedNodes += "\"akka.tcp://" + clusterConfig.getName() + "@" + seed.trim() + "\",";
        }
        seedNodes += "]";

        final Config applicationConf = ConfigFactory.load();
        final Config config = ConfigFactory.parseString("akka.cluster.seed-nodes=" + seedNodes)
                .withFallback(
                        ConfigFactory.parseString("akka.remote.netty.tcp.bind-port=" + clusterConfig.getLocalPort()))
                .withFallback(
                        ConfigFactory.parseString("akka.remote.netty.tcp.hostname=" + clusterConfig.getNodeHost()))
                .withFallback(ConfigFactory.parseString("akka.remote.netty.tcp.port=" + clusterConfig.getLocalPort()))
                .withFallback(ConfigFactory.parseString("akka.cluster.roles=[matcher]"))
                .withFallback(ConfigFactory.load(applicationConf));

        ActorSystem system = ActorSystem.create(clusterConfig.getName(), config);
        LoggingAdapter log = Logging.getLogger(system, this);
        log.info("Using Akka system settings: " + system.settings().toString());

        // initialize the application context in the Akka Spring Extension
        SpringExtension.SpringExtProvider.get(system).initialize(applicationContext);
        return system;
    }

    // To resolve ${} in @Value
    // found in http://www.mkyong.com/spring/spring-propertysources-example/
    @Bean
    public static PropertySourcesPlaceholderConfigurer propertyConfigInDev() {
        return new PropertySourcesPlaceholderConfigurer();
    }
}

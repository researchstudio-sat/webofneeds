package common.spring;

import akka.actor.ActorSystem;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.*;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

/**
 * The application configuration.
 */
@Configuration
@ImportResource({"classpath:/spring/component/linkeddatasource/linkeddatasource.xml",
  "classpath:/spring/component/wonNodeInformationService.xml",
  "classpath:/spring/component/ehcache/spring-node-ehcache.xml",
  "classpath:/spring/component/services/matcher-services.xml",
                 "classpath:spring/component/camel/matcher-camel.xml"})
@PropertySource("file:conf.local/matcher.properties")
@ComponentScan({"node.actor", "common.spring"})
public class AppConfiguration {

  // the application context is needed to initialize the Akka Spring Extension
  @Autowired
  private ApplicationContext applicationContext;

  /**
   * Actor system singleton for this application.
   */
  @Bean
  public ActorSystem actorSystem() {
    final Config config = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + 2551).
      withFallback(ConfigFactory.parseString("akka.cluster.roles = [core]")).withFallback(ConfigFactory.load());
    ActorSystem system = ActorSystem.create("ClusterSystem", config);

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

package common.spring;

import akka.actor.ActorSystem;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
@PropertySource("file:${WON_CONFIG_DIR}/matcher-service.properties")
@ComponentScan({"node", "common", "crawler"})
public class AppConfiguration
{
  @Value("${uri.sparql.endpoint}")
  private String sparqlEndpointUri;

  // the application context is needed to initialize the Akka Spring Extension
  @Autowired
  private ApplicationContext applicationContext;

  /**
   * Actor system singleton for this application.
   */
  @Bean
  public ActorSystem actorSystem() {

    // load the Akka configuration
    final Config applicationConf = ConfigFactory.load();
    final Config config = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + 2551).
      withFallback(ConfigFactory.parseString("akka.cluster.roles = [core]")).withFallback(
      ConfigFactory.load(applicationConf));

    ActorSystem system = ActorSystem.create("ClusterSystem", config);

    LoggingAdapter log = Logging.getLogger(system, this);
    log.info("Using Akka system settings: " + system.settings().toString());

    // initialize the application context in the Akka Spring Extension
    SpringExtension.SpringExtProvider.get(system).initialize(applicationContext);
    return system;
  }

//  @Bean
//  public CrawlSparqlService getCrawlSparqlService(){
//    return new CrawlSparqlService(this.sparqlEndpointUri);
//  }

//  @Bean
//  public HttpRequestService getHttpRequestService() {
//    //TODO: add timeouts
//    // httpRequestService = new HttpRequestService(crawlSettings.HTTP_READ_TIMEOUT, crawlSettings.HTTP_CONNECTION_TIMEOUT);
//    return new HttpRequestService();
//  }

  //To resolve ${} in @Value
  //found in http://www.mkyong.com/spring/spring-propertysources-example/
  @Bean
  public static PropertySourcesPlaceholderConfigurer propertyConfigInDev() {
    return new PropertySourcesPlaceholderConfigurer();
  }
}

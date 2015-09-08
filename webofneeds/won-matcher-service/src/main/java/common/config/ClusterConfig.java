package common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * Basic cluster configuration
 *
 * Created by hfriedrich on 07.09.2015.
 */
@Configuration
@PropertySource("file:${WON_CONFIG_DIR}/matcher-service.properties")
public class ClusterConfig
{
  @Value("${node.host}")
  private String nodeHost;

  @Value("${cluster.name}")
  private String name;

  @Value("${cluster.seed.host}")
  private String seedHost;

  @Value("${cluster.seed.port}")
  private int seedPort;

  public String getNodeHost() {
    return nodeHost;
  }

  public String getName() {
    return name;
  }

  public String getSeedHost() {
    return seedHost;
  }

  public int getSeedPort() {
    return seedPort;
  }
}

package common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * Basic cluster configuration of a component in the akka matcher service
 *
 * Created by hfriedrich on 07.09.2015.
 */
@Configuration
@PropertySource("file:${WON_CONFIG_DIR}/cluster-node.properties")
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

  @Value("${cluster.local.port}")
  private int localPort;

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

  public int getLocalPort() {
    return localPort;
  }

  public void setNodeHost(final String nodeHost) {
    this.nodeHost = nodeHost;
  }

  public void setName(final String name) {
    this.name = name;
  }

  public void setSeedHost(final String seedHost) {
    this.seedHost = seedHost;
  }

  public void setSeedPort(final int seedPort) {
    this.seedPort = seedPort;
  }

  public void setLocalPort(final int localPort) {
    this.localPort = localPort;
  }
}

package won.matcher.service.common.config;

import java.util.List;

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
public class ClusterConfig {
    @Value("${node.host}")
    private String nodeHost;

    @Value("${cluster.name}")
    private String name;

    @Value("${cluster.local.port}")
    private int localPort;

    @Value("#{'${cluster.seedNodes}'.split(',')}")
    private List<String> seedNodes;

    public String getNodeHost() {
        return nodeHost;
    }

    public String getName() {
        return name;
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

    public void setLocalPort(final int localPort) {
        this.localPort = localPort;
    }

    public List<String> getSeedNodes() {
        return seedNodes;
    }

    public void setSeedNodes(final List<String> seedNodes) {
        this.seedNodes = seedNodes;
    }

}

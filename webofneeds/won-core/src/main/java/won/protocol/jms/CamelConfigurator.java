package won.protocol.jms;

import org.apache.camel.CamelContext;
import org.apache.camel.CamelContextAware;
import won.protocol.exception.CamelConfigurationFailedException;

import java.net.URI;
import java.util.List;

/**
 * User: LEIH-NB
 * Date: 24.02.14
 */
public interface CamelConfigurator extends CamelContextAware {
    String configureCamelEndpointForNodeURI(URI wonNodeURI, URI brokerURI, String ownerProtocolQueueName) throws CamelConfigurationFailedException;

    void addRemoteQueueListeners(List<String> endpoints) throws CamelConfigurationFailedException;

    //todo: the method is activemq specific. refactor it to support other brokers.
    String addCamelComponentForWonNodeBroker(URI wonNodeURI, URI brokerURI, String ownerApplicationId);

    void addRouteForEndpoint(URI wonNodeURI) throws CamelConfigurationFailedException;

    String getStartingEndpoint(URI wonNodeURI);

    void setStartingEndpoint(URI wonNodeURI, String startingEndpoint);

    void setCamelContext(CamelContext camelContext);

    @Override
    CamelContext getCamelContext();

    String getEndpoint(URI wonNodeUri);

    void setStartingComponent(String startingComponent);

    void setComponentName(String componentName);

    void setDefaultNodeURI(String defaultNodeURI);
}

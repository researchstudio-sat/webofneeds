package won.protocol.jms;

import org.apache.camel.CamelContext;
import org.apache.camel.CamelContextAware;
import won.protocol.exception.CamelConfigurationFailedException;

import java.net.URI;

/**
 * User: LEIH-NB Date: 24.02.14
 */
public interface CamelConfigurator extends CamelContextAware {

  // todo: the method is activemq specific. refactor it to support other brokers.
  void addRouteForEndpoint(String startingEndpoint, final URI wonNodeURI) throws CamelConfigurationFailedException;

  void setCamelContext(CamelContext camelContext);

  @Override
  CamelContext getCamelContext();

  String getEndpoint(URI wonNodeUri);

  public String setupBrokerComponentName(URI brokerUri);

}

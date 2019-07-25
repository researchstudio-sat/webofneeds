/*
 * Copyright 2012 Research Studios Austria Forschungsges.m.b.H. Licensed under
 * the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable
 * law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package won.protocol.jms;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.apache.activemq.camel.component.ActiveMQComponent;
import org.apache.camel.CamelContext;
import org.apache.camel.RoutesBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import won.cryptography.ssl.MessagingContext;
import won.protocol.exception.CamelConfigurationFailedException;
import won.protocol.model.MessagingType;

import java.lang.invoke.MethodHandles;
import java.net.URI;

// import won.node.camel.routes.AtomProtocolDynamicRoutes;
/**
 * This class is responsible for creating an activemq broker to communicate with
 * a won node and for adding a route to this broker in the camel context that
 * can in the future be used to direct messages to the WoN node.
 */
public abstract class AtomBasedCamelConfiguratorImpl implements AtomProtocolCamelConfigurator {
    private BiMap<URI, String> endpointMap = HashBiMap.create();
    protected BiMap<URI, String> brokerComponentMap = HashBiMap.create();
    private String componentName;
    private final String localComponentName = "seda";
    private String vmComponentName;
    private CamelContext camelContext;
    private MessagingContext messagingContext;
    @Autowired
    protected BrokerComponentFactory brokerComponentFactory;
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Override
    public synchronized String configureCamelEndpointForAtomUri(URI wonNodeURI, URI brokerUri,
                    String atomProtocolQueueName) {
        String brokerComponentName = setupBrokerComponentName(brokerUri);
        if (!brokerComponentName.contains("brokerUri")) {
            addCamelComponentForWonNodeBroker(brokerUri, brokerComponentName);
        }
        String endpoint = brokerComponentName + ":queue:" + atomProtocolQueueName;
        endpointMap.put(wonNodeURI, endpoint);
        logger.info("endpoint of wonNodeURI {} is {}", wonNodeURI, endpointMap.get(wonNodeURI));
        return endpoint;
    }

    @Override
    public synchronized String setupBrokerComponentName(URI brokerUri) {
        return this.componentName + brokerUri.toString().replaceAll("[/:]", "");
    }

    /**
     * @param brokerUri
     * @return componentName
     */
    @Override
    public synchronized void addCamelComponentForWonNodeBroker(URI brokerUri, String brokerComponentName) {
        ActiveMQComponent activeMQComponent;
        if (camelContext.getComponent(brokerComponentName) == null) {
            activeMQComponent = (ActiveMQComponent) brokerComponentFactory.getBrokerComponent(brokerUri,
                            MessagingType.Queue, messagingContext);
            logger.info("adding activemqComponent for brokerUri {}", brokerUri);
            camelContext.addComponent(brokerComponentName, activeMQComponent);
            try {
                activeMQComponent.start();
            } catch (Exception e) {
                e.printStackTrace(); // To change body of catch statement use File | Settings | File Templates.
            }
        }
        brokerComponentMap.put(brokerUri, brokerComponentName);
    }

    @Override
    public synchronized void addRouteForEndpoint(String startingEndpoint, URI brokerUri)
                    throws CamelConfigurationFailedException {
        if (getCamelContext().getRoute(startingEndpoint) == null) {
            try {
                getCamelContext().addRoutes(createRoutesBuilder(startingEndpoint, brokerUri));
            } catch (Exception e) {
                throw new CamelConfigurationFailedException("adding route to camel context failed", e);
            }
        }
    }

    protected abstract RoutesBuilder createRoutesBuilder(final String startingComponent, final URI brokerUri);

    @Override
    public void setCamelContext(CamelContext camelContext) {
        this.camelContext = camelContext;
    }

    @Override
    public void setMessagingContext(MessagingContext messagingContext) {
        this.messagingContext = messagingContext;
    }

    public MessagingContext getMessagingContext() {
        return messagingContext;
    }

    @Override
    public CamelContext getCamelContext() {
        return this.camelContext;
    }

    @Override
    public String getEndpoint(URI nodeUri) {
        return endpointMap.get(nodeUri);
    }

    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }

    public String getComponentName() {
        return componentName;
    }

    @Override
    public String getBrokerComponentNameWithBrokerUri(URI brokerUri) {
        return brokerComponentMap.get(brokerUri);
    }
}

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
package won.owner.messaging;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.apache.camel.CamelContext;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.component.activemq.ActiveMQComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import won.cryptography.ssl.MessagingContext;
import won.owner.camel.routes.OwnerApplicationListenerRouteBuilder;
import won.owner.camel.routes.OwnerProtocolDynamicRoutes;
import won.protocol.exception.CamelConfigurationFailedException;
import won.protocol.jms.BrokerComponentFactory;
import won.protocol.jms.OwnerProtocolCamelConfigurator;
import won.protocol.model.MessagingType;
import won.protocol.repository.AtomRepository;
import won.protocol.repository.ConnectionRepository;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: LEIH-NB Date: 28.01.14
 */
public class OwnerProtocolCamelConfiguratorImpl implements OwnerProtocolCamelConfigurator {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private CamelContext camelContext;
    private MessagingContext messagingContext;
    @Autowired
    private AtomRepository atomRepository;
    @Autowired
    private ConnectionRepository connectionRepository;
    @Autowired
    private BrokerComponentFactory brokerComponentFactory;
    private BiMap<URI, String> endpointMap = HashBiMap.create();
    private Map<URI, String> startingComponentMap = new HashMap<>();
    private BiMap<URI, String> brokerComponentMap = HashBiMap.create();
    private String startingComponent;
    private String componentName;
    private String defaultNodeURI;

    protected OwnerProtocolCamelConfiguratorImpl() {
    }

    // TODO duplicate - see if can be mergerd with atombased - very similar code...
    @Override
    public synchronized final String configureCamelEndpointForNodeURI(URI wonNodeURI, URI brokerURI,
                    String ownerProtocolQueueName) throws CamelConfigurationFailedException {
        // TODO: the linked data description of the won node must be at
        // [NODE-URI]/resource
        // according to this code. This should be explicitly defined somewhere
        String brokerComponentName = setupBrokerComponentName(brokerURI);
        // addCamelComponentForWonNodeBroker(wonNodeURI,brokerURI,null);
        addCamelComponentForWonNodeBroker(brokerURI, brokerComponentName);
        // TODO: make this configurable
        String endpoint = brokerComponentName + ":queue:" + ownerProtocolQueueName;
        endpointMap.put(wonNodeURI, endpoint);
        List<String> endpointList = new ArrayList<>();
        endpointList.add(endpoint);
        logger.info("endpoint of wonNodeURI {} is {}", wonNodeURI, endpointMap.get(wonNodeURI));
        return endpointList.get(0);
    }

    @Override
    public synchronized void addRemoteQueueListener(String endpoint, URI remoteEndpoint)
                    throws CamelConfigurationFailedException {
        // sending and receiving endpoint need to have the same scheme,
        // look at WonJmsConfiguration and BrokerComponentFactory for JMS config info!
        endpoint = remoteEndpoint.getScheme() + endpoint;
        if (camelContext.hasEndpoint(endpoint) != null) {
            logger.debug("route for listening to remote queue {} already configured", remoteEndpoint);
            return;
        }
        logger.debug("Adding route for listening to remote queue {} ", endpoint);
        OwnerApplicationListenerRouteBuilder ownerApplicationListenerRouteBuilder = new OwnerApplicationListenerRouteBuilder(
                        camelContext, endpoint, remoteEndpoint);
        try {
            camelContext.addRoutes(ownerApplicationListenerRouteBuilder);
        } catch (Exception e) {
            logger.debug("adding route to camel context failed", e);
            throw new CamelConfigurationFailedException("adding route to camel context failed", e);
        }
    }

    /**
     * Scheme of the remote endpoint for which camel component has already bean
     * configured, should correspond to the scheme of the endpoints for which
     * listeners are being added. In this case, our component name can contain part
     * specific to a particular remote broker, so that they can connect to different
     * brokers without overriding each other.
     * 
     * @param endpoints
     * @param remoteEndpoint
     * @return
     */
    private List<String> adjustSchemeToRemoteEndpoint(final List<String> endpoints, final URI remoteEndpoint) {
        String remoteScheme = remoteEndpoint.getScheme();
        List<String> customSchemeEndpoints = new ArrayList<>(endpoints.size());
        for (String ep : endpoints) {
            String epScheme = URI.create(ep).getScheme();
            ep = ep.replace(epScheme, remoteScheme);
            customSchemeEndpoints.add(ep);
        }
        return customSchemeEndpoints;
    }

    // todo: the method is activemq specific. refactor it to support other brokers.
    // TODO some duplicate code between here and AtomBasedCamelConfiguratorImpl
    // (setup broker name) - i.e.
    // this method can probably be shared and owner's configurator can probably
    // extend atombased...
    public synchronized void addCamelComponentForWonNodeBroker(URI brokerURI, String brokerComponentName) {
        if (camelContext.getComponent(brokerComponentName, false) == null) {
            ActiveMQComponent activeMQComponent = (ActiveMQComponent) brokerComponentFactory
                            .getBrokerComponent(brokerURI, MessagingType.Queue, messagingContext);
            activeMQComponent.setTransacted(false);
            activeMQComponent.setUsePooledConnection(true);
            camelContext.addComponent(brokerComponentName, activeMQComponent);
            logger.info("adding component with component name {}", brokerComponentName);
            if (!brokerComponentMap.containsKey(brokerURI)) {
                brokerComponentMap.put(brokerURI, brokerComponentName);
            }
        }
    }

    @Override
    public void addRouteForEndpoint(String startingEndpoint, URI wonNodeURI) throws CamelConfigurationFailedException {
        addRouteForWoNNode(wonNodeURI);
    }

    public synchronized void addRouteForWoNNode(final URI wonNodeURI) throws CamelConfigurationFailedException {
        /**
         * there can be only one route per endpoint. Thus, consuming endpoint of each
         * route shall be unique.
         */
        // todo: using replaceAll might result in security issues. change this.
        String tempStartingComponentName = startingComponent;
        tempStartingComponentName = tempStartingComponentName + endpointMap.get(wonNodeURI).replaceAll(":", "_");
        setStartingEndpoint(wonNodeURI, tempStartingComponentName);
        if (camelContext.getComponent(tempStartingComponentName) == null
                        || camelContext.getRoute(endpointMap.get(wonNodeURI)) == null) {
            // OwnerProtocolDynamicRoutes ownerProtocolRouteBuilder = new
            // OwnerProtocolDynamicRoutes(camelContext, tempStartingComponentName);
            RoutesBuilder ownerProtocolRouteBuilder = createRoutesBuilder(tempStartingComponentName, wonNodeURI);
            try {
                camelContext.addRoutes(ownerProtocolRouteBuilder);
            } catch (Exception e) {
                throw new CamelConfigurationFailedException("adding route to camel context failed", e);
            }
        }
    }

    protected RoutesBuilder createRoutesBuilder(final String startingComponent, final URI brokerUri) {
        return new OwnerProtocolDynamicRoutes(camelContext, startingComponent);
    }

    @Override
    public String getStartingEndpoint(URI wonNodeURI) {
        return startingComponentMap.get(wonNodeURI);
    }

    @Override
    public void setStartingEndpoint(URI wonNodeURI, String startingEndpoint) {
        startingComponentMap.put(wonNodeURI, startingEndpoint);
    }

    @Override
    public CamelContext getCamelContext() {
        return camelContext;
    }

    @Override
    public void setCamelContext(CamelContext camelContext) {
        this.camelContext = camelContext;
    }

    @Override
    public void setMessagingContext(MessagingContext messagingContext) {
        this.messagingContext = messagingContext;
    }

    @Override
    public String getEndpoint(URI wonNodeUri) {
        return endpointMap.get(wonNodeUri);
    }

    // TODO: duplicate with atombasedcamelconfigimpl...
    @Override
    public String setupBrokerComponentName(URI brokerUri) {
        return this.componentName + brokerUri.toString().replaceAll("[/:]", "");
    }

    @Override
    public void setStartingComponent(String startingComponent) {
        this.startingComponent = startingComponent;
    }

    @Override
    public String getBrokerComponentName(URI brokerUri) {
        return brokerComponentMap.get(brokerUri);
    }

    @Override
    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }

    @Override
    public void setDefaultNodeURI(String defaultNodeURI) {
        this.defaultNodeURI = defaultNodeURI;
    }
}

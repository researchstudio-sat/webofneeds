/*
 * Copyright 2012  Research Studios Austria Forschungsges.m.b.H.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package won.owner.messaging;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.apache.activemq.camel.component.ActiveMQComponent;
import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import won.owner.camel.routes.OwnerApplicationListenerRouteBuilder;
import won.owner.camel.routes.OwnerProtocolDynamicRoutes;
import won.protocol.exception.CamelConfigurationFailedException;
import won.protocol.jms.BrokerComponentFactory;
import won.protocol.jms.OwnerProtocolCamelConfigurator;
import won.protocol.model.MessagingType;
import won.protocol.repository.ConnectionRepository;
import won.protocol.repository.NeedRepository;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: LEIH-NB
 * Date: 28.01.14
 */
public class OwnerProtocolCamelConfiguratorImpl implements OwnerProtocolCamelConfigurator {


    private CamelContext camelContext;

    @Autowired
    private NeedRepository needRepository;
    @Autowired
    private ConnectionRepository connectionRepository;
    @Autowired
    private BrokerComponentFactory brokerComponentFactory;

    private Logger logger = LoggerFactory.getLogger(getClass());

    private BiMap<URI,String> endpointMap = HashBiMap.create();
    private Map<URI,String> startingComponentMap = new HashMap<>();
    private BiMap<URI, String> brokerComponentMap = HashBiMap.create();

    private String startingComponent;
    private String componentName;
    private String defaultNodeURI;

    protected OwnerProtocolCamelConfiguratorImpl() {
    }

    @Override
    public synchronized final String configureCamelEndpointForNodeURI(URI wonNodeURI, URI brokerURI, String ownerProtocolQueueName) throws CamelConfigurationFailedException {
        //TODO: the linked data description of the won node must be at [NODE-URI]/resource
        // according to this code. This should be explicitly defined somewhere

        String tempComponentName = addCamelComponentForWonNodeBroker(wonNodeURI,brokerURI,null);
        //TODO: make this configurable
        String endpoint = tempComponentName+":queue:"+ownerProtocolQueueName;
        endpointMap.put(wonNodeURI,endpoint);
        List<String> endpointList = new ArrayList<>();
        endpointList.add(endpoint);
        logger.info("endpoint of wonNodeURI {} is {}",wonNodeURI,endpointMap.get(wonNodeURI));
        return endpointList.get(0);

    }
    @Override
    public synchronized void addRemoteQueueListeners(List<String> endpoints, URI remoteEndpoint) throws CamelConfigurationFailedException {
        logger.info("length of endpoints {}", endpoints.size());
        OwnerApplicationListenerRouteBuilder ownerApplicationListenerRouteBuilder = new OwnerApplicationListenerRouteBuilder(camelContext, endpoints, remoteEndpoint);
        try {
            camelContext.addRoutes(ownerApplicationListenerRouteBuilder);
        } catch (Exception e) {
            logger.debug("adding route to camel context failed", e);
            throw new CamelConfigurationFailedException("adding route to camel context failed",e);
        }
    }
    //todo: the method is activemq specific. refactor it to support other brokers.
    @Override
    public synchronized String addCamelComponentForWonNodeBroker(URI wonNodeURI, URI brokerURI, String ownerApplicationId){
        String componentName;

        if (ownerApplicationId==null){
            if (!wonNodeURI.equals(URI.create(defaultNodeURI)))
                componentName = this.componentName+brokerURI.toString().replaceAll("[/:]","");
            else
                componentName = this.componentName;
        }else
            componentName = this.componentName+ownerApplicationId;
        if(camelContext.getComponent(componentName,false)!=null){
            return componentName;
        }

        ActiveMQComponent activeMQComponent = (ActiveMQComponent) brokerComponentFactory.getBrokerComponent
          (brokerURI, MessagingType.Queue);

        camelContext.addComponent(componentName,activeMQComponent);

        logger.info("adding component with component name {}",componentName);
        if (!brokerComponentMap.containsKey(wonNodeURI))
            brokerComponentMap.put(wonNodeURI,componentName);
        return componentName;
    }

    @Override
    public synchronized void addRouteForEndpoint(String startingEndpoint, final URI wonNodeURI) throws CamelConfigurationFailedException {
        /**
         * there can be only one route per endpoint. Thus, consuming endpoint of each route shall be unique.
         */
        //todo: using replaceAll might result in security issues. change this.
        String tempStartingComponentName = startingComponent;
        tempStartingComponentName = tempStartingComponentName + endpointMap.get(wonNodeURI).replaceAll(":","_");
        //todo: make
        setStartingEndpoint(wonNodeURI, tempStartingComponentName);

        if (camelContext.getComponent(tempStartingComponentName)==null||camelContext.getRoute(endpointMap.get(wonNodeURI))==null){
            OwnerProtocolDynamicRoutes ownerProtocolRouteBuilder = new OwnerProtocolDynamicRoutes(camelContext, tempStartingComponentName);
            try {
                camelContext.addRoutes(ownerProtocolRouteBuilder);
            } catch (Exception e) {
                throw new CamelConfigurationFailedException("adding route to camel context failed",e);
            }
        }

    }
    @Override
    public String getStartingEndpoint(URI wonNodeURI){
        return startingComponentMap.get(wonNodeURI);
    }
    @Override
    public void setStartingEndpoint(URI wonNodeURI, String startingEndpoint) {
        startingComponentMap.put(wonNodeURI,startingEndpoint);

    }

    public synchronized String replaceEndpointNameWithOwnerApplicationId(String endpointName, String ownerApplicationId) throws Exception {
        Endpoint ep = camelContext.getEndpoint(endpointName);
        URI wonNodeUri = endpointMap.inverse().get(endpointName);

        camelContext.removeEndpoints(endpointName);
        String[] endpointSplit = endpointName.split(":");
        endpointSplit[0] = endpointSplit[0]+ownerApplicationId;
        endpointName = endpointSplit[0]+":"+endpointSplit[1]+":"+endpointSplit[2];

        //  endpointName = endpointName.replaceFirst(endpointName,endpointName+ownerApplicationId);
        camelContext.addEndpoint(endpointName, ep);
        endpointMap.put(wonNodeUri,endpointName);
        return endpointName;
    }

    public synchronized String replaceComponentNameWithOwnerApplicationId(String componentName, String ownerApplicationId){
        ActiveMQComponent activeMQComponent = (ActiveMQComponent)camelContext.getComponent(componentName);
        URI wonNodeUri = brokerComponentMap.inverse().get(componentName);

        camelContext.removeComponent(componentName);
        componentName = this.componentName+ownerApplicationId;
        camelContext.addComponent(componentName, activeMQComponent);
        logger.info("component name: "+componentName);
        brokerComponentMap.put(wonNodeUri, componentName);
        return componentName;
    }

    @Override
    public void setCamelContext(CamelContext camelContext) {
        this.camelContext = camelContext;
    }

    @Override
    public CamelContext getCamelContext() {
        return camelContext;
    }

    @Override
    public String getEndpoint(URI wonNodeUri){
       return endpointMap.get(wonNodeUri);
    }

    //TODO: not implemented yet
    @Override
    public String setupBrokerComponentName(URI brokerUri) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }


    @Override
    public void setStartingComponent(String startingComponent) {
        this.startingComponent = startingComponent;
    }

    @Override
    public String getBrokerComponent(URI resourceUri) {
        return brokerComponentMap.get(resourceUri);
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

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

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.camel.component.ActiveMQComponent;
import org.apache.camel.CamelContext;
import org.apache.camel.CamelContextAware;
import org.apache.camel.Endpoint;
import org.apache.camel.component.jms.JmsConfiguration;
import org.apache.camel.impl.UriEndpointConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.connection.CachingConnectionFactory;
import won.owner.camel.routes.OwnerProtocolDynamicRoutes;
import won.protocol.exception.CamelConfigurationFailedException;
import won.protocol.model.Connection;
import won.protocol.model.Need;
import won.protocol.repository.ConnectionRepository;
import won.protocol.repository.NeedRepository;
import won.protocol.util.DataAccessUtils;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.activemq.camel.component.ActiveMQComponent.activeMQComponent;

/**
 * User: LEIH-NB
 * Date: 28.01.14
 */
public class CamelConfigurator implements CamelContextAware{

    private CamelContext camelContext;

    @Autowired
    private NeedRepository needRepository;
    @Autowired
    private ConnectionRepository connectionRepository;

    private Logger logger = LoggerFactory.getLogger(getClass());

    private Map<URI,String> endpointMap = new HashMap<>();
    private Map<URI,String> startingComponentMap = new HashMap<>();
    private Map<URI, String> brokerComponentMap = new HashMap<>();

    private String startingComponent;
    private String componentName;
    private String defaultNodeURI;


    protected CamelConfigurator() {
    }

    public synchronized final String configureCamelEndpointForNeed(URI needURI, URI brokerURI,String ownerProtocolQueueName) throws Exception {
        Need need = needRepository.findByNeedURI(needURI).get(0);
        URI wonNodeURI = need.getWonNodeURI();
        return configureCamelEndpointForNodeURI(wonNodeURI, brokerURI,ownerProtocolQueueName);

    }
    public synchronized final String configureCamelEndpointForConnection(URI connectionURI, URI brokerURI, String ownerProtocolQueueName) throws Exception {

        Connection con = DataAccessUtils.loadConnection(connectionRepository, connectionURI);
        URI needURI = con.getNeedURI();
        return configureCamelEndpointForNeed(needURI,brokerURI, ownerProtocolQueueName);

    }
    public synchronized final String configureCamelEndpointForNodeURI(URI wonNodeURI, URI brokerURI,String ownerProtocolQueueName) throws CamelConfigurationFailedException {
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
    //todo: the method is activemq specific. refactor it to support other brokers.
    public synchronized String addCamelComponentForWonNodeBroker(URI wonNodeURI, URI brokerURI,String ownerApplicationId){
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
        ActiveMQConnectionFactory activeMQConnectionFactory = new ActiveMQConnectionFactory(brokerURI);
        CachingConnectionFactory cachingConnectionFactory = new CachingConnectionFactory(activeMQConnectionFactory);
        JmsConfiguration jmsConfiguration = new JmsConfiguration(cachingConnectionFactory);
        ActiveMQComponent activeMQComponent = activeMQComponent();

        activeMQComponent.setConfiguration(jmsConfiguration);
        camelContext.addComponent(componentName,activeMQComponent);


        logger.info("adding component with component name {}",componentName);
        if (!brokerComponentMap.containsKey(wonNodeURI))
            brokerComponentMap.put(wonNodeURI,componentName);
        return componentName;
    }

    public synchronized void addRouteForEndpoint(URI wonNodeURI) throws CamelConfigurationFailedException {
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
    public String getStartingEndpoint(URI wonNodeURI){
        return startingComponentMap.get(wonNodeURI);
    }
    public void setStartingEndpoint(URI wonNodeURI, String startingEndpoint) {
        startingComponentMap.put(wonNodeURI,startingEndpoint);

    }
    public synchronized String replaceEndpointNameWithOwnerApplicationId(String endpointName, String ownerApplicationId) throws Exception {
        Endpoint ep = camelContext.getEndpoint(endpointName);
        camelContext.removeEndpoints(endpointName);
        String[] endpointSplit = endpointName.split(":");
        endpointSplit[0] = endpointSplit[0]+ownerApplicationId;
        endpointName = endpointSplit[0]+":"+endpointSplit[1]+":"+endpointSplit[2];
        //  endpointName = endpointName.replaceFirst(endpointName,endpointName+ownerApplicationId);
        camelContext.addEndpoint(endpointName, ep);
        return endpointName;
    }
    public synchronized String replaceComponentNameWithOwnerApplicationId(String componentName, String ownerApplicationId){
        ActiveMQComponent activeMQComponent = (ActiveMQComponent)camelContext.getComponent(componentName);
        camelContext.removeComponent(componentName);
        componentName = this.componentName+ownerApplicationId;
        camelContext.addComponent(componentName, activeMQComponent);
        return componentName;
    }

    public void setCamelContext(CamelContext camelContext) {
        this.camelContext = camelContext;
    }

    @Override
    public CamelContext getCamelContext() {
        return camelContext;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getEndpoint(URI wonNodeUri){
       return endpointMap.get(wonNodeUri);
    }

    public void setStartingComponent(String startingComponent) {
        this.startingComponent = startingComponent;
    }

    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }

    public void setDefaultNodeURI(String defaultNodeURI) {
        this.defaultNodeURI = defaultNodeURI;
    }
}

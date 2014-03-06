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

package won.node.messaging;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.apache.activemq.camel.component.ActiveMQComponent;
import org.apache.camel.CamelContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import won.node.camel.routes.NeedProtocolDynamicRoutes;
import won.protocol.exception.CamelConfigurationFailedException;
import won.protocol.jms.BrokerComponentFactory;
import won.protocol.jms.NeedProtocolCamelConfigurator;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: LEIH-NB
 * Date: 26.02.14
 */
public class NeedProtocolCamelConfiguratorImpl implements NeedProtocolCamelConfigurator {

    private BiMap<URI, String> endpointMap = HashBiMap.create();
    private BiMap<URI,String> brokerComponentMap = HashBiMap.create();
    private String componentName;
    private final String localComponentName = "seda";
    private String vmComponentName;
    private CamelContext camelContext;

    @Autowired
    private BrokerComponentFactory brokerComponentFactory;
    private Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public synchronized String configureCamelEndpointForNeedUri(URI brokerUri, String needProtocolQueueName){
        String brokerComponentName = setupBrokerComponentName(brokerUri);
        addCamelComponentForWonNodeBroker(brokerUri, brokerComponentName);
        String endpoint = brokerComponentName+":queue:"+needProtocolQueueName;
        endpointMap.put(brokerUri,endpoint);
        logger.info("endpoint of wonNodeURI {} is {}",brokerUri,endpointMap.get(brokerUri));
        return endpoint;
    }

    @Override
    public synchronized String setupBrokerComponentName(URI brokerUri){
            return this.componentName+brokerUri.toString().replaceAll("[/:]","");
    }
    /**
     *
     * @param brokerUri
     * @return componentName
     */
    @Override
    public synchronized void addCamelComponentForWonNodeBroker(URI brokerUri,String brokerComponentName){

        ActiveMQComponent activeMQComponent;
        if (camelContext.getComponent(brokerComponentName)==null){
            activeMQComponent = (ActiveMQComponent) brokerComponentFactory.getBrokerComponent(brokerUri);
            logger.info("adding activemqComponent for brokerUri {}",brokerUri);
            camelContext.addComponent(brokerComponentName,activeMQComponent);
        }
        brokerComponentMap.put(brokerUri,brokerComponentName);
    }

    @Override
    public synchronized void addRouteForEndpoint(String startingComponent,URI brokerUri) throws CamelConfigurationFailedException {

        if (camelContext.getComponent(startingComponent)==null||camelContext.getRoute(startingComponent)==null){
            NeedProtocolDynamicRoutes needProtocolRouteBuilder = new NeedProtocolDynamicRoutes(camelContext,startingComponent);
            try {
                camelContext.addRoutes(needProtocolRouteBuilder);
            } catch (Exception e) {
                throw new CamelConfigurationFailedException("adding route to camel context failed",e);
            }
        }
    }

    @Override
    public void setCamelContext(CamelContext camelContext) {
        this.camelContext=camelContext;
    }

    @Override
    public CamelContext getCamelContext() {
        return this.camelContext;
    }


    @Override
    public String getEndpoint(URI brokerUri) {
        return  endpointMap.get(brokerUri);
    }
    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }

    public String getComponentName() {
        return componentName;
    }

    @Override
    public String getBrokerComponentNameWithBrokerUri(URI brokerUri){
        return brokerComponentMap.get(brokerUri);
    }





}

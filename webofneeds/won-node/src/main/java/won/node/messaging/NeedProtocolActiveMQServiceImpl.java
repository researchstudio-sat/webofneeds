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

import com.fasterxml.jackson.databind.util.LRUMap;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.camel.component.ActiveMQComponent;
import org.apache.camel.CamelContext;
import org.apache.camel.CamelContextAware;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import won.node.camel.routes.NeedProtocolDynamicRoutes;
import won.protocol.jms.ActiveMQService;
import won.protocol.jms.NeedProtocolActiveMQService;
import won.protocol.model.Connection;
import won.protocol.repository.ConnectionRepository;
import won.protocol.rest.LinkedDataRestClient;
import won.protocol.util.DataAccessUtils;
import won.protocol.vocabulary.WON;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static org.apache.activemq.camel.component.ActiveMQComponent.activeMQComponent;

/**
 * User: sbyim
 * Date: 26.11.13
 */
//TODO: devide this class into two classes, OwnerProtocolActiveMQServiceImpl and NeedProtocolActiveMQServiceImpl
public class NeedProtocolActiveMQServiceImpl implements ApplicationContextAware,CamelContextAware,NeedProtocolActiveMQService {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    public static final String PATH_BROKER_URI = "<" + WON.SUPPORTS_WON_PROTOCOL_IMPL + ">/<" + WON.HAS_BROKER_URI + ">";
    public static final String PATH_NEED_PROTOCOL_QUEUE_NAME = "<" + WON.SUPPORTS_WON_PROTOCOL_IMPL + ">/<" + WON.HAS_ACTIVEMQ_NEED_PROTOCOL_QUEUE_NAME + ">";
    private ApplicationContext applicationContext;
    private CamelContext camelContext;
    private String componentName;
    private String endpoint;

    public ActiveMQService getActiveMQService() {
        return activeMQService;
    }

    public void setActiveMQService(ActiveMQService activeMQService) {
        this.activeMQService = activeMQService;
    }

    private ActiveMQService activeMQService;

    private LRUMap<URI, String> knownBrokersByNeed = new LRUMap<URI, String>(10,1000);
    @Autowired
    private LinkedDataRestClient linkedDataRestClient;

    @Autowired
    private ConnectionRepository connectionRepository;

    @Override
    public void setCamelContext(CamelContext camelContext) {
        this.camelContext = camelContext;
    }

    @Override
    public CamelContext getCamelContext() {
        return camelContext;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getComponentName() {
        return componentName;
    }

    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }
    //todo: starting endpoint is currently only needed by ownerprotocol.


    public URI getBrokerURI() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    /**
     *
     *
     *
     * @param needURI
     * @param otherNeedURI
     * @param from
     * @throws Exception
     */
    public String getCamelEndpointForNeed(URI needURI, URI otherNeedURI, String from) throws Exception {
        logger.info("fetching camel endpoint for need {}, remote need {}, from {}", new Object[]{needURI, otherNeedURI, from});
        List<String> endpointList = new ArrayList<>();
        String endpoint = knownBrokersByNeed.get(otherNeedURI);
        logger.info("cached endpoint for need {},is {}", otherNeedURI, endpoint);
        if (endpoint != null){
            endpointList.add(endpoint);

            addRouteForEndpoint(camelContext,from);
            return endpoint;
        }
        if (camelContext.getEndpoint(from)!=null){
            logger.debug("getting activemq brokerURI for node with otherneed URI {}",otherNeedURI);
            URI remoteBrokerURI = activeMQService.getBrokerURI(otherNeedURI);
            URI ownBrokerURI = activeMQService.getBrokerURI(needURI);
            String tempComponentName = componentName;


            if (!remoteBrokerURI.equals(ownBrokerURI)){
                logger.debug("remote broker different from local broker");
                 //TODO: implement register method for node-to-node communication
                 //TODO: -> registration for node-to-node messaging is not needed at the moment. 20131210

                tempComponentName = componentName+remoteBrokerURI.toString().replaceAll("[/:]","");
                    if (camelContext.getComponent(tempComponentName)==null){
                        //TODO: check configuration of activemq component. e.g. using cachedConnectionFactory
                        ActiveMQComponent activeMQComponent = new ActiveMQComponent();
                        activeMQComponent.setAutoStartup(true);
                        activeMQComponent.setApplicationContext(applicationContext);
                        activeMQComponent.setCamelContext(camelContext);

                        camelContext.addComponent(tempComponentName,activeMQComponent(remoteBrokerURI.toString()+"?useLocalHost=false"));
                    }

            }

            endpoint = tempComponentName+":queue:"+activeMQService.getProtocolQueueNameWithResource(needURI);
            logger.debug("created endpoint: {} for need {}", endpoint, otherNeedURI);

            endpointList.add(endpoint);
            ActiveMQConnectionFactory activemqConnectionFactory = (ActiveMQConnectionFactory) applicationContext.getBean("activemqConnectionFactory");

            addRouteForEndpoint(camelContext,from);

            this.knownBrokersByNeed.put(otherNeedURI, endpoint);
            return endpoint;
        }  else {
           throw new IllegalArgumentException("tried to get endpoint for remote need "+otherNeedURI+"but 'from' argument was inappropriate ("+ from +")");

        }
    }

    private void addRouteForEndpoint(CamelContext camelContext,String from) throws Exception {
        NeedProtocolDynamicRoutes needProtocolRouteBuilder = new NeedProtocolDynamicRoutes(camelContext, from);
        addRoutes(needProtocolRouteBuilder);
    }

    @Override
    public String getActiveMQNeedProtocolQueueNameForNeed(URI needURI) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }


    public String getCamelEndpointForConnection(URI connectionURI, String from) throws Exception {
        if (camelContext.getEndpoint(from)!=null){
            Connection con = DataAccessUtils.loadConnection(connectionRepository, connectionURI);

            URI needURI = con.getNeedURI();
            URI otherNeedURI = con.getRemoteNeedURI();
            logger.debug("getting camel endpoint for needs {} and {}",needURI, otherNeedURI);
            return getCamelEndpointForNeed(needURI, otherNeedURI, from);
        }
        return  null;
    }


    public void addRoutes(RouteBuilder route) throws Exception {
        camelContext.addRoutes(route);
    }

    public void setLinkedDataRestClient(LinkedDataRestClient linkedDataRestClient) {
        this.linkedDataRestClient = linkedDataRestClient;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public URI getBrokerURI(URI resourceUri) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}

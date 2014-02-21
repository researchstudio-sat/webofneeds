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
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.sparql.path.Path;
import com.hp.hpl.jena.sparql.path.PathParser;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
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
import won.protocol.jms.NeedProtocolActiveMQService;
import won.protocol.model.Connection;
import won.protocol.repository.ConnectionRepository;
import won.protocol.rest.LinkedDataRestClient;
import won.protocol.util.DataAccessUtils;
import won.protocol.vocabulary.WON;

import javax.ws.rs.core.Response;
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




    public URI getBrokerURIForNode(URI needURI){
        URI activeMQEndpoint = null;
        try{
            logger.debug("fetching Broker URI for need {}", needURI);
            Path path = PathParser.parse(PATH_BROKER_URI, PrefixMapping.Standard);
            activeMQEndpoint = linkedDataRestClient.getURIPropertyForPropertyPath(needURI, path);
        } catch (UniformInterfaceException e){
            ClientResponse response = e.getResponse();
            if (response.getStatus() == Response.Status.NOT_FOUND.getStatusCode()){
                return null;
            }
            else throw e;
        }
        logger.debug("fetched Broker URI for need {}: {}", needURI,activeMQEndpoint);
        return activeMQEndpoint;
    }

    public URI getBrokerURI() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getActiveMQNeedProtocolQueueNameForNeed(URI needURI){
        String activeMQNeedProtocolQueueName = null;
        try{
            logger.debug("fetching queue name for need {}", needURI);
            Path path = PathParser.parse(PATH_NEED_PROTOCOL_QUEUE_NAME, PrefixMapping.Standard);
            activeMQNeedProtocolQueueName = linkedDataRestClient.getStringPropertyForPropertyPath(needURI, path);
        } catch (UniformInterfaceException e){
            ClientResponse response = e.getResponse();
            if (response.getStatus() == Response.Status.NOT_FOUND.getStatusCode()){
                return null;
            }
            else throw e;
        }
        logger.debug("fetched queue name for need {}: {}", needURI,activeMQNeedProtocolQueueName);
        return activeMQNeedProtocolQueueName;
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
            // tempComponentName = tempComponentName+endpoint.replaceAll(":","_");
            //ActiveMQConnectionFactory activemqConnectionFactory = (ActiveMQConnectionFactory) applicationContext.getBean("activemqConnectionFactory");

            NeedProtocolDynamicRoutes needProtocolRouteBuilder = new NeedProtocolDynamicRoutes(camelContext,endpointList,from);
            addRoutes(needProtocolRouteBuilder);
            return endpoint;
        }
        if (camelContext.getEndpoint(from)!=null){
            logger.debug("getting activemq brokerURI for node with otherneed URI {}",otherNeedURI);
            URI remoteBrokerURI = getBrokerURIForNode(otherNeedURI);
            URI ownBrokerURI = getBrokerURIForNode(needURI);
            String tempComponentName = componentName;


            if (!remoteBrokerURI.equals(ownBrokerURI)){
                logger.debug("remote broker different from local broker");
                 //TODO: implement register method for node-to-node communication
                 //TODO: -> registration for node-to-node messaging is not needed at the moment. 20131210

                tempComponentName = componentName+remoteBrokerURI.toString().replaceAll("[/:]","");
                //if (camelContext.getComponent(tempComponentName)==null)   {
                    if (camelContext.getComponent(tempComponentName)==null){
                        //TODO: check configuration of activemq component. e.g. using cachedConnectionFactory
                        ActiveMQComponent activeMQComponent = new ActiveMQComponent();
                        activeMQComponent.setAutoStartup(true);
                        activeMQComponent.setApplicationContext(applicationContext);
                        activeMQComponent.setCamelContext(camelContext);
                        //camelContext.addComponent(tempComponentName,activeMQComponent(remoteBrokerURI.toString()+"?useLocalHost=false"));

                        camelContext.addComponent(tempComponentName,activeMQComponent(remoteBrokerURI.toString()+"?useLocalHost=false"));
                    }

               // }
            }

            //endpoint = tempComponentName+":queue:"+getActiveMQNeedProtocolQueueNameForNeed(needURI);
            endpoint = tempComponentName+":queue:"+getActiveMQNeedProtocolQueueNameForNeed(needURI);
            logger.debug("created endpoint: {} for need {}", endpoint, otherNeedURI);

            endpointList.add(endpoint);
           // tempComponentName = tempComponentName+endpoint.replaceAll(":","_");
            ActiveMQConnectionFactory activemqConnectionFactory = (ActiveMQConnectionFactory) applicationContext.getBean("activemqConnectionFactory");

            NeedProtocolDynamicRoutes needProtocolRouteBuilder = new NeedProtocolDynamicRoutes(camelContext,endpointList,from);
            addRoutes(needProtocolRouteBuilder);
            this.knownBrokersByNeed.put(otherNeedURI, endpoint);
            return endpoint;
        }  else {
           throw new IllegalArgumentException("tried to get endpoint for remote need "+otherNeedURI+"but 'from' argument was inappropriate ("+ from +")");

        }
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
    public URI getBrokerURI(URI wonNodeUri) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}

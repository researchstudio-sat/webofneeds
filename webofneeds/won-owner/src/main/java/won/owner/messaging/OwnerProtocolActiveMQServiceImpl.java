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

import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.sparql.path.Path;
import com.hp.hpl.jena.sparql.path.PathParser;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.camel.component.ActiveMQComponent;
import org.apache.camel.CamelContext;
import org.apache.camel.CamelContextAware;
import org.apache.camel.Endpoint;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jms.JmsConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.connection.CachingConnectionFactory;
import won.owner.camel.routes.OwnerProtocolDynamicRoutes;
import won.protocol.exception.CamelConfigurationFailedException;
import won.protocol.exception.NoSuchConnectionException;
import won.protocol.jms.OwnerProtocolActiveMQService;
import won.protocol.model.Connection;
import won.protocol.model.Need;
import won.protocol.repository.ConnectionRepository;
import won.protocol.repository.NeedRepository;
import won.protocol.rest.LinkedDataRestClient;
import won.protocol.util.DataAccessUtils;
import won.protocol.vocabulary.WON;

import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.activemq.camel.component.ActiveMQComponent.activeMQComponent;

/**
 * User: sbyim
 * Date: 28.11.13
 */
public class OwnerProtocolActiveMQServiceImpl implements OwnerApplicationListener,CamelContextAware,OwnerProtocolActiveMQService {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private CamelContext camelContext;
    private String componentName;
    private String startingComponent;
    private String defaultNodeURI;
    public static final String PATH_OWNER_PROTOCOL_QUEUE_NAME = "<" + WON.SUPPORTS_WON_PROTOCOL_IMPL + ">/<" + WON.HAS_ACTIVEMQ_OWNER_PROTOCOL_QUEUE_NAME + ">";
    @Autowired
    private LinkedDataRestClient linkedDataRestClient;
    public static final String PATH_BROKER_URI = "<" + WON.SUPPORTS_WON_PROTOCOL_IMPL + ">/<" + WON.HAS_BROKER_URI + ">";
    private Map<URI,String> endpointMap = new HashMap();
    private Map<URI,String> startingComponentMap = new HashMap<>();
    private Map<URI, String> brokerComponentMap = new HashMap<>();

    @Autowired
    private ConnectionRepository connectionRepository;
    @Autowired
    private NeedRepository needRepository;

    @Override
    public String getActiveMQOwnerProtocolQueueNameForNeed(URI needURI){
        String activeMQOwnerProtocolQueueName = null;
        try{
            Path path = PathParser.parse(PATH_OWNER_PROTOCOL_QUEUE_NAME, PrefixMapping.Standard);
            activeMQOwnerProtocolQueueName = linkedDataRestClient.getStringPropertyForPropertyPath(needURI, path);
        } catch (UniformInterfaceException e){
            ClientResponse response = e.getResponse();
            if (response.getStatus() == Response.Status.NOT_FOUND.getStatusCode()){
                return null;
            }
            else throw e;
        }

        return activeMQOwnerProtocolQueueName;
    }

    /**
     * this method is used for owner-node endpoint negotiation.
     * using the wonNodeURI, it retrieves brokerURI.
     * if the won node URI, to which the endpoint shall be configured is different from default node URI,
     * a new activemq component with unique name shall be generated and added into camel context.
     * Endpoint is configured using component name and remote queue name.
     * When broker and endpoint are configured, new routes shall be generated for them.
     * @param wonNodeURI
     * @param from
     * @return returns brokerURI of wonNode
     * @throws Exception
     */
    public URI configureCamelEndpointForNodeURI(URI wonNodeURI, String from) throws CamelConfigurationFailedException {
        //TODO: the linked data description of the won node must be at [NODE-URI]/resource
        // according to this code. This should be explicitly defined somewhere
        URI resourceURI = URI.create(wonNodeURI.toString()+"/resource");
        URI brokerURI = getActiveMQBrokerURIForNode(resourceURI);

        String tempComponentName = componentName;
        String tempStartingComponentName = startingComponent;
        tempComponentName = addCamelComponentForWonNodeBroker(tempComponentName,wonNodeURI,brokerURI,null);

        String endpoint = tempComponentName+":queue:"+getActiveMQOwnerProtocolQueueNameForNeed(resourceURI);
        endpointMap.put(wonNodeURI,endpoint);


        List<String> endpointList = new ArrayList<>();
        endpointList.add(endpoint);
        /**
         * there can be only one route per endpoint. Thus, consuming endpoint of each route shall be unique.
         */
        //todo: using replaceAll might result in security issues. change this.
        logger.info("endpoint of wonNodeURI {} is {}",wonNodeURI,endpointMap.get(wonNodeURI));
        tempStartingComponentName = tempStartingComponentName + endpointMap.get(wonNodeURI).replaceAll(":","_");
        //todo: make
        setStartingEndpoint(wonNodeURI, tempStartingComponentName);


        if (camelContext.getComponent(tempComponentName)==null){
            addRouteForEndpoint(camelContext,endpointList,tempStartingComponentName);
        } else{
            if(camelContext.getRoute(endpointMap.get(wonNodeURI))==null)
            {
                addRouteForEndpoint(camelContext, endpointList, tempStartingComponentName);
            }
        }

        return brokerURI;


    }
    public void addRouteForEndpoint(CamelContext camelContext, List<String> endpointList, String startingComponentName) throws CamelConfigurationFailedException {
        OwnerProtocolDynamicRoutes ownerProtocolRouteBuilder = new OwnerProtocolDynamicRoutes(camelContext,endpointList,startingComponentName);
        addRoutes(ownerProtocolRouteBuilder);
    }
    public String addCamelComponentForWonNodeBroker(String componentName, URI wonNodeURI, URI brokerURI,String ownerApplicationId){
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
        brokerComponentMap.put(wonNodeURI,componentName);
        return componentName;
    }
    public String replaceEndpointNameWithOwnerApplicationId(String endpointName, String ownerApplicationId) throws Exception {
        Endpoint ep = camelContext.getEndpoint(endpointName);
        camelContext.removeEndpoints(endpointName);
        String[] endpointSplit = endpointName.split(":");
        endpointSplit[0] = endpointSplit[0]+ownerApplicationId;
        endpointName = endpointSplit[0]+":"+endpointSplit[1]+":"+endpointSplit[2];

      //  endpointName = endpointName.replaceFirst(endpointName,endpointName+ownerApplicationId);

        camelContext.addEndpoint(endpointName, ep);
        return endpointName;
    }
    public String replaceComponentNameWithOwnerApplicationId(String componentName, String ownerApplicationId){
        ActiveMQComponent activeMQComponent = (ActiveMQComponent)camelContext.getComponent(componentName);
        camelContext.removeComponent(componentName);
        componentName = this.componentName+ownerApplicationId;

        camelContext.addComponent(componentName, activeMQComponent);

        return componentName;
    }
    public String configureCamelEndpointForConnection(URI connectionURI, String from) throws Exception {

        Connection con = DataAccessUtils.loadConnection(connectionRepository, connectionURI);
        URI needURI = con.getNeedURI();
        return configureCamelEndpointForNeed(needURI,from);

    }
    public URI getOwnWonNodeUriWithConnection(URI connectionURI) throws NoSuchConnectionException {
        Connection con = DataAccessUtils.loadConnection(connectionRepository, connectionURI);
        URI needURI = con.getNeedURI();

        return getOwnWonNodeUriWithNeed(needURI);
    }

    public URI getOwnWonNodeUriWithNeed(URI needURI){
        Need need = needRepository.findByNeedURI(needURI).get(0);
        URI wonNodeURI = need.getWonNodeURI();
        return wonNodeURI;
    }
    public String configureCamelEndpointForNeed(URI needURI, String from) throws Exception {

        Need need = needRepository.findByNeedURI(needURI).get(0);
        URI wonNodeURI = need.getWonNodeURI();
        configureCamelEndpointForNodeURI(wonNodeURI, from);
        return getEndpoint(wonNodeURI);
    }

    public String getEndpoint(URI wonNodeURI) {

        return endpointMap.get(wonNodeURI);
    }


    @Override
    public URI getActiveMQBrokerURIForNode(URI nodeURI) {
        logger.debug("obtaining broker URI for node {}", nodeURI);
        String nodeInformationPath = nodeURI.toString();
        URI activeMQEndpoint = null;
        try{

            Path path = PathParser.parse(PATH_BROKER_URI, PrefixMapping.Standard);
            activeMQEndpoint = linkedDataRestClient.getURIPropertyForPropertyPath(URI.create(nodeInformationPath), path);
        } catch (UniformInterfaceException e){
            ClientResponse response = e.getResponse();
            if (response.getStatus() == Response.Status.NOT_FOUND.getStatusCode()){
                logger.warn("BrokerURI not found for node URI:{]",nodeURI);
                return null;
            }
            else throw e;
        }

        return activeMQEndpoint;
    }

    @Override
    public void addRoutes(RouteBuilder route) throws CamelConfigurationFailedException {


        try {
            camelContext.addRoutes(route);
        } catch (Exception e) {
            throw new CamelConfigurationFailedException("adding route to camel context failed",e);
        }
    }

    @Override
    public void setCamelContext(CamelContext camelContext) {
        this.camelContext = camelContext;
    }

    @Override
    public CamelContext getCamelContext() {
        return this.camelContext;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getBrokerComponentNameForWonNode(URI wonNodeURI){
        return brokerComponentMap.get(wonNodeURI);
    }

    public String getComponentName() {
        return componentName;
    }

    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }

    public String getDefaultNodeURI() {
        return defaultNodeURI;
    }

    public void setDefaultNodeURI(String defaultNodeURI) {
        this.defaultNodeURI = defaultNodeURI;
    }

    public void setStartingComponent(String startingComponent) {
        this.startingComponent = startingComponent;
    }
    public String getStartingComponent(URI wonNodeURI){
        return startingComponentMap.get(wonNodeURI);
    }



    public void setStartingEndpoint(URI wonNodeURI, String startingEndpoint) {
        startingComponentMap.put(wonNodeURI,startingEndpoint);

    }
}

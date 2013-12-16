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
import org.apache.camel.CamelContext;
import org.apache.camel.CamelContextAware;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import won.owner.camel.routes.OwnerProtocolDynamicRoutes;
import won.protocol.exception.BrokerConfigurationFailedException;
import won.protocol.exception.CamelConfigurationFailedException;
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
import java.util.List;

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
    private String endpoint;
    private String startingEndpoint;
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
        if (!wonNodeURI.equals(URI.create(defaultNodeURI))){
            tempComponentName = componentName+brokerURI.toString().replaceAll("[/:]","");
           // from = from+wonNodeURI;
            //todo: component may already exist.
            camelContext.addComponent(tempComponentName,activeMQComponent(brokerURI.toString()));
            logger.info("adding component with component name {}",tempComponentName);
        }

        endpoint = tempComponentName+":queue:"+getActiveMQOwnerProtocolQueueNameForNeed(resourceURI);

        List<String> endpointList = new ArrayList<>();
        endpointList.add(endpoint);
        /**
         * there can be only one route per endpoint. Thus, consuming endpoint of each route shall be unique.
         */
        //todo: using replaceAll might result in security issues. change this.
        tempStartingComponentName = tempStartingComponentName + endpoint.replaceAll(":","_");
        setStartingEndpoint(tempStartingComponentName);
        if (camelContext.getComponent(tempComponentName)==null){
            OwnerProtocolDynamicRoutes ownerProtocolRouteBuilder = new OwnerProtocolDynamicRoutes(camelContext,endpointList,tempStartingComponentName);
            addRoutes(ownerProtocolRouteBuilder);
        } else{
            if(camelContext.getRoute(endpoint)==null)
            {
                OwnerProtocolDynamicRoutes ownerProtocolRouteBuilder = new OwnerProtocolDynamicRoutes(camelContext,endpointList,tempStartingComponentName);
                addRoutes(ownerProtocolRouteBuilder);
            }
        }

        return brokerURI;


    }

    public void configureCamelEndpointForConnection(URI connectionURI,String from) throws Exception {

        Connection con = DataAccessUtils.loadConnection(connectionRepository, connectionURI);
        URI needURI = con.getNeedURI();
        configureCamelEndpointForNeed(needURI,from);

    }
    public void configureCamelEndpointForNeed(URI needURI,String from) throws Exception {

        Need need = needRepository.findByNeedURI(needURI).get(0);
        URI wonNodeURI = need.getWonNodeURI();
        configureCamelEndpointForNodeURI(wonNodeURI, from);

    }

    public String getEndpoint() {
        return endpoint;
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

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
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

    public String getStartingEndpoint() {
        return startingEndpoint;
    }

    public void setStartingEndpoint(String startingEndpoint) {
        this.startingEndpoint = startingEndpoint;
    }
}

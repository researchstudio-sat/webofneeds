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

import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.sparql.path.Path;
import com.hp.hpl.jena.sparql.path.PathParser;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import org.apache.camel.CamelContext;
import org.apache.camel.CamelContextAware;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
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
public class NeedProtocolActiveMQServiceImpl implements CamelContextAware,NeedProtocolActiveMQService {

    public static final String PATH_BROKER_URI = "<" + WON.SUPPORTS_WON_PROTOCOL_IMPL + ">/<" + WON.HAS_BROKER_URI + ">";
    public static final String PATH_NEED_PROTOCOL_QUEUE_NAME = "<" + WON.SUPPORTS_WON_PROTOCOL_IMPL + ">/<" + WON.HAS_ACTIVEMQ_NEED_PROTOCOL_QUEUE_NAME + ">";

    private CamelContext camelContext;
    private String componentName;
    private String endpoint;
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
    @Override
    public String getStartingEndpoint() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }


    public URI getActiveMQBrokerURIForNode(URI needURI){
        URI activeMQEndpoint = null;
        try{

            Path path = PathParser.parse(PATH_BROKER_URI, PrefixMapping.Standard);
            activeMQEndpoint = linkedDataRestClient.getURIPropertyForPropertyPath(needURI, path);
        } catch (UniformInterfaceException e){
            ClientResponse response = e.getResponse();
            if (response.getStatus() == Response.Status.NOT_FOUND.getStatusCode()){
                return null;
            }
            else throw e;
        }

        return activeMQEndpoint;
    }

    public String getActiveMQNeedProtocolQueueNameForNeed(URI needURI){
        String activeMQNeedProtocolQueueName = null;
        try{

            Path path = PathParser.parse(PATH_NEED_PROTOCOL_QUEUE_NAME, PrefixMapping.Standard);
            activeMQNeedProtocolQueueName = linkedDataRestClient.getStringPropertyForPropertyPath(needURI, path);
        } catch (UniformInterfaceException e){
            ClientResponse response = e.getResponse();
            if (response.getStatus() == Response.Status.NOT_FOUND.getStatusCode()){
                return null;
            }
            else throw e;
        }

        return activeMQNeedProtocolQueueName;
    }



    public void configureCamelEndpointForNeeds(URI needURI, URI otherNeedURI, String from) throws Exception {
        if (camelContext.getEndpoint(from)!=null){
            URI brokerURI = getActiveMQBrokerURIForNode(needURI);
            URI remoteBrokerURI = getActiveMQBrokerURIForNode(otherNeedURI);
            URI ownBrokerURI = getActiveMQBrokerURIForNode(needURI);

            if (!remoteBrokerURI.equals(ownBrokerURI)){
                 //TODO: implement register method for node-to-node communication
                componentName = componentName+brokerURI.toString().replaceAll(":","_");
                camelContext.addComponent(componentName,activeMQComponent(brokerURI.toString()));
            }
            endpoint = componentName+":queue:"+getActiveMQNeedProtocolQueueNameForNeed(needURI);
            List<String> endpointList = new ArrayList<>();
            endpointList.add(endpoint);
            NeedProtocolDynamicRoutes needProtocolRouteBuilder = new NeedProtocolDynamicRoutes(camelContext,endpointList,from);
            addRoutes(needProtocolRouteBuilder);
        }
    }
    public void configureCamelEndpointForConnection(URI connectionURI,String from) throws Exception {
        if (camelContext.getEndpoint(from)!=null){
            Connection con = DataAccessUtils.loadConnection(connectionRepository, connectionURI);
            URI needURI = con.getNeedURI();
            URI otherNeedURI = con.getRemoteNeedURI();
            configureCamelEndpointForNeeds(needURI,otherNeedURI,from);
        }
    }


    public void addRoutes(RouteBuilder route) throws Exception {
        camelContext.addRoutes(route);
    }

    public void setLinkedDataRestClient(LinkedDataRestClient linkedDataRestClient) {
        this.linkedDataRestClient = linkedDataRestClient;
    }
}

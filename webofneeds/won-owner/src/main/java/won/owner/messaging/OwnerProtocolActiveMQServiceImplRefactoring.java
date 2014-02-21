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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import won.protocol.jms.OwnerProtocolActiveMQService;
import won.protocol.rest.LinkedDataRestClient;
import won.protocol.vocabulary.WON;

import javax.ws.rs.core.Response;
import java.net.URI;

import static org.apache.activemq.camel.component.ActiveMQComponent.activeMQComponent;

/**
 * User: sbyim
 * Date: 28.11.13
 */
public class OwnerProtocolActiveMQServiceImplRefactoring implements OwnerProtocolActiveMQService {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private static final String PATH_OWNER_PROTOCOL_QUEUE_NAME = "<" + WON.SUPPORTS_WON_PROTOCOL_IMPL + ">/<" + WON.HAS_ACTIVEMQ_OWNER_PROTOCOL_QUEUE_NAME + ">";
    private static final String PATH_BROKER_URI = "<" + WON.SUPPORTS_WON_PROTOCOL_IMPL + ">/<" + WON.HAS_BROKER_URI + ">";
    @Autowired
    private LinkedDataRestClient linkedDataRestClient;

    @Override
    public final String getOwnerProtocolQueueNameWithResource(URI wonNodeUri){
        String activeMQOwnerProtocolQueueName;
        wonNodeUri = URI.create(wonNodeUri.toString()+"/resource");
        try{
            Path path = PathParser.parse(PATH_OWNER_PROTOCOL_QUEUE_NAME, PrefixMapping.Standard);
            activeMQOwnerProtocolQueueName = linkedDataRestClient.getStringPropertyForPropertyPath(wonNodeUri, path);
        } catch (UniformInterfaceException e){
            ClientResponse response = e.getResponse();
            if (response.getStatus() == Response.Status.NOT_FOUND.getStatusCode()){
                return null;
            }
            else throw e;
        }
        return activeMQOwnerProtocolQueueName;
    }

    //todo: rename this method to getBrokerURIForNode
    public final URI getBrokerURI(URI wonNodeUri) {
        logger.debug("obtaining broker URI for node {}", wonNodeUri);
        String nodeInformationPath = URI.create(wonNodeUri.toString() + "/resource").toString();
        URI activeMQEndpoint = null;
        try{
            Path path = PathParser.parse(PATH_BROKER_URI, PrefixMapping.Standard);
            activeMQEndpoint = linkedDataRestClient.getURIPropertyForPropertyPath(URI.create(nodeInformationPath), path);
        } catch (UniformInterfaceException e){
            ClientResponse response = e.getResponse();
            if (response.getStatus() == Response.Status.NOT_FOUND.getStatusCode()){
                logger.warn("BrokerURI not found for node URI:{]", wonNodeUri);
                return null;
            }
            else throw e;
        }
        return activeMQEndpoint;
    }
}

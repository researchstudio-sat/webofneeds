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

package won.protocol.jms;

import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.sparql.path.Path;
import com.hp.hpl.jena.sparql.path.PathParser;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import won.protocol.model.ProtocolType;
import won.protocol.util.RdfUtils;
import won.protocol.util.linkeddata.LinkedDataSource;
import won.protocol.vocabulary.WON;

import javax.ws.rs.core.Response;
import java.net.URI;

/**
 * User: sbyim
 * Date: 28.11.13
 */
public class ActiveMQServiceImpl implements ActiveMQService {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private static final String PATH_OWNER_PROTOCOL_QUEUE_NAME = "<" + WON.SUPPORTS_WON_PROTOCOL_IMPL + ">/<" + WON.HAS_ACTIVEMQ_OWNER_PROTOCOL_QUEUE_NAME + ">";
    private static final String PATH_NEED_PROTOCOL_QUEUE_NAME = "<" + WON.SUPPORTS_WON_PROTOCOL_IMPL + ">/<" + WON.HAS_ACTIVEMQ_NEED_PROTOCOL_QUEUE_NAME + ">";
    private static final String PATH_BROKER_URI = "<" + WON.SUPPORTS_WON_PROTOCOL_IMPL + ">/<" + WON.HAS_BROKER_URI + ">";
    private String queueNamePath;
    private ProtocolType protocolType;


    private String pathInformation;


    public ActiveMQServiceImpl(ProtocolType type) {
        switch (type) {
            case OwnerProtocol:
                queueNamePath = PATH_OWNER_PROTOCOL_QUEUE_NAME;
                pathInformation = "/resource";
                break;
            case NeedProtocol:
                queueNamePath = PATH_NEED_PROTOCOL_QUEUE_NAME;
                pathInformation = "";
                break;
            case MatcherProtocol:
                break;

        }
        protocolType = type;

    }

    @Autowired
    private LinkedDataSource linkedDataSource;

    @Override
    public final String getProtocolQueueNameWithResource(URI resourceUri){
        String activeMQOwnerProtocolQueueName = null;
        resourceUri = URI.create(resourceUri.toString()+pathInformation);
        try{
            Path path = PathParser.parse(queueNamePath, PrefixMapping.Standard);
            activeMQOwnerProtocolQueueName = RdfUtils.getStringPropertyForPropertyPath(
                linkedDataSource.getModelForResource(resourceUri),
                resourceUri,
                path);
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
    public final URI getBrokerEndpoint(URI resourceUri) {
        logger.debug("obtaining broker URI for node {}", resourceUri);

        URI nodeInformationPath = URI.create(resourceUri.toString() + pathInformation);
        URI activeMQEndpoint = null;
        try{
            Path path = PathParser.parse(PATH_BROKER_URI, PrefixMapping.Standard);
            activeMQEndpoint = RdfUtils.getURIPropertyForPropertyPath(
                linkedDataSource.getModelForResource(nodeInformationPath),
                nodeInformationPath,
                path);
        } catch (UniformInterfaceException e){
            ClientResponse response = e.getResponse();
            if (response.getStatus() == Response.Status.NOT_FOUND.getStatusCode()){
                logger.warn("BrokerURI not found for node URI:{}", resourceUri);
                return null;
            }
            else throw e;
        }
        logger.info("brokerUri {} for resourceUri {} ",activeMQEndpoint,resourceUri);
        return activeMQEndpoint;
    }

  public void setLinkedDataSource(final LinkedDataSource linkedDataSource)
  {
    this.linkedDataSource = linkedDataSource;
  }
}

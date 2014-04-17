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

import com.hp.hpl.jena.rdf.model.*;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * User: sbyim
 * Date: 28.11.13
 */
public class ActiveMQServiceImpl implements ActiveMQService {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private static final String PATH_OWNER_PROTOCOL_QUEUE_NAME = "<" + WON.SUPPORTS_WON_PROTOCOL_IMPL + ">/<" + WON.HAS_ACTIVEMQ_OWNER_PROTOCOL_QUEUE_NAME + ">";
    private static final String PATH_NEED_PROTOCOL_QUEUE_NAME = "<" + WON.SUPPORTS_WON_PROTOCOL_IMPL + ">/<" + WON.HAS_ACTIVEMQ_NEED_PROTOCOL_QUEUE_NAME + ">";
    private static final String PATH_BROKER_URI = "<" + WON.SUPPORTS_WON_PROTOCOL_IMPL + ">/<" + WON.HAS_BROKER_URI + ">";

    protected String queueNamePath;
    private List<String> matcherProtocolTopicList;
    private ProtocolType protocolType;


    private String pathInformation;


    public ActiveMQServiceImpl(ProtocolType type) {
        switch (type) {
            case OwnerProtocol:
                queueNamePath = PATH_OWNER_PROTOCOL_QUEUE_NAME;
                break;
            case NeedProtocol:
                queueNamePath = PATH_NEED_PROTOCOL_QUEUE_NAME;
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
    try{
      logger.debug("trying to get queue name prototol type {} on resource {}", protocolType, resourceUri);
      Path path = PathParser.parse(queueNamePath, PrefixMapping.Standard);
      Model resourceModel = linkedDataSource.getModelForResource(resourceUri);
      activeMQOwnerProtocolQueueName = RdfUtils.getStringPropertyForPropertyPath(
        resourceModel,
        resourceUri,
        path);
      //check if we've found the information we were looking for
      if (activeMQOwnerProtocolQueueName != null){
        return activeMQOwnerProtocolQueueName;
      }
      logger.debug("could not to get queue name from resource {}, trying to obtain won node URI",
                   resourceUri);
      //we didnt't get the queue name. Check if the model contains a triple <baseuri> won:hasWonNode
      // <wonNode> and get the information from there.
      Resource baseResource = RdfUtils.getBaseResource(resourceModel);
      StmtIterator wonNodeStatementIterator = baseResource.listProperties(WON.HAS_WON_NODE);
      if (! wonNodeStatementIterator.hasNext()){
        //no won:hasWonNode triple found. we can't do anything.
        logger.warn("no queue name found for protocol type {} on resource {}", protocolType,resourceUri);
        return null;
      }
      Statement stmt = wonNodeStatementIterator.nextStatement();
      RDFNode wonNodeNode = stmt.getObject();
      if (!wonNodeNode.isResource()) {
        logger.warn("resource {} links to won node {} which is not a resource",
                    resourceUri,
                    wonNodeNode );
        return null;
      }
      URI wonNodeUri = URI.create(wonNodeNode.asResource().getURI().toString());
      logger.debug("obtained WON node URI: {}",wonNodeUri);
      if (wonNodeStatementIterator.hasNext()) {
        logger.warn("multiple WON node URIs found for resource {}, using first one: {} ", resourceUri, wonNodeUri );
      }
      resourceModel = linkedDataSource.getModelForResource(wonNodeUri);
      activeMQOwnerProtocolQueueName = RdfUtils.getStringPropertyForPropertyPath(
        resourceModel,
        resourceUri,
        path);
      //now, even if it's null, we return the result.
      logger.debug("returning queue name {}",activeMQOwnerProtocolQueueName);
      return activeMQOwnerProtocolQueueName;
    } catch (UniformInterfaceException e){
      logger.warn("Could not obtain data for URI:{}", resourceUri);
      ClientResponse response = e.getResponse();
      if (response.getStatus() == Response.Status.NOT_FOUND.getStatusCode()){
        return null;
      }
      else throw e;
    }
  }

  public Set<String> getMatcherProtocolTopicNamesWithResource(URI resourceURI){
    Set<String> activeMQMatcherProtocolTopicNames = new HashSet<>();
    resourceURI = URI.create(resourceURI.toString()+pathInformation);
    for (int i = 0; i< matcherProtocolTopicList.size();i++){
      try{
        Path path = PathParser.parse(matcherProtocolTopicList.get(i),PrefixMapping.Standard);
        activeMQMatcherProtocolTopicNames.add(RdfUtils.getStringPropertyForPropertyPath(
          linkedDataSource.getModelForResource(resourceURI),
          resourceURI,
          path
        ));
      }catch (UniformInterfaceException e){
        ClientResponse response = e.getResponse();
        if (response.getStatus() == Response.Status.NOT_FOUND.getStatusCode()){
          return null;
        }
        else throw e;
      }

    }
    return activeMQMatcherProtocolTopicNames;
  }
  //todo: rename this method to getBrokerURIForNode
  public final URI getBrokerEndpoint(URI resourceUri) {
    logger.debug("obtaining broker URI for node {}", resourceUri);

    URI activeMQEndpoint = null;
    try{
      logger.debug("trying to get broker endpoint for {} on resource {}", protocolType, resourceUri);
      Path path = PathParser.parse(PATH_BROKER_URI, PrefixMapping.Standard);
      Model resourceModel = linkedDataSource.getModelForResource(resourceUri);
      activeMQEndpoint = RdfUtils.getURIPropertyForPropertyPath(
        resourceModel,
        resourceUri,
        path);
      //check if we've found the information we were looking for
      if (activeMQEndpoint != null) {
        return activeMQEndpoint;
      }
      logger.debug("could not to get broker URI from resource {}, trying to obtain won node URI",
                   resourceUri);
      //we didnt't get the queue name. Check if the model contains a triple <baseuri> won:hasWonNode
      // <wonNode> and get the information from there.
      Resource baseResource = RdfUtils.getBaseResource(resourceModel);
      StmtIterator wonNodeStatementIterator = baseResource.listProperties(WON.HAS_WON_NODE);
      if (! wonNodeStatementIterator.hasNext()){
        //no won:hasWonNode triple found. we can't do anything.
        logger.warn("no broker URI found for resource {}", resourceUri);
        return null;
      }
      Statement stmt = wonNodeStatementIterator.nextStatement();
      RDFNode wonNodeNode = stmt.getObject();
      if (!wonNodeNode.isResource()) {
        logger.warn("resource {} links to won node {} which is not a resource",
                    resourceUri,
                    wonNodeNode );
        return null;
      }
      URI wonNodeUri = URI.create(wonNodeNode.asResource().getURI().toString());
      logger.debug("obtained WON node URI: {}",wonNodeUri);
      if (wonNodeStatementIterator.hasNext()) {
        logger.warn("multiple WON node URIs found for resource {}, using first one: {} ", resourceUri, wonNodeUri );
      }
      resourceModel = linkedDataSource.getModelForResource(resourceUri);
      activeMQEndpoint = RdfUtils.getURIPropertyForPropertyPath(
        resourceModel,
        resourceUri,
        path);
    } catch (UniformInterfaceException e){
      ClientResponse response = e.getResponse();
      if (response.getStatus() == Response.Status.NOT_FOUND.getStatusCode()){
        logger.warn("BrokerURI not found for node URI:{}", resourceUri);
        return null;
      }
      else throw e;
    }
    logger.debug("returning brokerUri {} for resourceUri {} ",activeMQEndpoint,resourceUri);
    return activeMQEndpoint;
  }

  public void setLinkedDataSource(final LinkedDataSource linkedDataSource)
  {
    this.linkedDataSource = linkedDataSource;
  }
}

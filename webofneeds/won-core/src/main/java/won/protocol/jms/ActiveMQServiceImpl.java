/*
 * Copyright 2012 Research Studios Austria Forschungsges.m.b.H. Licensed under
 * the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable
 * law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package won.protocol.jms;

import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.jena.query.Dataset;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.path.Path;
import org.apache.jena.sparql.path.PathParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import won.protocol.model.ProtocolType;
import won.protocol.util.RdfUtils;
import won.protocol.util.linkeddata.LinkedDataSource;
import won.protocol.util.linkeddata.WonLinkedDataUtils;
import won.protocol.vocabulary.WON;

/**
 * User: sbyim Date: 28.11.13
 */
public class ActiveMQServiceImpl implements ActiveMQService {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private static final String PATH_OWNER_PROTOCOL_QUEUE_NAME = "<" + WON.SUPPORTS_WON_PROTOCOL_IMPL + ">/<"
                    + WON.HAS_ACTIVEMQ_OWNER_PROTOCOL_QUEUE_NAME + ">";
    private static final String PATH_NEED_PROTOCOL_QUEUE_NAME = "<" + WON.SUPPORTS_WON_PROTOCOL_IMPL + ">/<"
                    + WON.HAS_ACTIVEMQ_NEED_PROTOCOL_QUEUE_NAME + ">";
    private static final String PATH_BROKER_URI = "<" + WON.SUPPORTS_WON_PROTOCOL_IMPL + ">/<" + WON.HAS_BROKER_URI
                    + ">";
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
    @Qualifier("default")
    protected LinkedDataSource linkedDataSource;

    @Override
    public final String getProtocolQueueNameWithResource(URI resourceUri) {
        String activeMQOwnerProtocolQueueName = null;
        try {
            logger.debug("trying to get queue name prototol type {} on resource {}", protocolType, resourceUri);
            Path path = PathParser.parse(queueNamePath, PrefixMapping.Standard);
            Dataset resourceDataset = linkedDataSource.getDataForResource(resourceUri);
            activeMQOwnerProtocolQueueName = RdfUtils.getStringPropertyForPropertyPath(resourceDataset, resourceUri,
                            path);
            // check if we've found the information we were looking for
            if (activeMQOwnerProtocolQueueName != null) {
                return activeMQOwnerProtocolQueueName;
            }
            logger.debug("could not to get queue name from resource {}, trying to obtain won node URI", resourceUri);
            URI wonNodeUri = WonLinkedDataUtils.getWonNodeURIForNeedOrConnection(resourceUri, resourceDataset);
            activeMQOwnerProtocolQueueName = RdfUtils.getStringPropertyForPropertyPath(
                            linkedDataSource.getDataForResource(wonNodeUri), wonNodeUri, path);
            // now, even if it's null, we return the result.
            logger.debug("returning queue name {}", activeMQOwnerProtocolQueueName);
            return activeMQOwnerProtocolQueueName;
        } catch (HttpClientErrorException e) {
            logger.warn("Could not obtain data for URI:{}", resourceUri);
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return null;
            } else
                throw e;
        }
    }

    // todo: rename this method to getBrokerURIForNode
    public final URI getBrokerEndpoint(URI resourceUri) {
        logger.debug("obtaining broker URI for node {}", resourceUri);
        URI activeMQEndpoint = null;
        try {
            logger.debug("trying to get broker endpoint for {} on resource {}", protocolType, resourceUri);
            Path path = PathParser.parse(PATH_BROKER_URI, PrefixMapping.Standard);
            Dataset resourceDataset = linkedDataSource.getDataForResource(resourceUri);
            logger.debug("ResourceModel for {}: {}", resourceUri, resourceDataset);
            activeMQEndpoint = RdfUtils.getURIPropertyForPropertyPath(resourceDataset, resourceUri, path);
            // check if we've found the information we were looking for
            if (activeMQEndpoint != null) {
                return activeMQEndpoint;
            }
            logger.debug("could not to get broker URI from resource {}, trying to obtain won node URI", resourceUri);
            // we didnt't get the queue name. Check if the model contains a triple <baseuri>
            // won:hasWonNode
            // <wonNode> and get the information from there.
            URI wonNodeUri = WonLinkedDataUtils.getWonNodeURIForNeedOrConnection(resourceUri, resourceDataset);
            logger.debug("wonNodeUri: {}", wonNodeUri);
            resourceDataset = linkedDataSource.getDataForResource(wonNodeUri);
            activeMQEndpoint = RdfUtils.getURIPropertyForPropertyPath(resourceDataset, wonNodeUri, path);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return null;
            } else
                throw e;
        }
        logger.debug("returning brokerUri {} for resourceUri {} ", activeMQEndpoint, resourceUri);
        return activeMQEndpoint;
    }

    public Set<String> getMatcherProtocolTopicNamesWithResource(URI resourceURI) {
        Set<String> activeMQMatcherProtocolTopicNames = new HashSet<>();
        resourceURI = URI.create(resourceURI.toString() + pathInformation);
        for (int i = 0; i < matcherProtocolTopicList.size(); i++) {
            try {
                Path path = PathParser.parse(matcherProtocolTopicList.get(i), PrefixMapping.Standard);
                activeMQMatcherProtocolTopicNames.add(RdfUtils.getStringPropertyForPropertyPath(
                                linkedDataSource.getDataForResource(resourceURI), resourceURI, path));
            } catch (HttpClientErrorException e) {
                if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                    return null;
                } else
                    throw e;
            }
        }
        return activeMQMatcherProtocolTopicNames;
    }

    public void setLinkedDataSource(final LinkedDataSource linkedDataSource) {
        this.linkedDataSource = linkedDataSource;
    }
}

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

package won.matcher.protocol.impl;

import org.apache.activemq.camel.component.ActiveMQComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.protocol.exception.CamelConfigurationFailedException;
import won.protocol.exception.NoSuchConnectionException;
import won.protocol.jms.*;

import java.net.URI;
import java.util.Set;

/**
 * User: LEIH-NB
 * Date: 10.03.14
 */
public class MatcherProtocolCommunicationServiceImpl implements MatcherProtocolCommunicationService {

  private MatcherProtocolCamelConfigurator matcherProtocolCamelConfigurator;

  private MatcherActiveMQService activeMQService;


  private String componentName;

  private Logger logger = LoggerFactory.getLogger(this.getClass());

  @Override
  public synchronized CamelConfiguration configureCamelEndpoint(URI needUri, String startingEndpoint) throws Exception {
    String matcherProtocolQueueName;
    CamelConfiguration camelConfiguration = new CamelConfiguration();

    URI needBrokerUri =activeMQService.getBrokerEndpoint(needUri);


    if (matcherProtocolCamelConfigurator.getBrokerComponentNameWithBrokerUri(needBrokerUri)!=null){
      if (matcherProtocolCamelConfigurator.getEndpoint(needBrokerUri)!=null)
      {
        camelConfiguration.setEndpoint(matcherProtocolCamelConfigurator.getEndpoint(needBrokerUri));
      } else {
        //matcherProtocolCamelConfigurator.addRouteForEndpoint(startingEndpoint,needBrokerUri);
        matcherProtocolQueueName = activeMQService.getProtocolQueueNameWithResource(needUri);
        camelConfiguration.setEndpoint(matcherProtocolCamelConfigurator.configureCamelEndpointForNeedUri(needBrokerUri,
                                                                                                         matcherProtocolQueueName));
      }
      camelConfiguration.setBrokerComponentName(matcherProtocolCamelConfigurator.getBrokerComponentNameWithBrokerUri(needBrokerUri));

    } else{

      URI resourceUri = needUri;
      URI brokerUri = needBrokerUri;

      matcherProtocolQueueName = activeMQService.getProtocolQueueNameWithResource(resourceUri);
      camelConfiguration.setEndpoint(matcherProtocolCamelConfigurator.configureCamelEndpointForNeedUri(brokerUri,matcherProtocolQueueName));
      //matcherProtocolCamelConfigurator.addRouteForEndpoint(startingEndpoint,brokerUri);
      camelConfiguration.setBrokerComponentName(matcherProtocolCamelConfigurator.getBrokerComponentNameWithBrokerUri(brokerUri));
      ActiveMQComponent activeMQComponent = (ActiveMQComponent)matcherProtocolCamelConfigurator.getCamelContext().getComponent(matcherProtocolCamelConfigurator.getBrokerComponentNameWithBrokerUri(brokerUri));
      logger.info("ActiveMQ Service Status : {}",activeMQComponent.getStatus().toString());
      activeMQComponent.start();
    }
    return camelConfiguration;
  }

  @Override
  public synchronized Set<String> getMatcherProtocolOutTopics(URI wonNodeURI) {
    Set<String> matcherProtocolTopics = ((MatcherActiveMQService)activeMQService)
      .getMatcherProtocolTopicNamesWithResource(wonNodeURI);
    return matcherProtocolTopics;
  }

  @Override
  public synchronized void addRemoteTopicListeners(final Set<String> endpoints, final URI wonNodeUri)
    throws CamelConfigurationFailedException {
    URI remoteEndpoint = activeMQService.getBrokerEndpoint(wonNodeUri);
    String remoteComponentName = componentName+ remoteEndpoint.toString().replaceAll("[/:]","" );
    logger.debug("remoteComponentName: {}", remoteComponentName);
    matcherProtocolCamelConfigurator.addCamelComponentForWonNodeBrokerForTopics(remoteEndpoint,
                                                                       remoteComponentName
    );
    matcherProtocolCamelConfigurator.addRemoteTopicListeners(endpoints, remoteEndpoint);
  }

  @Override
  public URI getBrokerUri(URI resourceUri) throws NoSuchConnectionException {
    return activeMQService.getBrokerEndpoint(resourceUri);
  }

  @Override
  public ActiveMQService getActiveMQService() {
    return activeMQService;
  }

  @Override
  public void setActiveMQService(ActiveMQService activeMQService) {
    this.activeMQService = (MatcherActiveMQService) activeMQService;
  }

  @Override
  public CamelConfigurator getProtocolCamelConfigurator() {
    return this.matcherProtocolCamelConfigurator;

  }

  public void setComponentName(final String componentName) {
    this.componentName = componentName;
  }


  public void setMatcherProtocolCamelConfigurator(NeedProtocolCamelConfigurator matcherProtocolCamelConfigurator) {
    this.matcherProtocolCamelConfigurator = (MatcherProtocolCamelConfigurator) matcherProtocolCamelConfigurator;
  }



}

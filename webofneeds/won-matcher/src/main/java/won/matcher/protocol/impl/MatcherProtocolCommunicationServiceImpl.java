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
import org.springframework.beans.factory.annotation.Autowired;
import won.protocol.exception.NoSuchConnectionException;
import won.protocol.jms.*;
import won.protocol.model.Need;
import won.protocol.model.WonNode;
import won.protocol.repository.NeedRepository;
import won.protocol.repository.WonNodeRepository;

import java.net.URI;
import java.util.List;

/**
 * User: LEIH-NB
 * Date: 10.03.14
 */
public class MatcherProtocolCommunicationServiceImpl implements MatcherProtocolCommunicationService {

    @Autowired
    private NeedProtocolCamelConfigurator matcherProtocolCamelConfigurator;

    @Autowired
    private ActiveMQService matcherActiveMQService;

    @Autowired
    private NeedRepository needRepository;
    @Autowired
    private WonNodeRepository wonNodeRepository;

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public CamelConfiguration configureCamelEndpoint(URI needUri, String startingEndpoint) throws Exception {
        String matcherProtocolQueueName;
        CamelConfiguration camelConfiguration = new CamelConfiguration();

        URI needBrokerUri = matcherActiveMQService.getBrokerEndpoint(needUri);

        if (matcherProtocolCamelConfigurator.getBrokerComponentNameWithBrokerUri(needBrokerUri)!=null){
            camelConfiguration.setEndpoint(matcherProtocolCamelConfigurator.getEndpoint(needBrokerUri));
            camelConfiguration.setBrokerComponentName(matcherProtocolCamelConfigurator.getBrokerComponentNameWithBrokerUri(needBrokerUri));
           //matcherProtocolCamelConfigurator.addRouteForEndpoint(startingEndpoint,needBrokerUri);
        } else{

            URI resourceUri = needUri;
            URI brokerUri = needBrokerUri;

            matcherProtocolQueueName = matcherActiveMQService.getProtocolQueueNameWithResource(resourceUri);
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
    public URI getWonNodeUriWithNeedUri(URI needUri) throws NoSuchConnectionException {
        Need need = null;
        URI wonNodeUri = null;
        //need = needRepository.findByNeedURI(needUri).get(0);
        List<Need> needList = needRepository.findByNeedURI(needUri);
        if (needList.size()>0) {
            need = needList.get(0);
            wonNodeUri = need.getWonNodeURI();

        }


        return wonNodeUri;
    }

    @Override
    public URI getBrokerUri(URI resourceUri) throws NoSuchConnectionException {
        return matcherActiveMQService.getBrokerEndpoint(resourceUri);
    }

    @Override
    public ActiveMQService getActiveMQService() {
        return matcherActiveMQService;
    }

    @Override
    public void setActiveMQService(ActiveMQService activeMQService) {
       this.matcherActiveMQService = activeMQService;
    }

    @Override
    public CamelConfigurator getProtocolCamelConfigurator() {
        return this.matcherProtocolCamelConfigurator;

    }


}

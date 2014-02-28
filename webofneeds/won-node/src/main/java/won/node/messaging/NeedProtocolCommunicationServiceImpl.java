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

import org.springframework.beans.factory.annotation.Autowired;
import won.protocol.exception.NoSuchConnectionException;
import won.protocol.jms.*;

import java.net.URI;

/**
 * User: syim
 * Date: 27.01.14
 */

public class NeedProtocolCommunicationServiceImpl implements NeedProtocolCommunicationService {

    @Autowired
    private NeedProtocolCamelConfigurator needProtocolCamelConfigurator;

    @Autowired
    private ActiveMQService activeMQService;


    public CamelConfiguration configureCamelEndpoint(URI needUri,URI otherNeedUri,String startingEndpoint) throws Exception {
        String needProtocolQueueName;
        CamelConfiguration camelConfiguration = new CamelConfiguration();

        URI needBrokerUri = activeMQService.getBrokerEndpoint(needUri);
        URI otherNeedBrokerUri = activeMQService.getBrokerEndpoint(otherNeedUri);

        if (needProtocolCamelConfigurator.getBrokerComponentNameWithBrokerUri(otherNeedBrokerUri)!=null){
            camelConfiguration.setEndpoint(needProtocolCamelConfigurator.getEndpoint(otherNeedBrokerUri));
            camelConfiguration.setBrokerComponentName(needProtocolCamelConfigurator.getBrokerComponentNameWithBrokerUri(otherNeedBrokerUri));
            needProtocolCamelConfigurator.addRouteForEndpoint(startingEndpoint,otherNeedBrokerUri);
        } else{
            URI resourceUri;
            URI brokerUri;
            if (needUri!=otherNeedUri){
                resourceUri = otherNeedUri;
                brokerUri = otherNeedBrokerUri;
            }
            else{
                resourceUri = needUri;
                brokerUri = needBrokerUri;
            }
            needProtocolQueueName = activeMQService.getProtocolQueueNameWithResource(resourceUri);
            camelConfiguration.setEndpoint(needProtocolCamelConfigurator.configureCamelEndpointForNeedUri(brokerUri,needProtocolQueueName));
            needProtocolCamelConfigurator.addRouteForEndpoint(startingEndpoint,brokerUri);
            camelConfiguration.setBrokerComponentName(needProtocolCamelConfigurator.getBrokerComponentNameWithBrokerUri(brokerUri));
        }
        return camelConfiguration;
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
        this.activeMQService = activeMQService;
    }


    @Override
    public CamelConfigurator getProtocolCamelConfigurator() {
        return needProtocolCamelConfigurator;
    }
}

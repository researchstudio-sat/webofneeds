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

import org.apache.activemq.camel.component.ActiveMQComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    public synchronized CamelConfiguration configureCamelEndpoint(URI needUri,URI otherNeedUri,String startingEndpoint) throws Exception {
        String needProtocolQueueName;
        CamelConfiguration camelConfiguration = new CamelConfiguration();
        logger.debug("ensuring camel is configured for local need {}, remote need {} and starting endpoint {}",new Object[]{needUri, otherNeedUri, startingEndpoint});
        URI needBrokerUri = activeMQService.getBrokerEndpoint(needUri);
        URI otherNeedBrokerUri = activeMQService.getBrokerEndpoint(otherNeedUri);

        if (needProtocolCamelConfigurator.getBrokerComponentNameWithBrokerUri(otherNeedBrokerUri)!=null){
            logger.debug("broker component name is already known");
            camelConfiguration.setEndpoint(needProtocolCamelConfigurator.getEndpoint(otherNeedBrokerUri));
            camelConfiguration.setBrokerComponentName(needProtocolCamelConfigurator.getBrokerComponentNameWithBrokerUri(otherNeedBrokerUri));
            //HINT: we may have to handle routes that were shut down automatically after a timeout here...
            logger.debug("setting up the route if necessary");
            needProtocolCamelConfigurator.addRouteForEndpoint(startingEndpoint, otherNeedBrokerUri);
            logger.debug("done setting up the route");
        } else{
            logger.debug("broker component name unknown - setting up a new component for the remote broker");
            URI resourceUri;
            URI brokerUri;
            if (needBrokerUri.equals(otherNeedBrokerUri)){
                resourceUri = needUri;
                brokerUri = needBrokerUri;
            } else {
                resourceUri = otherNeedUri;
                brokerUri = otherNeedBrokerUri;
            }
            needProtocolQueueName = activeMQService.getProtocolQueueNameWithResource(resourceUri);
            camelConfiguration.setEndpoint(needProtocolCamelConfigurator.configureCamelEndpointForNeedUri(brokerUri,needProtocolQueueName));
            needProtocolCamelConfigurator.addRouteForEndpoint(startingEndpoint, otherNeedBrokerUri);
            camelConfiguration.setBrokerComponentName(needProtocolCamelConfigurator.getBrokerComponentNameWithBrokerUri(brokerUri));
            ActiveMQComponent activeMQComponent = (ActiveMQComponent)needProtocolCamelConfigurator.getCamelContext().getComponent(needProtocolCamelConfigurator.getBrokerComponentNameWithBrokerUri(brokerUri));
            logger.info("ActiveMQ Service Status : {}",activeMQComponent.getStatus().toString());
            activeMQComponent.start();
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

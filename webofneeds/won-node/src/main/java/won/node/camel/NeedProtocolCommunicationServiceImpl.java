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

package won.node.camel;

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


    public synchronized CamelConfiguration configureCamelEndpoint(URI wonNodeUri) throws Exception {
        String needProtocolQueueName;
        CamelConfiguration camelConfiguration = new CamelConfiguration();
        logger.debug("ensuring camel is configured for remote wonNodeUri",new Object[]{wonNodeUri});
        URI remoteNodeBrokerUri = activeMQService.getBrokerEndpoint(wonNodeUri);

        if (needProtocolCamelConfigurator.getBrokerComponentNameWithBrokerUri(remoteNodeBrokerUri)!=null){
            logger.debug("broker component name is already known");
            camelConfiguration.setEndpoint(needProtocolCamelConfigurator.getEndpoint(remoteNodeBrokerUri));
            camelConfiguration.setBrokerComponentName(needProtocolCamelConfigurator.getBrokerComponentNameWithBrokerUri(remoteNodeBrokerUri));
            //HINT: we may have to handle routes that were shut down automatically after a timeout here...
        } else{
            logger.debug("broker component name unknown - setting up a new component for the remote broker");
            URI resourceUri;
            URI brokerUri;

            resourceUri = wonNodeUri;
            brokerUri = remoteNodeBrokerUri;

            needProtocolQueueName = activeMQService.getProtocolQueueNameWithResource(resourceUri);
            camelConfiguration.setEndpoint(
              needProtocolCamelConfigurator.configureCamelEndpointForNeedUri(brokerUri, needProtocolQueueName));
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

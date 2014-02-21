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

import org.springframework.beans.factory.annotation.Autowired;
import won.protocol.exception.CamelConfigurationFailedException;
import won.protocol.exception.NoSuchConnectionException;
import won.protocol.jms.MessageBrokerService;
import won.protocol.jms.OwnerProtocolActiveMQService;
import won.protocol.model.Connection;
import won.protocol.model.Need;
import won.protocol.model.WonNode;
import won.protocol.repository.ConnectionRepository;
import won.protocol.repository.NeedRepository;
import won.protocol.util.DataAccessUtils;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * User: syim
 * Date: 27.01.14
 */
public class OwnerProtocolCommunicationService {
    public CamelConfigurator getCamelConfigurator() {
        return camelConfigurator;
    }

    @Autowired
    private CamelConfigurator camelConfigurator;
    @Autowired
    private ActiveMQServiceFactory activeMQServiceFactory;

    public final synchronized CamelConfiguration configureCamelEndpoint(String methodName, URI uri, List<WonNode> wonNodeList) throws Exception {
        CamelConfiguration camelConfiguration = new CamelConfiguration();
        URI brokerURI;
        String ownerProtocolQueueName;
        //CamelConfigurator camelConfigurator = camelConfiguratorFactory.createCamelConfigurator(methodName);
        OwnerProtocolActiveMQService activeMQService = activeMQServiceFactory.createActiveMQService(methodName,uri);

        if (wonNodeList.size()>0){
            WonNode wonNode = wonNodeList.get(0);
            brokerURI = wonNode.getBrokerURI();
            camelConfiguration.setEndpoint(wonNode.getOwnerProtocolEndpoint());
            if (camelConfigurator.getCamelContext().getComponent(wonNodeList.get(0).getBrokerComponent())==null){
                camelConfiguration.setBrokerComponentName(camelConfigurator.addCamelComponentForWonNodeBroker(wonNode.getWonNodeURI(),brokerURI,wonNode.getOwnerApplicationID()));
                camelConfigurator.getCamelContext().getComponent(camelConfiguration.getBrokerComponentName()).createEndpoint(camelConfiguration.getEndpoint());
                camelConfigurator.addRouteForEndpoint(wonNode.getWonNodeURI());
            }
        } else{
            URI wonNodeURI = activeMQService.getWonNodeURI();
            brokerURI = activeMQService.getBrokerURI();
            camelConfiguration.setBrokerComponentName(camelConfigurator.addCamelComponentForWonNodeBroker(wonNodeURI,brokerURI,null));
            ownerProtocolQueueName = activeMQService.getOwnerProtocolQueueNameWithResource();
            camelConfiguration.setEndpoint(camelConfigurator.configureCamelEndpointForNodeURI(wonNodeURI, brokerURI, ownerProtocolQueueName));
        }
        return camelConfiguration;
    }

    public String replaceComponentNameWithOwnerApplicationId(CamelConfiguration camelConfiguration,String ownerApplicationId){
        return camelConfigurator.replaceComponentNameWithOwnerApplicationId(camelConfiguration.getBrokerComponentName(),ownerApplicationId);
    }

    public String replaceEndpointNameWithOwnerApplicationId(CamelConfiguration camelConfiguration,String ownerApplicationId) throws Exception {
        return camelConfigurator.replaceEndpointNameWithOwnerApplicationId(camelConfiguration.getEndpoint(),ownerApplicationId);
    }
    public URI getBrokerUri(URI wonNodeUri) throws NoSuchConnectionException {
        OwnerProtocolActiveMQService activeMQService = activeMQServiceFactory.createActiveMQService("register",wonNodeUri);
        return activeMQService.getBrokerURI();
    };
    public void setActiveMQServiceFactory(ActiveMQServiceFactory activeMQServiceFactory) {
        this.activeMQServiceFactory = activeMQServiceFactory;
    }


}

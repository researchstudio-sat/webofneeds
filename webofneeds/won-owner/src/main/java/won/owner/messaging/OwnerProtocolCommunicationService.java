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
import won.protocol.model.Connection;
import won.protocol.model.Need;
import won.protocol.model.WonNode;
import won.protocol.repository.ConnectionRepository;
import won.protocol.repository.NeedRepository;
import won.protocol.util.DataAccessUtils;

import java.net.URI;
import java.util.ArrayList;

/**
 * User: syim
 * Date: 27.01.14
 */
public class OwnerProtocolCommunicationService {
    @Autowired
    CamelConfiguratorFactory camelConfiguratorFactory;
    @Autowired
    ActiveMQServiceFactory activeMQServiceFactory;
    @Autowired
    ConnectionRepository connectionRepository;

    final String configureCamelEndpoint(String methodName, URI uri,ArrayList<WonNode> wonNodeList) throws NoSuchConnectionException, CamelConfigurationFailedException {
        String endpoint;
        URI brokerURI;
        String ownerProtocolQueueName;
        CamelConfigurator camelConfigurator = camelConfiguratorFactory.createCamelConfigurator(methodName);
        if (wonNodeList.size()>0){
            WonNode wonNode = wonNodeList.get(0);
            String startingComponent = wonNode.getStartingComponent();
            brokerURI = wonNode.getBrokerURI();
            if (camelConfigurator.getCamelContext().getComponent(wonNodeList.get(0).getBrokerComponent())==null){
                String brokerComponentName = camelConfigurator.addCamelComponentForWonNodeBroker(wonNode.getWonNodeURI(),brokerURI,wonNode.getOwnerApplicationID());
            }

            endpoint = wonNode.getOwnerProtocolEndpoint();
        } else{
            URI wonNodeURI;
            OwnerProtocolActiveMQServiceImplRefactoring ownerProtocolActiveMQServiceImplRefactoring = activeMQServiceFactory.createActiveMQService(methodName,uri);
            wonNodeURI=ownerProtocolActiveMQServiceImplRefactoring.getWonNodeURI();
            brokerURI = ownerProtocolActiveMQServiceImplRefactoring.getBrokerURIForNode();
            ownerProtocolQueueName = ownerProtocolActiveMQServiceImplRefactoring.getOwnerProtocolQueueNameWithResource();

            endpoint = camelConfigurator.configureCamelEndpointForNodeURI(wonNodeURI,brokerURI,ownerProtocolQueueName);

        }
        return endpoint;
    }

    public void setActiveMQServiceFactory(ActiveMQServiceFactory activeMQServiceFactory) {
        this.activeMQServiceFactory = activeMQServiceFactory;
    }


}

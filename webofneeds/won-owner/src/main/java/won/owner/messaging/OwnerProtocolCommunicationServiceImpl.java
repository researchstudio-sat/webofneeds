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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import won.protocol.exception.NoSuchConnectionException;
import won.protocol.jms.ActiveMQService;
import won.protocol.jms.CamelConfiguration;
import won.protocol.jms.OwnerProtocolCamelConfigurator;
import won.protocol.jms.OwnerProtocolCommunicationService;
import won.protocol.model.Connection;
import won.protocol.model.Need;
import won.protocol.model.WonNode;
import won.protocol.repository.ConnectionRepository;
import won.protocol.repository.NeedRepository;
import won.protocol.repository.WonNodeRepository;
import won.protocol.util.DataAccessUtils;

import java.net.URI;
import java.util.List;

/**
 * User: syim
 * Date: 27.01.14
 */
public class OwnerProtocolCommunicationServiceImpl implements OwnerProtocolCommunicationService {

    @Autowired
    private OwnerProtocolCamelConfigurator ownerProtocolCamelConfigurator;
    @Autowired
    private ActiveMQService activeMQService;
    @Autowired
    private NeedRepository needRepository;
    @Autowired
    private ConnectionRepository connectionRepository;
    @Autowired
    private WonNodeRepository wonNodeRepository;

    Logger logger = LoggerFactory.getLogger(this.getClass());
    @Override
    public final synchronized CamelConfiguration configureCamelEndpoint(URI wonNodeUri) throws Exception {
        CamelConfiguration camelConfiguration = new CamelConfiguration();
        URI brokerURI;
        String ownerProtocolQueueName;
        List<WonNode> wonNodeList = wonNodeRepository.findByWonNodeURI(wonNodeUri);
        //OwnerProtocolCamelConfigurator ownerProtocolCamelConfigurator = camelConfiguratorFactory.createCamelConfigurator(methodName);
        logger.debug("configuring camel endpoint");
        if (wonNodeList.size()>0){
            WonNode wonNode = wonNodeList.get(0);
            brokerURI = wonNode.getBrokerURI();
            camelConfiguration.setEndpoint(wonNode.getOwnerProtocolEndpoint());
            if (ownerProtocolCamelConfigurator.getCamelContext().getComponent(wonNodeList.get(0).getBrokerComponent())==null){
                camelConfiguration.setBrokerComponentName(ownerProtocolCamelConfigurator.addCamelComponentForWonNodeBroker(wonNode.getWonNodeURI(),brokerURI,wonNode.getOwnerApplicationID()));
                ownerProtocolCamelConfigurator.getCamelContext().getComponent(camelConfiguration.getBrokerComponentName()).createEndpoint(camelConfiguration.getEndpoint());
                if(ownerProtocolCamelConfigurator.getCamelContext().getRoute(wonNode.getStartingComponent())==null)
                    ownerProtocolCamelConfigurator.addRouteForEndpoint(null,wonNode.getWonNodeURI());
            }
        } else{  //if unknown wonNode
            brokerURI = activeMQService.getBrokerEndpoint(wonNodeUri);
            camelConfiguration.setBrokerComponentName(ownerProtocolCamelConfigurator.addCamelComponentForWonNodeBroker(wonNodeUri,brokerURI,null));

            //TODO: brokerURI gets the node information already. so requesting node information again for queuename would be duplicate
            ownerProtocolQueueName = activeMQService.getProtocolQueueNameWithResource(wonNodeUri);
            camelConfiguration.setEndpoint(ownerProtocolCamelConfigurator.configureCamelEndpointForNodeURI(wonNodeUri, brokerURI, ownerProtocolQueueName));
            ownerProtocolCamelConfigurator.addRouteForEndpoint(null, wonNodeUri);
        }

        return camelConfiguration;
    }
    @Override
    public synchronized URI  getWonNodeUriWithConnectionUri(URI connectionUri) throws NoSuchConnectionException {
        Connection con = DataAccessUtils.loadConnection(connectionRepository, connectionUri);
        URI needURI = con.getNeedURI();
        Need need = needRepository.findByNeedURI(needURI).get(0);
        return need.getWonNodeURI();
    }
    @Override
    public synchronized URI  getWonNodeUriWithNeedUri(URI needUri) throws NoSuchConnectionException {
            Need need = needRepository.findByNeedURI(needUri).get(0);
            return need.getWonNodeURI();
    }

    public String replaceComponentNameWithOwnerApplicationId(CamelConfiguration camelConfiguration,String ownerApplicationId){
        return ownerProtocolCamelConfigurator.replaceComponentNameWithOwnerApplicationId(camelConfiguration.getBrokerComponentName(),ownerApplicationId);
    }

    public String replaceEndpointNameWithOwnerApplicationId(CamelConfiguration camelConfiguration,String ownerApplicationId) throws Exception {
        return ownerProtocolCamelConfigurator.replaceEndpointNameWithOwnerApplicationId(camelConfiguration.getEndpoint(),ownerApplicationId);
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
    public OwnerProtocolCamelConfigurator getProtocolCamelConfigurator() {
        return ownerProtocolCamelConfigurator;
    }
}

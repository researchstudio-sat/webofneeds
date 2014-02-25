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
import won.protocol.exception.NoSuchConnectionException;
import won.protocol.jms.ActiveMQService;
import won.protocol.model.Connection;
import won.protocol.model.Need;
import won.protocol.model.WonNode;
import won.protocol.repository.ConnectionRepository;
import won.protocol.repository.NeedRepository;
import won.protocol.util.DataAccessUtils;

import java.net.URI;
import java.util.List;

/**
 * User: syim
 * Date: 27.01.14
 */
public class OwnerProtocolCommunicationService {

    @Autowired
    private OwnerProtocolCamelConfigurator ownerProtocolCamelConfigurator;
    @Autowired
    private ActiveMQService activeMQService;
    @Autowired
    private NeedRepository needRepository;
    @Autowired
    private ConnectionRepository connectionRepository;

    public final synchronized CamelConfiguration configureCamelEndpoint(URI wonNodeUri, List<WonNode> wonNodeList) throws Exception {
        CamelConfiguration camelConfiguration = new CamelConfiguration();
        URI brokerURI;
        String ownerProtocolQueueName;
        //OwnerProtocolCamelConfigurator ownerProtocolCamelConfigurator = camelConfiguratorFactory.createCamelConfigurator(methodName);

        if (wonNodeList.size()>0){
            WonNode wonNode = wonNodeList.get(0);
            brokerURI = wonNode.getBrokerURI();
            camelConfiguration.setEndpoint(wonNode.getOwnerProtocolEndpoint());
            if (ownerProtocolCamelConfigurator.getCamelContext().getComponent(wonNodeList.get(0).getBrokerComponent())==null){
                camelConfiguration.setBrokerComponentName(ownerProtocolCamelConfigurator.addCamelComponentForWonNodeBroker(wonNode.getWonNodeURI(),brokerURI,wonNode.getOwnerApplicationID()));
                ownerProtocolCamelConfigurator.getCamelContext().getComponent(camelConfiguration.getBrokerComponentName()).createEndpoint(camelConfiguration.getEndpoint());
                if(ownerProtocolCamelConfigurator.getCamelContext().getRoute(wonNode.getStartingComponent())==null)
                    ownerProtocolCamelConfigurator.addRouteForEndpoint(wonNode.getWonNodeURI());
            }
        } else{

            brokerURI = activeMQService.getBrokerURI(wonNodeUri);
            camelConfiguration.setBrokerComponentName(ownerProtocolCamelConfigurator.addCamelComponentForWonNodeBroker(wonNodeUri,brokerURI,null));

            //TODO: brokerURI gets the node information already. so requesting node information again for queuename would be duplicate
            ownerProtocolQueueName = activeMQService.getProtocolQueueNameWithResource(wonNodeUri);
            camelConfiguration.setEndpoint(ownerProtocolCamelConfigurator.configureCamelEndpointForNodeURI(wonNodeUri, brokerURI, ownerProtocolQueueName));
            ownerProtocolCamelConfigurator.addRouteForEndpoint(wonNodeUri);
        }
        return camelConfiguration;
    }
    public synchronized URI  getWonNodeUri(String methodName, URI uri) throws NoSuchConnectionException {

        if (methodName.equals("connect")||methodName.equals("deactivate")||methodName.equals("activate")){
            Need need = needRepository.findByNeedURI(uri).get(0);
            return need.getWonNodeURI();
        } else if (methodName.equals("textMessage")||methodName.equals("close")||methodName.equals("open")){
            Connection con = DataAccessUtils.loadConnection(connectionRepository, uri);
            URI needURI = con.getNeedURI();
            Need need = needRepository.findByNeedURI(needURI).get(0);
            return need.getWonNodeURI();
        } else
            return null;


    }
    public String replaceComponentNameWithOwnerApplicationId(CamelConfiguration camelConfiguration,String ownerApplicationId){
        return ownerProtocolCamelConfigurator.replaceComponentNameWithOwnerApplicationId(camelConfiguration.getBrokerComponentName(),ownerApplicationId);
    }

    public String replaceEndpointNameWithOwnerApplicationId(CamelConfiguration camelConfiguration,String ownerApplicationId) throws Exception {
        return ownerProtocolCamelConfigurator.replaceEndpointNameWithOwnerApplicationId(camelConfiguration.getEndpoint(),ownerApplicationId);
    }
    public URI getBrokerUri(URI wonNodeUri) throws NoSuchConnectionException {
        return activeMQService.getBrokerURI(wonNodeUri);
    }
    public ActiveMQService getActiveMQService() {
        return activeMQService;
    }

    public void setActiveMQService(ActiveMQService activeMQService) {
        this.activeMQService = activeMQService;
    }
    public OwnerProtocolCamelConfigurator getOwnerProtocolCamelConfigurator() {
        return ownerProtocolCamelConfigurator;
    }
}

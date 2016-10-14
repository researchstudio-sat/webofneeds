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
import won.cryptography.service.RegistrationClient;
import won.cryptography.service.RegistrationRestClientHttps;
import won.protocol.exception.CamelConfigurationFailedException;
import won.protocol.exception.NoSuchConnectionException;
import won.protocol.jms.*;
import won.protocol.model.Connection;
import won.protocol.model.Need;
import won.protocol.model.WonNode;
import won.protocol.repository.ConnectionRepository;
import won.protocol.repository.NeedRepository;
import won.protocol.repository.WonNodeRepository;
import won.protocol.util.DataAccessUtils;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * User: syim
 * Date: 27.01.14
 */
public class OwnerProtocolCommunicationServiceImpl implements OwnerProtocolCommunicationService
{

  @Autowired
  private OwnerProtocolCamelConfiguratorImpl ownerProtocolCamelConfigurator;

  private ActiveMQService activeMQService;
  @Autowired
  private NeedRepository needRepository;
  @Autowired
  private ConnectionRepository connectionRepository;
  @Autowired
  private WonNodeRepository wonNodeRepository;

  //can also be autowired
  private RegistrationClient registrationClient;



  Logger logger = LoggerFactory.getLogger(this.getClass());



  public void setRegistrationClient(final RegistrationRestClientHttps registrationClient) {
    this.registrationClient = registrationClient;
  }

  /**
   * Registers the owner application at a won node. Owner Id is typically his Key ID (lower 64 bits of the owner public
   * key fingerprint). Unless there is a collision of owner ids on the node - then the owner can assign another id...
   *
   * @return ownerApplicationId
   * @throws Exception
   */
  public synchronized void register(URI wonNodeURI, MessagingService messagingService) throws Exception {
    CamelConfiguration camelConfiguration = null;

    logger.debug("register at won node: " + wonNodeURI);
    if (isRegistered(wonNodeURI)) {
      //TODO: if the WoN node does not remember the owner (eg because it decided to erase the owner application from
      // its db), this will fail, and we currently have no way of restoring the registration, except for manually
      // removing the registration data from the owner application's db, too.
      WonNode wonNode = DataAccessUtils.loadWonNode(wonNodeRepository, wonNodeURI);
      String ownerApplicationId = wonNode.getOwnerApplicationID();
      configureCamelEndpoint(wonNodeURI, ownerApplicationId);
      configureRemoteEndpointsForOwnerApplication(ownerApplicationId, getProtocolCamelConfigurator().getEndpoint
        (wonNodeURI), messagingService);

    } else {

      String ownerApplicationId = registrationClient.register(wonNodeURI.toString());
      logger.debug("registered ownerappID: " + ownerApplicationId);
      camelConfiguration = configureCamelEndpoint(wonNodeURI, ownerApplicationId);
      storeWonNode(ownerApplicationId, camelConfiguration, wonNodeURI);
      configureRemoteEndpointsForOwnerApplication(ownerApplicationId, getProtocolCamelConfigurator().getEndpoint
        (wonNodeURI), messagingService);
    }

  }

  // TODO this is messy, has to be improved, maybe endpoints should be obtained in the same step as registration,
  // e.g. the register call returns not only application id, but also the endpoints...
  private void configureRemoteEndpointsForOwnerApplication(String ownerApplicationID, String remoteEndpoint, MessagingService messagingService)
    throws CamelConfigurationFailedException, ExecutionException, InterruptedException {
    Map<String, Object> headerMap = new HashMap<>();
    headerMap.put("ownerApplicationID", ownerApplicationID);
    headerMap.put("methodName", "getEndpoints");
    headerMap.put("remoteBrokerEndpoint", remoteEndpoint);

    Future<List<String>> futureResults = messagingService
      .sendInOutMessageGeneric(headerMap, headerMap, null, "seda:outgoingMessages");
    List<String> endpoints = futureResults.get();

    getProtocolCamelConfigurator().addRemoteQueueListeners(endpoints, URI.create(remoteEndpoint));
    //TODO: some checks needed to assure that the application is configured correctly.
    //todo this method should return routes
  }

  /**
   * Stores the won node information, possibly overwriting existing data.
   *
   * @param ownerApplicationId
   * @param camelConfiguration
   * @param wonNodeURI
   * @return
   * @throws NoSuchConnectionException
   */
  public WonNode storeWonNode(String ownerApplicationId, CamelConfiguration camelConfiguration, URI wonNodeURI)
    throws NoSuchConnectionException {
    WonNode wonNode = DataAccessUtils.loadWonNode(wonNodeRepository, wonNodeURI);
    if (wonNode == null) {
      wonNode = new WonNode();
    }
    wonNode.setOwnerApplicationID(ownerApplicationId);
    wonNode.setOwnerProtocolEndpoint(camelConfiguration.getEndpoint());
    wonNode.setWonNodeURI(wonNodeURI);
    wonNode.setBrokerURI(getBrokerUri(wonNodeURI));
    wonNode.setBrokerComponent(camelConfiguration.getBrokerComponentName());
    wonNode.setStartingComponent(getProtocolCamelConfigurator().getStartingEndpoint(wonNodeURI));
    wonNodeRepository.save(wonNode);
    logger.debug("setting starting component {}", wonNode.getStartingComponent());
    return wonNode;
  }

  public final synchronized CamelConfiguration configureCamelEndpoint(URI wonNodeUri, String ownerId) throws
    Exception {

    CamelConfiguration camelConfiguration = new CamelConfiguration();
    URI brokerURI = activeMQService.getBrokerEndpoint(wonNodeUri);
    String ownerProtocolQueueName;
    List<WonNode> wonNodeList = wonNodeRepository.findByWonNodeURI(wonNodeUri);
    //OwnerProtocolCamelConfigurator ownerProtocolCamelConfigurator = camelConfiguratorFactory.createCamelConfigurator(methodName);
    logger.debug("configuring camel endpoint");
    if (ownerProtocolCamelConfigurator.getBrokerComponentName(brokerURI)!=null &&
      ownerProtocolCamelConfigurator
        .getEndpoint(wonNodeUri)!=null){
      logger.debug("wonNode known");
      WonNode wonNode = wonNodeList.get(0);
      //brokerURI = wonNode.getBrokerURI();
      camelConfiguration.setEndpoint(wonNode.getOwnerProtocolEndpoint());
      if (ownerProtocolCamelConfigurator.getCamelContext().getComponent(wonNodeList.get(0).getBrokerComponent())==null){
        //camelConfiguration.setBrokerComponentName(ownerProtocolCamelConfigurator.addCamelComponentForWonNodeBroker
        //  (wonNode.getWonNodeURI(),brokerURI,wonNode.getOwnerApplicationID()));
        ownerProtocolQueueName = activeMQService.getProtocolQueueNameWithResource(wonNodeUri);
        String endpoint = ownerProtocolCamelConfigurator.configureCamelEndpointForNodeURI(wonNodeUri, brokerURI,
                                                                                          ownerProtocolQueueName);
        camelConfiguration.setBrokerComponentName(ownerProtocolCamelConfigurator.getBrokerComponentName(brokerURI));

        ownerProtocolCamelConfigurator.getCamelContext().getComponent(camelConfiguration.getBrokerComponentName()).createEndpoint(camelConfiguration.getEndpoint());
        if(ownerProtocolCamelConfigurator.getCamelContext().getRoute(wonNode.getStartingComponent())==null)
          ownerProtocolCamelConfigurator.addRouteForEndpoint(null, wonNode.getWonNodeURI());
      }
    } else {  //if unknown wonNode
      logger.debug("wonNode unknown");
      //TODO: brokerURI gets the node information already. so requesting node information again for queuename would be duplicate
      ownerProtocolQueueName = activeMQService.getProtocolQueueNameWithResource(wonNodeUri);
      String endpoint = ownerProtocolCamelConfigurator.configureCamelEndpointForNodeURI(wonNodeUri, brokerURI,
                                                                                        ownerProtocolQueueName);
      camelConfiguration.setEndpoint(endpoint);
      camelConfiguration.setBrokerComponentName(ownerProtocolCamelConfigurator.getBrokerComponentName(brokerURI));
      ownerProtocolCamelConfigurator.addRouteForEndpoint(null, wonNodeUri);
    }

    return camelConfiguration;
  }

  private boolean isRegistered(URI wonNodeURI) {
    List<WonNode> wonNodes = wonNodeRepository.findByWonNodeURI(wonNodeURI);
    if (!wonNodes.isEmpty()) {
      return true;
    }
    return false;
  }


  @Override
  public synchronized URI  getWonNodeUriWithConnectionUri(URI connectionUri) throws NoSuchConnectionException {
    //TODO: make this more efficient
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

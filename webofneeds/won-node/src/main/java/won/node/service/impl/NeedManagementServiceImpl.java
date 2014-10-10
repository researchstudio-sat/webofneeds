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

package won.node.service.impl;

/**
 * User: LEIH-NB
 * Date: 28.10.13
 */

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.vocabulary.RDF;
import org.javasimon.SimonManager;
import org.javasimon.Split;
import org.javasimon.Stopwatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import won.node.protocol.MatcherProtocolMatcherServiceClientSide;
import won.protocol.exception.*;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.message.WonMessageEncoder;
import won.protocol.message.WonMessageType;
import won.protocol.model.*;
import won.protocol.owner.OwnerProtocolOwnerServiceClientSide;
import won.protocol.repository.*;
import won.protocol.repository.rdfstorage.RDFStorageService;
import won.protocol.service.LinkedDataService;
import won.protocol.service.NeedInformationService;
import won.protocol.service.NeedManagementService;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.DataAccessUtils;
import won.protocol.util.RdfUtils;
import won.protocol.util.WonRdfUtils;
import won.protocol.vocabulary.WON;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * User: fkleedorfer
 * Date: 02.11.12
 */
/* TODO: The logic of the methods of this class has nothing to do with JMS. should be merged with NeedManagementServiceImpl class. The only change was made in createNeed method, where the concept of authorizedApplications for each need was introduced.
 */
@Component
public class NeedManagementServiceImpl implements NeedManagementService
{
  final Logger logger = LoggerFactory.getLogger(getClass());
  private OwnerProtocolOwnerServiceClientSide ownerProtocolOwnerService;

  private MatcherProtocolMatcherServiceClientSide matcherProtocolMatcherClient;
  //used to close connections when a need is deactivated
  private OwnerFacingConnectionCommunicationServiceImpl ownerFacingConnectionCommunicationService;
  private NeedInformationService needInformationService;
  private URIService uriService;
  private RDFStorageService rdfStorage;

  @Autowired
  private NeedRepository needRepository;
  @Autowired
  private ConnectionRepository connectionRepository;
  @Autowired
  FacetRepository facetRepository;
  @Autowired
  private OwnerApplicationRepository ownerApplicationRepository;
  @Autowired
  private MessageEventRepository messageEventRepository;
  @Autowired
  private LinkedDataService linkedDataService;
  @Autowired
  private WonNodeInformationService wonNodeInformationService;


  //TODO: remove 'active' parameter, make need active by default, and look into RDF for an optional 'isInState' triple.
  @Override
  public URI createNeed(
    final Model content,
    final boolean activate,
    String ownerApplicationID,
    WonMessage wonMessage) throws IllegalNeedContentException {


    // distinguish between the new message format (WonMessage) and the old parameters
    // ToDo (FS): remove this distinction if the old parameters not used anymore
    if (wonMessage != null) {
      WonMessage newWonMessage = new WonMessageBuilder()
        .wrap(wonMessage)
        .setTimestamp(System.currentTimeMillis())
        .build();
      // store the newWonMessage as it is
      logger.debug("STORING message with id {}", newWonMessage.getMessageURI());
      rdfStorage.storeDataset(newWonMessage.getMessageURI(),
                              WonMessageEncoder.encodeAsDataset(newWonMessage));


      // the model where all the information created by the WON node is stored
      // Update: the meta data model is not needed at the moment since the meta data
      // are generated on each request from the LinkedDataService
      // Model needMeta = ModelFactory.createDefaultModel();

      // the dataset which contains the need model graphs from the owner application
      Dataset needContent = newWonMessage.getMessageContent();

      URI needURI = getNeedURIFromWonMessage(needContent);

      if (!needURI.equals(newWonMessage.getSenderNeedURI()))
        throw new IllegalArgumentException("receiverNeedURI and NeedURI of the content are not equal");

      Need need = new Need();

      need.setState(NeedState.ACTIVE);
      need.setNeedURI(needURI);

      // ToDo (FS) check if the WON node URI corresponds with the WON node (maybe earlier in the message layer)
      need.setWonNodeURI(newWonMessage.getReceiverNodeURI());

      need = needRepository.save(need);

      // store the message event placeholder to keep the connection between need and message event
      messageEventRepository.save(new MessageEventPlaceholder(needURI, newWonMessage));

      List<Facet> facets = WonRdfUtils.NeedUtils.getFacets(needURI, needContent);
      if (facets.size() == 0)
        throw new IllegalArgumentException("at least one RDF node must be of type won:HAS_FACET");
      for (Facet f : facets) {
        // TODO: check if there is a implementation for the facet on the node
        facetRepository.save(f);
      }

      // remove connection container if the create message contains already one (or some)
      WonRdfUtils.NeedUtils.removeConnectionContainer(needContent, needURI);

      rdfStorage.storeDataset(needURI, needContent);
      authorizeOwnerApplicationForNeed(ownerApplicationID, need);

      // ToDo (FS): send the same newWonMessage or create a new one (with new type)?


      try {
        Dataset needDataset = linkedDataService.getNeedDataset(need.getNeedURI());
        WonMessage newNeedNotificationMessage =
          new WonMessageBuilder()
            .setWonMessageType(WonMessageType.NEED_CREATED_NOTIFICATION)
            .setMessageURI(wonNodeInformationService.generateMessageEventURI())
            .setSenderNeedURI(need.getNeedURI())
            .setSenderNodeURI(need.getWonNodeURI())
            .build(needDataset);
        matcherProtocolMatcherClient.needCreated(needURI, ModelFactory.createDefaultModel(), newNeedNotificationMessage);
      } catch (Exception e) {
        logger.warn("could not create NeedCreatedNotification", e);
      }
      return needURI;

    } else {

      // do it the traditional way

      String stopwatchName = getClass().getName() + ".createNeed";

      Stopwatch stopwatch = SimonManager.getStopwatch(stopwatchName + "_phase1");
      Split split = stopwatch.start();
      logger.debug("CREATING need. OwnerApplicationId:{}", ownerApplicationID);
      Need need = new Need();
      need.setState(activate ? NeedState.ACTIVE : NeedState.INACTIVE);
      need = needRepository.save(need);
      split.stop();

      stopwatch = SimonManager.getStopwatch(stopwatchName + "_phase2");
      split = stopwatch.start();
      //now, create the need URI and save again
      need.setNeedURI(uriService.createNeedURI(need));
      need.setWonNodeURI(URI.create(uriService.getGeneralURIPrefix()));
      need = needRepository.save(need);
      split.stop();

      stopwatch = SimonManager.getStopwatch(stopwatchName + "_phase3");
      split = stopwatch.start();
      String baseURI = need.getNeedURI().toString();
      RdfUtils.replaceBaseURI(content, baseURI);
      rdfStorage.storeModel(need.getNeedURI(), content);
      split.stop();

      stopwatch = SimonManager.getStopwatch(stopwatchName + "_phase4");
      split = stopwatch.start();
      ResIterator needIt = content.listSubjectsWithProperty(RDF.type, WON.NEED);
      if (!needIt.hasNext()) throw new IllegalArgumentException("at least one RDF node must be of type won:Need");

      Resource needRes = needIt.next();
      logger.debug("processing need resource {}", needRes.getURI());

      StmtIterator stmtIterator = content.listStatements(needRes, WON.HAS_FACET, (RDFNode) null);
      if (!stmtIterator.hasNext())
        throw new IllegalArgumentException("at least one RDF node must be of type won:HAS_FACET");
      else
        //TODO: check if there is a implementation for the facet on the node
        do {
          Facet facet = new Facet();
          facet.setNeedURI(need.getNeedURI());
          facet.setTypeURI(URI.create(stmtIterator.next().getObject().asResource().getURI()));
          facetRepository.save(facet);
        } while (stmtIterator.hasNext());
      split.stop();

      stopwatch = SimonManager.getStopwatch(stopwatchName + "_phase5");
      split = stopwatch.start();
      authorizeOwnerApplicationForNeed(ownerApplicationID, need);
      split.stop();
      matcherProtocolMatcherClient.needCreated(need.getNeedURI(), content, wonMessage);
      return need.getNeedURI();

    }

  }

  @Override
  public void authorizeOwnerApplicationForNeedURI(final String ownerApplicationID, URI needURI) {
    logger.debug("AUTHORIZING owner application. needURI:{}, OwnerApplicationId:{}", needURI, ownerApplicationID);
    Need need = needRepository.findByNeedURI(needURI).get(0);

    authorizeOwnerApplicationForNeed(ownerApplicationID, need);
  }

  @Override
  public void authorizeOwnerApplicationForNeed(final String ownerApplicationID, Need need) {
    String stopwatchName = getClass().getName() + ".authorizeOwnerApplicationForNeed";
    Stopwatch stopwatch = SimonManager.getStopwatch(stopwatchName + "_phase1");
    Split split = stopwatch.start();
    List<OwnerApplication> ownerApplications = ownerApplicationRepository.findByOwnerApplicationId(ownerApplicationID);
    split.stop();
    stopwatch = SimonManager.getStopwatch(stopwatchName + "_phase2");
    split = stopwatch.start();
    if (ownerApplications.size() > 0) {
      logger.debug("owner application is already known");
      OwnerApplication ownerApplication = ownerApplications.get(0);
      List<OwnerApplication> authorizedApplications = need.getAuthorizedApplications();
      if (authorizedApplications == null) {
        authorizedApplications = new ArrayList<OwnerApplication>(1);
      }
      authorizedApplications.add(ownerApplication);
      need.setAuthorizedApplications(authorizedApplications);
    } else {
      logger.debug("owner application is new - creating");
      List<OwnerApplication> ownerApplicationList = new ArrayList<>(1);
      OwnerApplication ownerApplication = new OwnerApplication();
      ownerApplication.setOwnerApplicationId(ownerApplicationID);
      ownerApplicationList.add(ownerApplication);
      need.setAuthorizedApplications(ownerApplicationList);
      logger.debug("setting OwnerApp ID: " + ownerApplicationList.get(0));
    }
    split.stop();
    stopwatch = SimonManager.getStopwatch(stopwatchName + "_phase3");
    split = stopwatch.start();
    need = needRepository.save(need);
    split.stop();
  }

  @Override
  public void activate(final URI needURI, WonMessage wonMessage) throws NoSuchNeedException {

    // distinguish between the new message format (WonMessage) and the old parameters
    // ToDo (FS): remove this distinction if the old parameters not used anymore
    if (wonMessage != null) {
      WonMessage newWonMessage = new WonMessageBuilder()
        .wrap(wonMessage)
        .setTimestamp(System.currentTimeMillis())
        .build();
      logger.debug("STORING message with id {}", newWonMessage.getMessageURI());
      rdfStorage.storeDataset(newWonMessage.getMessageURI(),
                              WonMessageEncoder.encodeAsDataset(newWonMessage));

      URI receiverNeedURI = newWonMessage.getReceiverNeedURI();
      logger.debug("ACTIVATING need. needURI:{}", receiverNeedURI);
      if (receiverNeedURI == null) throw new IllegalArgumentException("receiverNeedURI is not set");
      Need need = DataAccessUtils.loadNeed(needRepository, receiverNeedURI);
      need.setState(NeedState.ACTIVE);
      logger.debug("Setting Need State: " + need.getState());
      needRepository.save(need);
      messageEventRepository.save(new MessageEventPlaceholder(need.getNeedURI(), newWonMessage));

      matcherProtocolMatcherClient.needActivated(need.getNeedURI(), newWonMessage);

    } else {

      logger.debug("ACTIVATING need. needURI:{}", needURI);
      if (needURI == null) throw new IllegalArgumentException("needURI is not set");
      Need need = DataAccessUtils.loadNeed(needRepository, needURI);
      need.setState(NeedState.ACTIVE);
      logger.debug("Setting Need State: " + need.getState());
      needRepository.save(need);

      matcherProtocolMatcherClient.needActivated(need.getNeedURI(), wonMessage);
    }

  }

  @Override
  public void deactivate(final URI needURI, WonMessage wonMessage)
    throws NoSuchNeedException, NoSuchConnectionException {

    // distinguish between the new message format (WonMessage) and the old parameters
    // ToDo (FS): remove this distinction if the old parameters not used anymore
    if (wonMessage != null) {
      WonMessage newWonMessage = new WonMessageBuilder()
        .wrap(wonMessage)
        .setTimestamp(System.currentTimeMillis())
        .build();
      logger.debug("STORING message with id {}", newWonMessage.getMessageURI());
      rdfStorage.storeDataset(newWonMessage.getMessageURI(),
                              WonMessageEncoder.encodeAsDataset(newWonMessage));

      URI receiverNeedURI = newWonMessage.getReceiverNeedURI();
      logger.debug("DEACTIVATING need. needURI:{}", receiverNeedURI);
      if (receiverNeedURI == null) throw new IllegalArgumentException("receiverNeedURI is not set");
      Need need = DataAccessUtils.loadNeed(needRepository, receiverNeedURI);
      need.setState(NeedState.INACTIVE);
      need = needRepository.save(need);
      messageEventRepository.save(new MessageEventPlaceholder(need.getNeedURI(), newWonMessage));

      //close all connections
      Collection<URI> connectionURIs = connectionRepository.getConnectionURIsByNeedURIAndNotInState(need.getNeedURI
        (), ConnectionState.CLOSED);
      for (URI connURI : connectionURIs) {
        try {
          WonMessage closeWonMessage = createCloseWonMessage(connURI);
          ownerFacingConnectionCommunicationService.close(connURI, null, closeWonMessage);
        } catch (IllegalMessageForConnectionStateException e) {
          logger.warn("wrong connection state", e);
        } catch (WonMessageBuilderException e) {
          logger.warn("close message could not be created", e);
        }

      }
      // ToDo (FS): define own message or forward the deactivate message?
      matcherProtocolMatcherClient.needDeactivated(need.getNeedURI(), newWonMessage);

    } else {

      logger.debug("DEACTIVATING need. needURI:{}", needURI);
      if (needURI == null) throw new IllegalArgumentException("needURI is not set");
      Need need = DataAccessUtils.loadNeed(needRepository, needURI);
      need.setState(NeedState.INACTIVE);
      need = needRepository.save(need);
      //close all connections
      Collection<URI> connectionURIs = connectionRepository.getConnectionURIsByNeedURIAndNotInState(need.getNeedURI
        (), ConnectionState.CLOSED);
      for (URI connURI : connectionURIs) {
        try {
          // ToDo (FS): create appropriate wonMessage
          ownerFacingConnectionCommunicationService.close(connURI, null, wonMessage);
        } catch (IllegalMessageForConnectionStateException e) {
          logger.warn("wrong connection state", e);
        }

      }
      matcherProtocolMatcherClient.needDeactivated(need.getNeedURI(), wonMessage);
    }
  }

  private boolean isNeedActive(final Need need) {
    return NeedState.ACTIVE == need.getState();
  }


  public void setOwnerProtocolOwnerService(final OwnerProtocolOwnerServiceClientSide ownerProtocolOwnerService) {
    this.ownerProtocolOwnerService = ownerProtocolOwnerService;
  }

  public void setOwnerFacingConnectionCommunicationService(final OwnerFacingConnectionCommunicationServiceImpl ownerFacingConnectionCommunicationService) {
    this.ownerFacingConnectionCommunicationService = ownerFacingConnectionCommunicationService;
  }

  public void setNeedInformationService(final NeedInformationService needInformationService) {
    this.needInformationService = needInformationService;
  }

  public void setURIService(final URIService uriService) {
    this.uriService = uriService;
  }

  public void setNeedRepository(final NeedRepository needRepository) {
    this.needRepository = needRepository;
  }

  public void setRdfStorage(RDFStorageService rdfStorage) {
    this.rdfStorage = rdfStorage;
  }

  public void setOwnerApplicationRepository(OwnerApplicationRepository ownerApplicationRepository) {
    this.ownerApplicationRepository = ownerApplicationRepository;
  }

  public void setMatcherProtocolMatcherClient(final MatcherProtocolMatcherServiceClientSide matcherProtocolMatcherClient) {
    this.matcherProtocolMatcherClient = matcherProtocolMatcherClient;
  }

  private URI getNeedURIFromWonMessage(final Dataset wonMessage) {
    URI needURI;
    needURI = WonRdfUtils.NeedUtils.getNeedURI(wonMessage);
    if (needURI == null) {
      throw new IllegalArgumentException("at least one RDF node must be of type won:Need");
    }
    return needURI;
  }

  // ToDo (FS): move to more general place where everybody can use the method
  private WonMessage createCloseWonMessage(URI connectionURI)
    throws WonMessageBuilderException {

    List<Connection> connections = connectionRepository.findByConnectionURI(connectionURI);
    if (connections.size() != 1)
      throw new IllegalArgumentException("no or too many connections found for ID " + connectionURI.toString());

    Connection connection = connections.get(0);
    Need need = needRepository.findByNeedURI(connection.getNeedURI()).get(0);
    Need remoteNeed = needRepository.findByNeedURI(connection.getRemoteNeedURI()).get(0);

    WonMessageBuilder builder = new WonMessageBuilder();
    return builder
      .setMessageURI(wonNodeInformationService.generateMessageEventURI())
      .setWonMessageType(WonMessageType.CLOSE)
      .setSenderURI(connection.getConnectionURI())
      .setSenderNeedURI(connection.getNeedURI())
      .setSenderNodeURI(need.getWonNodeURI())
      .setReceiverURI(connection.getRemoteConnectionURI())
      .setReceiverNeedURI(connection.getRemoteNeedURI())
      .setReceiverNodeURI(remoteNeed.getWonNodeURI())
      .setTimestamp(System.currentTimeMillis())
      .build();

  }
}

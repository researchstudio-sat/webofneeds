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
import org.javasimon.SimonManager;
import org.javasimon.Split;
import org.javasimon.Stopwatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import won.node.protocol.MatcherProtocolMatcherServiceClientSide;
import won.protocol.exception.WonMessageBuilderException;
import won.protocol.message.WonMessageDirection;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.model.Connection;
import won.protocol.model.ConnectionState;
import won.protocol.model.Need;
import won.protocol.model.OwnerApplication;
import won.protocol.repository.*;
import won.protocol.service.LinkedDataService;
import won.protocol.service.NeedInformationService;
import won.protocol.service.NeedManagementServiceRefactoring;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.WonRdfUtils;
import won.protocol.util.linkeddata.LinkedDataSource;
import won.protocol.util.linkeddata.WonLinkedDataUtils;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * User: fkleedorfer
 * Date: 02.11.12
 */
/* TODO: The logic of the methods of this class has nothing to do with JMS. should be merged with NeedManagementServiceImpl class. The only change was made in createNeed method, where the concept of authorizedApplications for each need was introduced.
 */
@Component
public class NeedManagementServiceImplRefactoring implements NeedManagementServiceRefactoring
{
  final Logger logger = LoggerFactory.getLogger(getClass());

  private MatcherProtocolMatcherServiceClientSide matcherProtocolMatcherClient;
  //used to close connections when a need is deactivated

  private NeedInformationService needInformationService;
  private URIService uriService;

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
  @Autowired
  private LinkedDataSource linkedDataSource;





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




  public void setNeedInformationService(final NeedInformationService needInformationService) {
    this.needInformationService = needInformationService;
  }

  public void setURIService(final URIService uriService) {
    this.uriService = uriService;
  }

  public void setNeedRepository(final NeedRepository needRepository) {
    this.needRepository = needRepository;
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
  private WonMessage createCloseWonMessage(URI connectionURI, final boolean fromDeactivate, WonMessage wonMessage)
    throws WonMessageBuilderException {

    List<Connection> connections = connectionRepository.findByConnectionURI(connectionURI);
    if (connections.size() != 1)
      throw new IllegalArgumentException("no or too many connections found for ID " + connectionURI.toString());

    Connection connection = connections.get(0);
    if (!ConnectionState.closeOnNeedDeactivate(connection.getState())) return null;

    URI localWonNodeUri = WonLinkedDataUtils.getWonNodeURIForNeedOrConnectionURI(connection.getConnectionURI(),
                                                                         linkedDataSource);
    URI remoteWonNodeUri = null;
    if (connection.getRemoteConnectionURI() != null) {
      if(fromDeactivate){
        WonMessageBuilder builder = new WonMessageBuilder();
        return builder.forward(wonMessage).setMessagePropertiesForClose(
          wonNodeInformationService.generateEventURI(),
          connection.getConnectionURI(),
          connection.getNeedURI(),
          localWonNodeUri,
          connection.getConnectionURI(),
          connection.getNeedURI(),
          localWonNodeUri)
          .setWonMessageDirection(WonMessageDirection.FROM_SYSTEM)
          .build();

      }else{
        remoteWonNodeUri = WonLinkedDataUtils.getWonNodeURIForNeedOrConnectionURI(connection.getRemoteConnectionURI(),
                                                                                  linkedDataSource);
        WonMessageBuilder builder = new WonMessageBuilder();
        return builder.forward(wonMessage).setMessagePropertiesForClose(
          wonNodeInformationService.generateEventURI(),
          connection.getConnectionURI(),
          connection.getNeedURI(),
          localWonNodeUri,
          connection.getRemoteConnectionURI(),
          connection.getRemoteNeedURI(),
          remoteWonNodeUri)
          .setWonMessageDirection(WonMessageDirection.FROM_EXTERNAL)
                      .build();
      }

    } else {
      WonMessageBuilder builder = new WonMessageBuilder();
      return builder.forward(wonMessage).setMessagePropertiesForLocalOnlyClose(
        wonNodeInformationService.generateEventURI(),
        connection.getConnectionURI(),
        connection.getNeedURI(),
        localWonNodeUri)
                    .build();

    }


  }
}

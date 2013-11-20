/*
 * Copyright 2012  Research Studios Austria Forschungsges.m.b.H.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package won.node.service.impl;

import com.hp.hpl.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import won.protocol.exception.IllegalNeedContentException;
import won.protocol.exception.NoSuchNeedException;
import won.protocol.exception.WonProtocolException;
import won.protocol.model.Need;
import won.protocol.model.NeedState;
import won.protocol.model.OwnerApplication;
import won.protocol.owner.OwnerProtocolOwnerService;
import won.protocol.owner.OwnerProtocolOwnerServiceClient;
import won.protocol.repository.NeedRepository;
import won.protocol.repository.OwnerApplicationRepository;
import won.protocol.service.ConnectionCommunicationService;
import won.protocol.service.NeedInformationService;
import won.protocol.service.NeedManagementService;
import won.protocol.util.DataAccessUtils;
import won.protocol.util.RdfUtils;

import java.net.URI;
import java.util.Collection;
import java.util.List;

/**
 * User: fkleedorfer
 * Date: 02.11.12
 */
@Component
public class NeedManagementServiceImpl implements NeedManagementService
{
  final Logger logger = LoggerFactory.getLogger(getClass());
  private OwnerProtocolOwnerServiceClient ownerProtocolOwnerService;
  //used to close connections when a need is deactivated
  private ConnectionCommunicationService ownerFacingConnectionCommunicationService;
  private NeedInformationService needInformationService;
  private URIService URIService;
  private RDFStorageService rdfStorage;

  @Autowired
  private NeedRepository needRepository;

  @Autowired
  private OwnerApplicationRepository ownerApplicationRepository;

  @Override
  public URI createNeed(final URI ownerURI, final Model content, final boolean activate, String ownerApplicationID) throws IllegalNeedContentException
  {
    if (ownerURI == null) throw new IllegalArgumentException("ownerURI is not set");
    Need need = new Need();
    List<OwnerApplication> ownerApplicationList = ownerApplicationRepository.findByOwnerApplicationId(ownerApplicationID);
    OwnerApplication ownerApplication = ownerApplicationList.get(0);
    need.setState(activate ? NeedState.ACTIVE : NeedState.INACTIVE);
    need.setOwnerURI(ownerURI);
    need = needRepository.save(need);
    //now, create the need URI and save again
    need.setNeedURI(URIService.createNeedURI(need));

    need.getAuthorizedApplications().add(ownerApplication);
    need = needRepository.saveAndFlush(need);
    String baseURI = need.getNeedURI().toString();
    RdfUtils.replaceBaseURI(content, baseURI);

    rdfStorage.storeContent(need, content);

    return need.getNeedURI();
  }


  @Override
  public void activate(final URI needURI) throws NoSuchNeedException
  {
    if (needURI == null) throw new IllegalArgumentException("needURI is not set");
    Need need = DataAccessUtils.loadNeed(needRepository, needURI);
    need.setState(NeedState.ACTIVE);
    needRepository.saveAndFlush(need);
  }

  @Override
  public void deactivate(final URI needURI) throws NoSuchNeedException
  {
    if (needURI == null) throw new IllegalArgumentException("needURI is not set");
    Need need = DataAccessUtils.loadNeed(needRepository, needURI);
    need.setState(NeedState.INACTIVE);
    need = needRepository.saveAndFlush(need);
    //close all connections
    Collection<URI> connectionURIs = needInformationService.listConnectionURIs(need.getNeedURI());
    for (URI connURI : connectionURIs) {
      try {
        ownerFacingConnectionCommunicationService.close(connURI, null);
      } catch (WonProtocolException e) {
        logger.warn("caught exception when trying to close connection", e);
      }
    }
  }

  private boolean isNeedActive(final Need need)
  {
    return NeedState.ACTIVE == need.getState();
  }


  public void setOwnerProtocolOwnerService(final OwnerProtocolOwnerServiceClient ownerProtocolOwnerService)
  {
    this.ownerProtocolOwnerService = ownerProtocolOwnerService;
  }

  public void setOwnerFacingConnectionCommunicationService(final ConnectionCommunicationService ownerFacingConnectionCommunicationService)
  {
    this.ownerFacingConnectionCommunicationService = ownerFacingConnectionCommunicationService;
  }

  public void setNeedInformationService(final NeedInformationService needInformationService)
  {
    this.needInformationService = needInformationService;
  }

  public void setURIService(final URIService URIService)
  {
    this.URIService = URIService;
  }

  public void setNeedRepository(final NeedRepository needRepository)
  {
    this.needRepository = needRepository;
  }

  public void setRdfStorage(RDFStorageService rdfStorage)
  {
    this.rdfStorage = rdfStorage;
  }

    public void setOwnerApplicationRepository(OwnerApplicationRepository ownerApplicationRepository) {
        this.ownerApplicationRepository = ownerApplicationRepository;
    }
}

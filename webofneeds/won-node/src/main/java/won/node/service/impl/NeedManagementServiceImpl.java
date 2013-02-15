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
import won.protocol.model.Match;
import won.protocol.model.Need;
import won.protocol.model.NeedState;
import won.protocol.owner.OwnerProtocolOwnerService;
import won.protocol.repository.MatchRepository;
import won.protocol.repository.NeedRepository;
import won.protocol.service.ConnectionCommunicationService;
import won.protocol.service.NeedInformationService;
import won.protocol.service.NeedManagementService;
import won.protocol.util.DataAccessUtils;

import java.net.URI;
import java.util.Collection;

/**
 * User: fkleedorfer
 * Date: 02.11.12
 */
@Component
public class NeedManagementServiceImpl implements NeedManagementService
{
  final Logger logger = LoggerFactory.getLogger(getClass());
  private OwnerProtocolOwnerService ownerProtocolOwnerService;
  //used to close connections when a need is deactivated
  private ConnectionCommunicationService ownerFacingConnectionCommunicationService;
  private NeedInformationService needInformationService;
  private URIService URIService;
  private RDFStorageService rdfStorage;

  @Autowired
  private NeedRepository needRepository;

  @Autowired
  private MatchRepository matchRepository;

  @Override
  public URI createNeed(final URI ownerURI, final Model content, final boolean activate) throws IllegalNeedContentException {
    if (ownerURI == null) throw new IllegalArgumentException("ownerURI is not set");
    //TODO: when we have RDF handling, check that the graph is valid here.

    Need need = new Need();
    need.setState(activate ? NeedState.ACTIVE : NeedState.INACTIVE);
    need.setOwnerURI(ownerURI);
    need = needRepository.save(need);
    //now, create the need URI and save again
    need.setNeedURI(URIService.createNeedURI(need));
    need = needRepository.saveAndFlush(need);
    rdfStorage.storeContent(need, content);

    return need.getNeedURI();
  }

  @Override
  public void activate(final URI needURI) throws NoSuchNeedException
  {
    if (needURI == null) throw new IllegalArgumentException("needURI is not set");
    Need need = DataAccessUtils.loadNeed(needRepository, needURI);
    need.setState(NeedState.ACTIVE);
    need = needRepository.saveAndFlush(need);
  }

  @Override
  public void deactivate(final URI needURI) throws NoSuchNeedException
  {
    if (needURI == null) throw new IllegalArgumentException("needURI is not set");
    Need need = DataAccessUtils.loadNeed(needRepository, needURI);
    need.setState(NeedState.INACTIVE);
    need = needRepository.saveAndFlush(need);
    //close all connections
    //TODO: add a filter to the method/repo to filter only non-closed connections
    Collection<URI> connectionURIs = needInformationService.listConnectionURIs(need.getNeedURI());
    for (URI connURI : connectionURIs){
      try {
        ownerFacingConnectionCommunicationService.close(connURI);
      } catch (WonProtocolException e) {
        logger.debug("caught exception when trying to close connection", e);
      }
    }
  }

  @Override
  public Collection<Match> getMatches(final URI needURI) throws NoSuchNeedException
  {
    if (needURI == null) throw new IllegalArgumentException("needURI is not set");
    Need need = DataAccessUtils.loadNeed(needRepository, needURI);
    return matchRepository.findByFromNeed(need.getNeedURI());
  }



  private boolean isNeedActive(final Need need) {
    return NeedState.ACTIVE == need.getState();
  }


  public void setOwnerProtocolOwnerService(final OwnerProtocolOwnerService ownerProtocolOwnerService)
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

  public void setMatchRepository(final MatchRepository matchRepository)
  {
    this.matchRepository = matchRepository;
  }

    public void setRdfStorage(RDFStorageService rdfStorage) {
        this.rdfStorage = rdfStorage;
    }
}

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

import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import won.protocol.exception.IllegalNeedContentException;
import won.protocol.exception.NoSuchNeedException;
import won.protocol.exception.WonProtocolException;
import won.protocol.model.Facet;
import won.protocol.model.Need;
import won.protocol.model.NeedState;
import won.protocol.owner.OwnerProtocolOwnerServiceClient;
import won.protocol.repository.FacetRepository;
import won.protocol.repository.NeedRepository;
import won.protocol.service.ConnectionCommunicationService;
import won.protocol.service.NeedInformationService;
import won.protocol.service.NeedManagementService;
import won.protocol.util.DataAccessUtils;
import won.protocol.util.RdfUtils;
import won.protocol.vocabulary.WON;

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
  private OwnerProtocolOwnerServiceClient ownerProtocolOwnerService;
  //used to close connections when a need is deactivated
  private ConnectionCommunicationService ownerFacingConnectionCommunicationService;
  private NeedInformationService needInformationService;
  private URIService URIService;
  private RDFStorageService rdfStorage;

  @Autowired
  private NeedRepository needRepository;
  @Autowired
  private FacetRepository facetRepository;

  @Override
  public URI createNeed(final URI ownerURI, final Model content, final boolean activate, String ownerApplicationID) throws IllegalNeedContentException
  {
    if (ownerURI == null) throw new IllegalArgumentException("ownerURI is not set");
    Need need = new Need();
    need.setState(activate ? NeedState.ACTIVE : NeedState.INACTIVE);
    need.setOwnerURI(ownerURI);
    need = needRepository.save(need);
    //now, create the need URI and save again
    need.setNeedURI(URIService.createNeedURI(need));
    need = needRepository.saveAndFlush(need);
    String baseURI = need.getNeedURI().toString();
    RdfUtils.replaceBaseURI(content, baseURI);

    rdfStorage.storeContent(need, content);

    ResIterator needIt = content.listSubjectsWithProperty(RDF.type, WON.NEED);
    if (!needIt.hasNext()) throw new IllegalArgumentException("at least one RDF node must be of type won:Need");

    Resource needRes = needIt.next();
    logger.debug("processing need resource {}", needRes.getURI());

    StmtIterator stmtIterator = content.listStatements(needRes, WON.HAS_FACET, (RDFNode) null);
    if(!stmtIterator.hasNext())
      throw new IllegalArgumentException("at least one RDF node must be of type won:HAS_FACET");
    else
      //TODO: check if there is a implementation for the facet on the node
      do {
        Facet facet = new Facet();
        facet.setNeedURI(need.getNeedURI());
        facet.setTypeURI(URI.create(stmtIterator.next().getObject().asResource().getURI()));
        facetRepository.save(facet);
      } while(stmtIterator.hasNext());

    return need.getNeedURI();
  }

    @Override
    public void authorizeOwnerApplicationForNeed(String ownerApplicationID, URI needURI) {
        //To change body of implemented methods use File | Settings | File Templates.
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
}

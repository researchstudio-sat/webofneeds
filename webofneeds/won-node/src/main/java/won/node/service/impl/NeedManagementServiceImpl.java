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
import won.protocol.model.OwnerApplication;
import won.protocol.owner.OwnerProtocolOwnerServiceClientSide;
import won.protocol.repository.FacetRepository;
import won.protocol.repository.NeedRepository;
import won.protocol.repository.OwnerApplicationRepository;
import won.protocol.service.NeedInformationService;
import won.protocol.service.NeedManagementService;
import won.protocol.util.DataAccessUtils;
import won.protocol.util.RdfUtils;
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
  //used to close connections when a need is deactivated
  private OwnerFacingConnectionCommunicationServiceImpl ownerFacingConnectionCommunicationService;
  private NeedInformationService needInformationService;
  private URIService URIService;
  private RDFStorageService rdfStorage;

  @Autowired
  private NeedRepository needRepository;

    @Autowired
    FacetRepository facetRepository;
    @Autowired
    private OwnerApplicationRepository ownerApplicationRepository;

  @Override
  public URI createNeed(final URI ownerURI, final Model content, final boolean activate, String ownerApplicationID) throws IllegalNeedContentException
  {
    logger.info("CREATING need. OwnerURI:{}, OwnerApplicationId:{}",ownerURI, ownerApplicationID);
    if (ownerURI == null) throw new IllegalArgumentException("ownerURI is not set");
    Need need = new Need();
    need.setState(activate ? NeedState.ACTIVE : NeedState.INACTIVE);
    need.setOwnerURI(ownerURI);
    need = needRepository.save(need);
    //now, create the need URI and save again
    need.setNeedURI(URIService.createNeedURI(need));
    need.setWonNodeURI(URI.create(URIService.getGeneralURIPrefix()));
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

    authorizeOwnerApplicationForNeed(ownerApplicationID,need.getNeedURI());
    return need.getNeedURI();
  }
    @Override
    public void authorizeOwnerApplicationForNeed(final String ownerApplicationID, URI needURI){
        logger.info("AUTHORIZING owner application. needURI:{}, OwnerApplicationId:{}",needURI, ownerApplicationID);
        Need need = needRepository.findByNeedURI(needURI).get(0);
        List<OwnerApplication> ownerApplications = ownerApplicationRepository.findByOwnerApplicationId(ownerApplicationID);
        if(ownerApplications.size()>0)  {
            OwnerApplication ownerApplication = ownerApplicationRepository.findByOwnerApplicationId(ownerApplicationID).get(0);
            List<OwnerApplication> authorizedApplications = new ArrayList<>();
            authorizedApplications.add(ownerApplication);
            need.setAuthorizedApplications(authorizedApplications);
        }

        else{
            List<OwnerApplication> ownerApplicationList = new ArrayList<>();
            OwnerApplication ownerApplication = new OwnerApplication();
            ownerApplication.setOwnerApplicationId(ownerApplicationID);
            ownerApplicationList.add(ownerApplication);
            need.setAuthorizedApplications(ownerApplicationList);
            logger.info("setting OwnerApp ID: "+ownerApplicationList.get(0));
        }
        need = needRepository.saveAndFlush(need);
    }

    @Override
    public void activate(final URI needURI) throws NoSuchNeedException
    {
        logger.info("ACTIVATING need. needURI:{}",needURI);
        if (needURI == null) throw new IllegalArgumentException("needURI is not set");
        Need need = DataAccessUtils.loadNeed(needRepository, needURI);
        need.setState(NeedState.ACTIVE);
        logger.info("Setting Need State: "+ need.getState());
        needRepository.saveAndFlush(need);
    }

    @Override
    public void deactivate(final URI needURI) throws NoSuchNeedException
    {
        logger.info("DEACTIVATING need. needURI:{}",needURI);
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


    public void setOwnerProtocolOwnerService(final OwnerProtocolOwnerServiceClientSide ownerProtocolOwnerService)
    {
        this.ownerProtocolOwnerService = ownerProtocolOwnerService;
    }

    public void setOwnerFacingConnectionCommunicationService(final OwnerFacingConnectionCommunicationServiceImpl ownerFacingConnectionCommunicationService)
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

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import won.protocol.repository.rdfstorage.RDFStorageService;
import won.protocol.exception.NoSuchConnectionException;
import won.protocol.exception.NoSuchNeedException;
import won.protocol.model.Connection;
import won.protocol.model.ConnectionEvent;
import won.protocol.model.Need;
import won.protocol.model.NeedState;
import won.protocol.repository.ConnectionRepository;
import won.protocol.repository.EventRepository;
import won.protocol.repository.NeedRepository;
import won.protocol.service.NeedInformationService;
import won.protocol.util.DataAccessUtils;

import java.net.URI;
import java.util.Collection;
import java.util.List;

/**
 * User: fkleedorfer
 * Date: 02.11.12
 */
@Component
public class NeedInformationServiceImpl implements NeedInformationService
{

  @Autowired
  private RDFStorageService rdfStorage;
  @Autowired
  private NeedRepository needRepository;
  @Autowired
  private ConnectionRepository connectionRepository;
  @Autowired
  private EventRepository eventRepository;
  @Autowired
  private URIService uriService;

  private static final int DEFAULT_PAGE_SIZE = 500;

  private int pageSize = DEFAULT_PAGE_SIZE;

  @Override
  public Collection<URI> listNeedURIs()
  {
    return needRepository.getAllNeedURIs();
  }

  @Override
  public Collection<URI> listNeedURIs(int page)
  {
    return needRepository.getAllNeedURIs(new PageRequest(page, this.pageSize));
  }

  @Override
  public Collection<URI> listConnectionURIs(final URI needURI) throws NoSuchNeedException
  {
    return connectionRepository.getAllConnectionURIsForNeedURI(needURI);
  }

  @Override
  public Collection<URI> listConnectionURIs()
  {
    return connectionRepository.getAllConnectionURIs();
  }

  @Override
  public Collection<URI> listConnectionURIs(int page)
  {
    return connectionRepository.getAllConnectionURIs(new PageRequest(page, this.pageSize));
  }

  @Override
  public Collection<URI> listConnectionURIs(final URI needURI, int page) throws NoSuchNeedException
  {
    return connectionRepository.getAllConnectionURIsForNeedURI(needURI, new PageRequest(page, this.pageSize));
  }


  @Override
  public List<ConnectionEvent> readEvents(final URI connectionURI) throws NoSuchConnectionException
  {
    return eventRepository.findByConnectionURI(connectionURI);
  }

  /**
   * Returns null if no event found.
   * @param eventURI
   * @return
   */
  @Override
  public ConnectionEvent readEvent(final URI eventURI)
  {
    return eventRepository.findOne(uriService.getEventIdFromEventURI(eventURI));
  }


  @Override
  public Need readNeed(final URI needURI) throws NoSuchNeedException
  {
    if (needURI == null) throw new IllegalArgumentException("needURI is not set");
    return (DataAccessUtils.loadNeed(needRepository, needURI));
  }

  @Override
  public Model readNeedContent(final URI needURI) throws NoSuchNeedException
  {
    if (needURI == null) throw new IllegalArgumentException("needURI is not set");
    Need need = DataAccessUtils.loadNeed(needRepository, needURI);
    return rdfStorage.loadContent(need.getNeedURI());
  }

  @Override
  public Connection readConnection(final URI connectionURI) throws NoSuchConnectionException
  {
    if (connectionURI == null) throw new IllegalArgumentException("connectionURI is not set");
    return DataAccessUtils.loadConnection(connectionRepository, connectionURI);
  }

  //TODO implement RDF handling!
  @Override
  public Model readConnectionContent(final URI connectionURI) throws NoSuchConnectionException
  {
    return null;
  }

  public void setNeedRepository(final NeedRepository needRepository)
  {
    this.needRepository = needRepository;
  }

  public void setConnectionRepository(final ConnectionRepository connectionRepository)
  {
    this.connectionRepository = connectionRepository;
  }

  private boolean isNeedActive(final Need need)
  {
    return NeedState.ACTIVE == need.getState();
  }

  public void setPageSize(int pageSize)
  {
    this.pageSize = pageSize;
  }

  public void setRdfStorage(RDFStorageService rdfStorage)
  {
    this.rdfStorage = rdfStorage;
  }
}

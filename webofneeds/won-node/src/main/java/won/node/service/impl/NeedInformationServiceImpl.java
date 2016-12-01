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
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import won.protocol.exception.NoSuchConnectionException;
import won.protocol.exception.NoSuchNeedException;
import won.protocol.message.WonMessageType;
import won.protocol.model.*;
import won.protocol.repository.ConnectionRepository;
import won.protocol.repository.MessageEventRepository;
import won.protocol.repository.NeedRepository;
import won.protocol.repository.rdfstorage.RDFStorageService;
import won.protocol.service.NeedInformationService;
import won.protocol.util.DataAccessUtils;

import java.net.URI;
import java.util.Collection;
import java.util.Date;
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
  private MessageEventRepository messageEventRepository;
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
  public Slice<URI> listNeedURIs(int page, Integer preferedPageSize, NeedState needState)
  {
    int pageSize = this.pageSize;
    int pageNum = page - 1;
    if (preferedPageSize != null && preferedPageSize < this.pageSize) {
      pageSize = preferedPageSize;
    }
    Slice<URI> slice = null;
    if (needState == null) {
      slice = needRepository.getAllNeedURIs(new PageRequest(pageNum, pageSize, Sort.Direction.DESC, "creationDate"));
    } else {
      slice = needRepository.getAllNeedURIs(needState, new PageRequest(pageNum, pageSize, Sort.Direction.DESC,
                                                                       "creationDate"));
    }
    return slice;
  }

  @Override
  public Slice<URI> listNeedURIsBefore(URI needURI, Integer preferedPageSize, NeedState needState)
  {
    Need referenceNeed = needRepository.findOneByNeedURI(needURI);
    Date referenceDate = referenceNeed.getCreationDate();
    int pageSize = this.pageSize;
    if (preferedPageSize != null && preferedPageSize < this.pageSize) {
      pageSize = preferedPageSize;
    }
    Slice<URI> slice = null;
    if (needState == null) {
      slice = needRepository.getNeedURIsBefore(referenceDate, new PageRequest(0, pageSize, Sort
        .Direction.DESC, "creationDate"));
    } else {
      slice = needRepository.getNeedURIsBefore(referenceDate, needState, new PageRequest(0, pageSize, Sort
        .Direction.DESC, "creationDate"));
    }
    return slice;
  }

  @Override
  public Slice<URI> listNeedURIsAfter(URI needURI, Integer preferedPageSize, NeedState needState)
  {
    Need referenceNeed = needRepository.findOneByNeedURI(needURI);
    Date referenceDate = referenceNeed.getCreationDate();
    int pageSize = this.pageSize;
    if (preferedPageSize != null && preferedPageSize < this.pageSize) {
      pageSize = preferedPageSize;
    }
    Slice<URI> slice = null;
    if (needState == null) {
      slice = needRepository.getNeedURIsAfter(referenceDate, new PageRequest(0, pageSize, Sort.Direction.ASC,
                                                                             "creationDate"));
    } else {
      slice = needRepository.getNeedURIsAfter(referenceDate, needState, new PageRequest(0, pageSize, Sort.Direction.ASC,
                                                                                        "creationDate"));
    }
    return slice;
  }


  @Override
  public Collection<URI> listConnectionURIs()
  {
    return connectionRepository.getAllConnectionURIs();
  }

  @Override
  public Slice<URI> listConnectionURIs(int page, Integer preferedPageSize, Date timeSpot)
  {
    int pageSize = getPageSize(preferedPageSize);
    int pageNum = page - 1;
    Slice<URI> slice;
    if (timeSpot == null) {
      slice = connectionRepository.getConnectionURIByLatestActivity(
        new PageRequest(pageNum, pageSize, Sort.Direction.DESC, "max(msg.creationDate)"));
    } else {
      slice = connectionRepository.getConnectionURIByLatestActivity(
        timeSpot,
        new PageRequest(pageNum, pageSize, Sort.Direction.DESC, "max(msg.creationDate)"));
    }
    return slice;
  }

  @Override
  public Slice<URI> listConnectionURIsBefore(
    final URI resumeConnURI, final Integer preferredPageSize, final Date timeSpot) {

    Date resume = messageEventRepository.findMaxActivityDateOfParentAtTime(resumeConnURI, timeSpot);
    int pageSize = getPageSize(preferredPageSize);
    Slice<URI> slice = null;
    slice = connectionRepository.getConnectionURIsBeforeByLatestActivity(
      resume, timeSpot, new PageRequest(0, pageSize, Sort.Direction.DESC, "max(msg.creationDate)"));
    return slice;
  }

  @Override
  public Slice<URI> listConnectionURIsAfter(
    final URI resumeConnURI, final Integer preferredPageSize, final Date timeSpot) {

    Date resume = messageEventRepository.findMaxActivityDateOfParentAtTime(resumeConnURI, timeSpot);
    int pageSize = getPageSize(preferredPageSize);
    Slice<URI> slice = null;
    slice = connectionRepository.getConnectionURIsAfterByLatestActivity(
      resume, timeSpot, new PageRequest(0, pageSize, Sort.Direction.ASC, "max(msg.creationDate)"));
    return slice;
  }

  @Override
  public Collection<URI> listConnectionURIs(final URI needURI) throws NoSuchNeedException
  {
    return connectionRepository.getAllConnectionURIsForNeedURI(needURI);
  }

  @Override
  public Slice<URI> listConnectionURIs(final URI needURI, int page, Integer preferedPageSize, WonMessageType
    messageType, Date timeSpot) {
    Slice<URI> slice = null;
    int pageSize = getPageSize(preferedPageSize);
    int pageNum = page - 1;
    PageRequest pageRequest = new PageRequest(pageNum, pageSize, Sort.Direction.DESC, "max(msg.creationDate)");
    if (messageType == null) {
      if (timeSpot == null) {
        slice = connectionRepository.getConnectionURIByLatestActivity(needURI, pageRequest);
      } else {
        slice = connectionRepository.getConnectionURIByLatestActivity(needURI, timeSpot, pageRequest);
      }
    } else {
      if (timeSpot == null) {
        slice = connectionRepository.getConnectionURIByLatestActivity(needURI, messageType, pageRequest);
      } else {
        slice = connectionRepository.getConnectionURIByLatestActivity(needURI, messageType, timeSpot, pageRequest);
      }
    }
    return slice;
  }

  @Override
  public Slice listConnectionURIsBefore(final URI needURI, final URI resumeConnURI, final Integer preferredPageSize,
                                        WonMessageType messageType, final Date timeSpot) {

    Date resume = null;
    int pageSize = getPageSize(preferredPageSize);
    Slice<URI> slice = null;
    if (messageType == null) {
      resume = messageEventRepository.findMaxActivityDateOfParentAtTime(resumeConnURI, timeSpot);
      slice = connectionRepository.getConnectionURIsBeforeByLatestActivity(
        needURI, resume, timeSpot, new PageRequest(0, pageSize, Sort.Direction.DESC, "max(msg.creationDate)"));
    } else {
      resume = messageEventRepository.findMaxActivityDateOfParentAtTime(resumeConnURI, messageType, timeSpot);
      slice = connectionRepository.getConnectionURIsBeforeByLatestActivity(
        needURI, resume, messageType, timeSpot, new PageRequest(
          0, pageSize, Sort.Direction.DESC, "max(msg.creationDate)"));
    }
    return slice;
  }

  @Override
  public Slice listConnectionURIsAfter(final URI needURI, final URI resumeConnURI, final Integer preferredPageSize,
                                        final WonMessageType messageType, final Date timeSpot) {

    Date resume = null;
    int pageSize = getPageSize(preferredPageSize);
    Slice<URI> slice = null;
    if (messageType == null) {
      resume = messageEventRepository.findMaxActivityDateOfParentAtTime(resumeConnURI, timeSpot);
      slice = connectionRepository.getConnectionURIsAfterByLatestActivity(
        needURI, resume, timeSpot, new PageRequest(0, pageSize, Sort.Direction.ASC, "max(msg.creationDate)"));
    } else {
      resume = messageEventRepository.findMaxActivityDateOfParentAtTime(resumeConnURI, messageType, timeSpot);
      slice = connectionRepository.getConnectionURIsAfterByLatestActivity(
        needURI, resume, messageType, timeSpot, new PageRequest(
          0, pageSize, Sort.Direction.ASC, "max(msg.creationDate)"));
    }
    return slice;
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
    return rdfStorage.loadModel(need.getNeedURI());
  }

  @Override
  public Connection readConnection(final URI connectionURI) throws NoSuchConnectionException
  {
    if (connectionURI == null) throw new IllegalArgumentException("connectionURI is not set");
    return DataAccessUtils.loadConnection(connectionRepository, connectionURI);
  }

  @Override
  public DataWithEtag<Connection> readConnection(final URI connectionURI, String etag)
  {
    if (connectionURI == null) throw new IllegalArgumentException("connectionURI is not set");
    Connection con = null;
    if (etag == null) {
      con = connectionRepository.findOneByConnectionURI(connectionURI);
    } else {
      Long version = Long.valueOf(etag);
      con = connectionRepository.findOneByConnectionURIAndVersionNot(connectionURI, version);
    }
    return new DataWithEtag<>(con, con == null ? etag : Long.toString(con.getVersion()), etag);
  }


  //TODO implement RDF handling!
  @Override
  public Model readConnectionContent(final URI connectionURI) throws NoSuchConnectionException
  {
    return null;
  }


  @Override
  public List<URI> listConnectionEventURIs(URI connectionUri) {
    return messageEventRepository.getMessageURIsByParentURI(connectionUri);
  }

  @Override
  public Slice<URI> listConnectionEventURIs(
    URI connectionUri, int page, Integer preferedPageSize, WonMessageType messageType)
  {
    int pageSize = getPageSize(preferedPageSize);
    int pageNum = page - 1;
    Slice<URI> slice = null;
    if (messageType == null) {
      slice = messageEventRepository.getMessageURIsByParentURI(
        connectionUri, new PageRequest(pageNum, pageSize, Sort.Direction.DESC, "creationDate"));
    } else {
      slice = messageEventRepository.getMessageURIsByParentURI(
        connectionUri, messageType, new PageRequest(pageNum, pageSize, Sort.Direction.DESC, "creationDate"));
    }
    return slice;
  }

  @Override
  public Slice<URI> listConnectionEventURIsAfter(URI connectionUri, URI msgURI, Integer preferredPageSize,
                                                 WonMessageType msgType) {
      MessageEventPlaceholder referenceMsg = messageEventRepository.findOneByMessageURI(msgURI);
      Date referenceDate = referenceMsg.getCreationDate();
      int pageSize = getPageSize(preferredPageSize);
      Slice<URI> slice = null;
      if (msgType == null) {
        slice = messageEventRepository.getMessageURIsByParentURIAfter(
          connectionUri, referenceDate, new PageRequest(0, pageSize, Sort.Direction.ASC, "creationDate"));
      } else {
        slice = messageEventRepository.getMessageURIsByParentURIAfter(
          connectionUri, referenceDate, msgType, new PageRequest(0, pageSize, Sort.Direction.ASC, "creationDate"));
      }
      return slice;
  }

  @Override
  public Slice<URI> listConnectionEventURIsBefore(final URI connectionUri, final URI msgURI, final Integer
    preferredPageSize, final WonMessageType msgType) {
    MessageEventPlaceholder referenceMsg = messageEventRepository.findOneByMessageURI(msgURI);
    Date referenceDate = referenceMsg.getCreationDate();
    int pageSize = getPageSize(preferredPageSize);
    Slice<URI> slice = null;
    if (msgType == null) {
      slice = messageEventRepository.getMessageURIsByParentURIBefore(
        connectionUri, referenceDate, new PageRequest(0, pageSize, Sort.Direction.DESC, "creationDate"));
    } else {
      slice = messageEventRepository.getMessageURIsByParentURIBefore(
        connectionUri, referenceDate, msgType, new PageRequest(0, pageSize, Sort.Direction.DESC, "creationDate"));
    }
    return slice;
  }

  private int getPageSize(final Integer preferredPageSize) {
    int pageSize = this.pageSize;
    if (preferredPageSize != null && preferredPageSize < this.pageSize) {
      pageSize = preferredPageSize;
    }
    return pageSize;
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

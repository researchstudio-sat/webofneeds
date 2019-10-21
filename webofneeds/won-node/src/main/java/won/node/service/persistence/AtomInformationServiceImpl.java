/*
 * Copyright 2012 Research Studios Austria Forschungsges.m.b.H. Licensed under
 * the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable
 * law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package won.node.service.persistence;

import java.net.URI;
import java.util.Collection;
import java.util.Date;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import won.node.service.nodeconfig.URIService;
import won.protocol.exception.NoSuchAtomException;
import won.protocol.exception.NoSuchConnectionException;
import won.protocol.exception.NoSuchMessageException;
import won.protocol.message.WonMessageType;
import won.protocol.model.Atom;
import won.protocol.model.AtomState;
import won.protocol.model.Connection;
import won.protocol.model.DataWithEtag;
import won.protocol.model.MessageEvent;
import won.protocol.repository.AtomRepository;
import won.protocol.repository.ConnectionRepository;
import won.protocol.repository.MessageEventRepository;
import won.protocol.util.DataAccessUtils;

/**
 * User: fkleedorfer Date: 02.11.12
 */
@Component
public class AtomInformationServiceImpl implements AtomInformationService {
    @Autowired
    private AtomRepository atomRepository;
    @Autowired
    private ConnectionRepository connectionRepository;
    @Autowired
    private MessageEventRepository messageEventRepository;
    @Autowired
    private URIService uriService;
    private static final int DEFAULT_PAGE_SIZE = 500;
    private int pageSize = DEFAULT_PAGE_SIZE;

    @Override
    public Collection<URI> listAtomURIs() {
        return atomRepository.getAllAtomURIs(null);
    }

    @Override
    public Collection<URI> listAtomURIs(AtomState atomState) {
        return atomRepository.getAllAtomURIs(atomState);
    }

    @Override
    public Slice<URI> listPagedAtomURIs(int page, Integer preferedPageSize, AtomState atomState) {
        int pageSize = this.pageSize;
        int pageNum = page - 1;
        if (preferedPageSize != null && preferedPageSize < this.pageSize) {
            pageSize = preferedPageSize;
        }
        // use 'creationDate' to keep a constant atom order over requests
        return atomRepository.getAllAtomURIs(atomState,
                        new PageRequest(pageNum, pageSize, Sort.Direction.DESC, "creationDate"));
    }

    @Override
    public Slice<URI> listPagedAtomURIsBefore(URI atomURI, Integer preferedPageSize, AtomState atomState) {
        Atom referenceAtom = atomRepository.findOneByAtomURI(atomURI)
                        .orElseThrow(() -> new NoSuchAtomException(atomURI));
        Date referenceDate = referenceAtom.getCreationDate();
        int pageSize = this.pageSize;
        if (preferedPageSize != null && preferedPageSize < this.pageSize) {
            pageSize = preferedPageSize;
        }
        Slice<URI> slice = null;
        if (atomState == null) {
            // use 'creationDate' to keep a constant atom order over requests
            slice = atomRepository.getAtomURIsBefore(referenceDate,
                            new PageRequest(0, pageSize, Sort.Direction.DESC, "creationDate"));
        } else {
            // use 'creationDate' to keep a constant atom order over requests
            slice = atomRepository.getAtomURIsBefore(referenceDate, atomState,
                            new PageRequest(0, pageSize, Sort.Direction.DESC, "creationDate"));
        }
        return slice;
    }

    @Override
    public Collection<URI> listAtomURIsModifiedAfter(Date modifiedAfter, AtomState atomState) {
        return atomRepository.getAllAtomURIsModifiedAfter(modifiedAfter, atomState);
    }

    @Override
    public Collection<URI> listAtomURIsCreatedAfter(Date createdAfter, AtomState atomState) {
        return atomRepository.getAllAtomURIsCreatedAfter(createdAfter, atomState);
    }

    @Override
    public Slice<URI> listPagedAtomURIsAfter(URI atomURI, Integer preferedPageSize, AtomState atomState) {
        Atom referenceAtom = atomRepository.findOneByAtomURI(atomURI)
                        .orElseThrow(() -> new NoSuchAtomException(atomURI));
        Date referenceDate = referenceAtom.getCreationDate();
        int pageSize = this.pageSize;
        if (preferedPageSize != null && preferedPageSize < this.pageSize) {
            pageSize = preferedPageSize;
        }
        Slice<URI> slice = null;
        if (atomState == null) {
            // use 'creationDate' to keep a constant atom order over requests
            slice = atomRepository.getAtomURIsAfter(referenceDate,
                            new PageRequest(0, pageSize, Sort.Direction.ASC, "creationDate"));
        } else {
            // use 'creationDate' to keep a constant atom order over requests
            slice = atomRepository.getAtomURIsAfter(referenceDate, atomState,
                            new PageRequest(0, pageSize, Sort.Direction.ASC, "creationDate"));
        }
        return slice;
    }

    @Override
    public Collection<URI> listConnectionURIs() {
        return connectionRepository.getAllConnectionURIs();
    }

    @Override
    public Collection<Connection> listConnections() {
        return connectionRepository.getAllConnections();
    }

    @Override
    public Collection<Connection> listModifiedConnectionsAfter(Date modifiedAfter) {
        return connectionRepository.findModifiedConnectionsAfter(modifiedAfter);
    }

    @Override
    @Deprecated
    public Slice<URI> listConnectionURIs(int page, Integer preferedPageSize, Date timeSpot) {
        int pageSize = getPageSize(preferedPageSize);
        int pageNum = page - 1;
        Slice<URI> slice;
        if (timeSpot == null) {
            // use 'min(msg.creationDate)' to keep a constant connection order over requests
            slice = connectionRepository.getConnectionURIByActivityDate(
                            new PageRequest(pageNum, pageSize, Sort.Direction.DESC, "min(msg.creationDate)"));
        } else {
            // use 'min(msg.creationDate)' to keep a constant connection order over requests
            slice = connectionRepository.getConnectionURIByActivityDate(timeSpot,
                            new PageRequest(pageNum, pageSize, Sort.Direction.DESC, "min(msg.creationDate)"));
        }
        return slice;
    }

    @Override
    public Slice<Connection> listConnections(int page, Integer preferedPageSize, Date timeSpot) {
        int pageSize = getPageSize(preferedPageSize);
        int pageNum = page - 1;
        Slice<Connection> slice;
        if (timeSpot == null) {
            // use 'min(msg.creationDate)' to keep a constant connection order over requests
            slice = connectionRepository.getConnectionsByActivityDate(
                            new PageRequest(pageNum, pageSize, Sort.Direction.DESC, "min(msg.creationDate)"));
        } else {
            // use 'min(msg.creationDate)' to keep a constant connection order over requests
            slice = connectionRepository.getConnectionsByActivityDate(timeSpot,
                            new PageRequest(pageNum, pageSize, Sort.Direction.DESC, "min(msg.creationDate)"));
        }
        return slice;
    }

    @Override
    public Slice<Connection> listConnectionsBefore(final URI resumeConnURI, final Integer preferredPageSize,
                    final Date timeSpot) {
        Date resume = messageEventRepository.findMaxActivityDateOfParentAtTime(resumeConnURI, timeSpot);
        int pageSize = getPageSize(preferredPageSize);
        Slice<Connection> slice;
        // use 'min(msg.creationDate)' to keep a constant connection order over requests
        slice = connectionRepository.getConnectionsBeforeByActivityDate(resume, timeSpot,
                        new PageRequest(0, pageSize, Sort.Direction.DESC, "min(msg.creationDate)"));
        return slice;
    }

    @Override
    public Slice<Connection> listConnectionsAfter(final URI resumeConnURI, final Integer preferredPageSize,
                    final Date timeSpot) {
        Date resume = messageEventRepository.findMaxActivityDateOfParentAtTime(resumeConnURI, timeSpot);
        int pageSize = getPageSize(preferredPageSize);
        Slice<Connection> slice;
        // use 'min(msg.creationDate)' to keep a constant connection order over requests
        slice = connectionRepository.getConnectionsAfterByActivityDate(resume, timeSpot,
                        new PageRequest(0, pageSize, Sort.Direction.ASC, "min(msg.creationDate)"));
        return slice;
    }

    @Override
    @Deprecated
    public Collection<URI> listConnectionURIs(final URI atomURI) throws NoSuchAtomException {
        return connectionRepository.getAllConnectionURIsForAtomURI(atomURI);
    }

    @Override
    public Collection<Connection> listConnections(final URI atomURI) throws NoSuchAtomException {
        return connectionRepository.findByAtomURI(atomURI);
    }

    @Override
    @Deprecated
    public Slice<URI> listConnectionURIs(final URI atomURI, int page, Integer preferedPageSize,
                    WonMessageType messageType, Date timeSpot) {
        Slice<URI> slice = null;
        int pageSize = getPageSize(preferedPageSize);
        int pageNum = page - 1;
        // use 'min(msg.creationDate)' to keep a constant connection order over requests
        PageRequest pageRequest = new PageRequest(pageNum, pageSize, Sort.Direction.DESC, "min(msg.creationDate)");
        if (messageType == null) {
            if (timeSpot == null) {
                slice = connectionRepository.getConnectionURIByActivityDate(atomURI, pageRequest);
            } else {
                slice = connectionRepository.getConnectionURIByActivityDate(atomURI, timeSpot, pageRequest);
            }
        } else {
            if (timeSpot == null) {
                slice = connectionRepository.getConnectionURIByActivityDate(atomURI, messageType, pageRequest);
            } else {
                slice = connectionRepository.getConnectionURIByActivityDate(atomURI, messageType, timeSpot,
                                pageRequest);
            }
        }
        return slice;
    }

    @Override
    public Slice<Connection> listConnections(final URI atomURI, int page, Integer preferedPageSize,
                    WonMessageType messageType, Date timeSpot) {
        Slice<Connection> slice = null;
        int pageSize = getPageSize(preferedPageSize);
        int pageNum = page - 1;
        // use 'min(msg.creationDate)' to keep a constant connection order over requests
        PageRequest pageRequest = new PageRequest(pageNum, pageSize, Sort.Direction.DESC, "min(msg.creationDate)");
        if (messageType == null) {
            if (timeSpot == null) {
                slice = connectionRepository.getConnectionsByActivityDate(atomURI, pageRequest);
            } else {
                slice = connectionRepository.getConnectionsByActivityDate(atomURI, timeSpot, pageRequest);
            }
        } else {
            if (timeSpot == null) {
                slice = connectionRepository.getConnectionsByActivityDate(atomURI, messageType, pageRequest);
            } else {
                slice = connectionRepository.getConnectionsByActivityDate(atomURI, messageType, timeSpot, pageRequest);
            }
        }
        return slice;
    }

    @Override
    public Slice<Connection> listConnectionsBefore(final URI atomURI, final URI resumeConnURI,
                    final Integer preferredPageSize, WonMessageType messageType, final Date timeSpot) {
        Date resume;
        int pageSize = getPageSize(preferredPageSize);
        Slice<Connection> slice;
        if (messageType == null) {
            resume = messageEventRepository.findMaxActivityDateOfParentAtTime(resumeConnURI, timeSpot);
            // use 'min(msg.creationDate)' to keep a constant connection order over requests
            slice = connectionRepository.getConnectionsBeforeByActivityDate(atomURI, resume, timeSpot,
                            new PageRequest(0, pageSize, Sort.Direction.DESC, "min(msg.creationDate)"));
        } else {
            resume = messageEventRepository.findMaxActivityDateOfParentAtTime(resumeConnURI, messageType, timeSpot);
            // use 'min(msg.creationDate)' to keep a constant connection order over requests
            slice = connectionRepository.getConnectionsBeforeByActivityDate(atomURI, resume, messageType, timeSpot,
                            new PageRequest(0, pageSize, Sort.Direction.DESC, "min(msg.creationDate)"));
        }
        return slice;
    }

    @Override
    public Slice<Connection> listConnectionsAfter(final URI atomURI, final URI resumeConnURI,
                    final Integer preferredPageSize, final WonMessageType messageType, final Date timeSpot) {
        Date resume;
        int pageSize = getPageSize(preferredPageSize);
        Slice<Connection> slice;
        if (messageType == null) {
            resume = messageEventRepository.findMaxActivityDateOfParentAtTime(resumeConnURI, timeSpot);
            // use 'min(msg.creationDate)' to keep a constant connection order over requests
            slice = connectionRepository.getConnectionsAfterByActivityDate(atomURI, resume, timeSpot,
                            new PageRequest(0, pageSize, Sort.Direction.ASC, "min(msg.creationDate)"));
        } else {
            resume = messageEventRepository.findMaxActivityDateOfParentAtTime(resumeConnURI, messageType, timeSpot);
            // use 'min(msg.creationDate)' to keep a constant connection order over requests
            slice = connectionRepository.getConnectionsAfterByActivityDate(atomURI, resume, messageType, timeSpot,
                            new PageRequest(0, pageSize, Sort.Direction.ASC, "min(msg.creationDate)"));
        }
        return slice;
    }

    @Override
    public Atom readAtom(final URI atomURI) throws NoSuchAtomException {
        if (atomURI == null)
            throw new IllegalArgumentException("atomURI is not set");
        return (DataAccessUtils.loadAtom(atomRepository, atomURI));
    }

    @Override
    public DataWithEtag<Atom> readAtom(final URI atomURI, String etag) throws NoSuchAtomException {
        if (atomURI == null)
            throw new IllegalArgumentException("atomURI is not set");
        Atom atom = null;
        if (etag == null) {
            atom = DataAccessUtils.loadAtom(atomRepository, atomURI);
        } else {
            Integer version = Integer.valueOf(etag);
            atom = atomRepository.findOneByAtomURIAndVersionNot(atomURI, version);
        }
        boolean isDeleted = atom != null && (atom.getState() == AtomState.DELETED);
        return new DataWithEtag<>(atom, atom == null ? etag : Integer.toString(atom.getVersion()), etag, isDeleted);
    }

    @Override
    public Model readAtomContent(final URI atomURI) throws NoSuchAtomException {
        if (atomURI == null)
            throw new IllegalArgumentException("atomURI is not set");
        Atom atom = DataAccessUtils.loadAtom(atomRepository, atomURI);
        return (atom == null || atom.getState() == AtomState.DELETED) ? ModelFactory.createDefaultModel()
                        : atom.getDatatsetHolder().getDataset().getDefaultModel();
    }

    @Override
    public Connection readConnection(final URI connectionURI) throws NoSuchConnectionException {
        if (connectionURI == null)
            throw new IllegalArgumentException("connectionURI is not set");
        return DataAccessUtils.loadConnection(connectionRepository, connectionURI);
    }

    @Override
    public DataWithEtag<Connection> readConnection(final URI connectionURI, String etag) {
        if (connectionURI == null)
            throw new IllegalArgumentException("connectionURI is not set");
        Connection con = null;
        if (etag == null) {
            con = connectionRepository.findOneByConnectionURI(connectionURI)
                            .orElseThrow(() -> new NoSuchConnectionException(connectionURI));
        } else {
            Integer version = Integer.valueOf(etag);
            con = connectionRepository.findOneByConnectionURIAndVersionNot(connectionURI, version);
        }
        return new DataWithEtag<>(con, con == null ? etag : Integer.toString(con.getVersion()), etag);
    }

    // TODO implement RDF handling!
    @Override
    public Model readConnectionContent(final URI connectionURI) throws NoSuchConnectionException {
        return null;
    }

    @Override
    public Slice<MessageEvent> listConnectionEvents(URI connectionUri, int page, Integer preferedPageSize,
                    WonMessageType messageType) {
        int pageSize = getPageSize(preferedPageSize);
        int pageNum = page - 1;
        Slice<MessageEvent> slice = null;
        if (messageType == null) {
            slice = messageEventRepository.findByParentURI(connectionUri,
                            new PageRequest(pageNum, pageSize, Sort.Direction.DESC, "creationDate"));
        } else {
            slice = messageEventRepository.findByParentURIAndType(connectionUri, messageType,
                            new PageRequest(pageNum, pageSize, Sort.Direction.DESC, "creationDate"));
        }
        return slice;
    }

    @Override
    public Slice<MessageEvent> listConnectionEventsAfter(URI connectionUri, URI msgURI,
                    Integer preferredPageSize, WonMessageType msgType) {
        MessageEvent referenceMsg = messageEventRepository.findOneByMessageURI(msgURI)
                        .orElseThrow(() -> new NoSuchMessageException(msgURI));
        Date referenceDate = referenceMsg.getCreationDate();
        int pageSize = getPageSize(preferredPageSize);
        Slice<MessageEvent> slice = null;
        if (msgType == null) {
            slice = messageEventRepository.findByParentURIAfter(connectionUri, referenceDate,
                            new PageRequest(0, pageSize, Sort.Direction.ASC, "creationDate"));
        } else {
            slice = messageEventRepository.findByParentURIAndTypeAfter(connectionUri, referenceDate, msgType,
                            new PageRequest(0, pageSize, Sort.Direction.ASC, "creationDate"));
        }
        return slice;
    }

    @Override
    public Slice<MessageEvent> listConnectionEventsBefore(final URI connectionUri, final URI msgURI,
                    final Integer preferredPageSize, final WonMessageType msgType) {
        int pageSize = getPageSize(preferredPageSize);
        Slice<MessageEvent> slice = null;
        if (msgType == null) {
            slice = messageEventRepository.findByParentURIBeforeFetchDatasetEagerly(connectionUri, msgURI,
                            new PageRequest(0, pageSize, Sort.Direction.DESC, "creationDate"));
        } else {
            slice = messageEventRepository.findByParentURIAndTypeBeforeFetchDatasetEagerly(connectionUri, msgURI,
                            msgType, new PageRequest(0, pageSize, Sort.Direction.DESC, "creationDate"));
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

    public void setAtomRepository(final AtomRepository atomRepository) {
        this.atomRepository = atomRepository;
    }

    public void setConnectionRepository(final ConnectionRepository connectionRepository) {
        this.connectionRepository = connectionRepository;
    }

    private boolean isAtomActive(final Atom atom) {
        return AtomState.ACTIVE == atom.getState();
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }
}

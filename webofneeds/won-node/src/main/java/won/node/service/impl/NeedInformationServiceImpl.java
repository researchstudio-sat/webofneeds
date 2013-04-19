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
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import won.protocol.exception.NoSuchConnectionException;
import won.protocol.exception.NoSuchNeedException;
import won.protocol.model.Connection;
import won.protocol.model.Match;
import won.protocol.model.Need;
import won.protocol.model.NeedState;
import won.protocol.repository.ConnectionRepository;
import won.protocol.repository.MatchRepository;
import won.protocol.repository.NeedRepository;
import won.protocol.service.NeedInformationService;
import won.protocol.util.DataAccessUtils;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * User: fkleedorfer
 * Date: 02.11.12
 */
@Component
public class NeedInformationServiceImpl implements NeedInformationService {

    private RDFStorageService rdfStorage;
    @Autowired
    private NeedRepository needRepository;
    @Autowired
    private ConnectionRepository connectionRepository;
    @Autowired
    private MatchRepository matchRepository;

    private static final int DEFAULT_PAGE_SIZE = 500;

    private int pageSize = DEFAULT_PAGE_SIZE;

    @Override
    public Collection<URI> listNeedURIs() {
        //TODO: provide a repository method for listing just the need URIs
        Iterable<Need> allNeeds = needRepository.findAll();
        List<URI> needURIs = new ArrayList<URI>();
        for (Need need : allNeeds) {
            needURIs.add(need.getNeedURI());
        }
        return needURIs;
    }

    @Override
    public Collection<URI> listNeedURIs(int page) {
        //TODO: provide a repository method for listing just the need URIs
        Iterable<Need> allNeeds = needRepository.findAll(new PageRequest(page, this.pageSize));
        List<URI> needURIs = new ArrayList<URI>();
        for (Need need : allNeeds) {
            needURIs.add(need.getNeedURI());
        }
        return needURIs;
    }

    @Override
    public Collection<URI> listConnectionURIs(final URI needURI) throws NoSuchNeedException {
        if (needURI == null) throw new IllegalArgumentException("needURI is not set");
        Need need = DataAccessUtils.loadNeed(needRepository, needURI);
        //TODO: provide a repository method for listing the connection URIs for a need
        List<Connection> allConnections = connectionRepository.findByNeedURI(need.getNeedURI());
        List<URI> connectionURIs = new ArrayList<URI>(allConnections.size());
        for (Connection connection : allConnections) {
            connectionURIs.add(connection.getConnectionURI());
        }
        return connectionURIs;
    }

    @Override
    public Collection<URI> listConnectionURIs() {
        Iterable<Connection> allConnections = connectionRepository.findAll();
        List<URI> connectionURIs = new ArrayList<URI>();
        for (Connection connection : allConnections) {
            connectionURIs.add(connection.getConnectionURI());
        }
        return connectionURIs;
    }

    @Override
    public Collection<URI> listConnectionURIs(int page) {
        Iterable<Connection> allConnections = connectionRepository.findAll(new PageRequest(page, this.pageSize));
        List<URI> connectionURIs = new ArrayList<URI>();
        for (Connection connection : allConnections) {
            connectionURIs.add(connection.getConnectionURI());
        }
        return connectionURIs;
    }

    @Override
    public Collection<URI> listConnectionURIs(final URI needURI, int page) throws NoSuchNeedException {
        if (needURI == null) throw new IllegalArgumentException("needURI is not set");
        Need need = DataAccessUtils.loadNeed(needRepository, needURI);
        //TODO: provide a repository method for listing the connection URIs for a need
        List<Connection> allConnections = connectionRepository.findByNeedURI(need.getNeedURI(), new PageRequest(page, this.pageSize));
        List<URI> connectionURIs = new ArrayList<URI>(allConnections.size());
        for (Connection connection : allConnections) {
            connectionURIs.add(connection.getConnectionURI());
        }
        return connectionURIs;
    }

    @Override
    public Need readNeed(final URI needURI) throws NoSuchNeedException {
        if (needURI == null) throw new IllegalArgumentException("needURI is not set");
        return (DataAccessUtils.loadNeed(needRepository, needURI));
    }

    @Override
    public Model readNeedContent(final URI needURI) throws NoSuchNeedException {
        if (needURI == null) throw new IllegalArgumentException("needURI is not set");
        return rdfStorage.loadContent(DataAccessUtils.loadNeed(needRepository, needURI));
    }

    @Override
    public Connection readConnection(final URI connectionURI) throws NoSuchConnectionException {
        if (connectionURI == null) throw new IllegalArgumentException("connectionURI is not set");
        return DataAccessUtils.loadConnection(connectionRepository, connectionURI);
    }

    //TODO implement RDF handling!
    @Override
    public Model readConnectionContent(final URI connectionURI) throws NoSuchConnectionException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    //TODO implement paging
    @Override
    public Collection<Match> listMatches(URI needURI, int page) throws NoSuchNeedException {
        if (needURI == null) throw new IllegalArgumentException("needURI is not set");
        Need need = DataAccessUtils.loadNeed(needRepository, needURI);
        return matchRepository.findByFromNeed(need.getNeedURI(),new Sort(Sort.Direction.DESC,"score"));
    }

    @Override
    public Collection<Match> listMatches(URI needURI) throws NoSuchNeedException {
        if (needURI == null) throw new IllegalArgumentException("needURI is not set");
        Need need = DataAccessUtils.loadNeed(needRepository, needURI);
        return matchRepository.findByFromNeed(need.getNeedURI(),new Sort(Sort.Direction.DESC,"score"));
    }

    public void setNeedRepository(final NeedRepository needRepository) {
        this.needRepository = needRepository;
    }

    public void setConnectionRepository(final ConnectionRepository connectionRepository) {
        this.connectionRepository = connectionRepository;
    }

    public void setMatchRepository(final MatchRepository matchRepository)
    {
        this.matchRepository = matchRepository;
    }

    private boolean isNeedActive(final Need need) {
        return NeedState.ACTIVE == need.getState();
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public void setRdfStorage(RDFStorageService rdfStorage) {
        this.rdfStorage = rdfStorage;
    }
}

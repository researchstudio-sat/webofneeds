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

import com.hp.hpl.jena.graph.Graph;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import won.protocol.exception.NoSuchConnectionException;
import won.protocol.exception.NoSuchNeedException;
import won.protocol.model.Connection;
import won.protocol.model.Need;
import won.protocol.model.NeedState;
import won.protocol.repository.ConnectionRepository;
import won.protocol.repository.NeedRepository;
import won.protocol.service.NeedInformationService;

import java.net.URI;
import java.util.ArrayList;
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
  private NeedRepository needRepository;
  @Autowired
  private ConnectionRepository connectionRepository;

  @Override
  public Collection<URI> listNeedURIs()
  {
    //TODO: provide a repository method for listing just the need URIs
    List<Need> allNeeds = needRepository.findAll();
    List<URI> needURIs = new ArrayList<URI>(allNeeds.size());
    for (Need need : allNeeds) {
      needURIs.add(need.getNeedURI());
    }
    return needURIs;
  }


  @Override
  public Collection<URI> listConnectionURIs(final URI needURI) throws NoSuchNeedException
  {
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
  public Need readNeed(final URI needURI) throws NoSuchNeedException
  {
    if (needURI == null) throw new IllegalArgumentException("needURI is not set");
    return (DataAccessUtils.loadNeed(needRepository, needURI));
  }

  //TODO implement RDF handling!
  @Override
  public Graph readNeedContent(final URI needURI) throws NoSuchNeedException
  {
    if (needURI == null) throw new IllegalArgumentException("needURI is not set");
    return null;
  }

  @Override
  public Connection readConnection(final URI connectionURI) throws NoSuchConnectionException
  {
    if (connectionURI == null) throw new IllegalArgumentException("connectionURI is not set");
    return DataAccessUtils.loadConnection(connectionRepository, connectionURI);
  }

  //TODO implement RDF handling!
  @Override
  public Graph readConnectionContent(final URI connectionURI) throws NoSuchConnectionException
  {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
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
}

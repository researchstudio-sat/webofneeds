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

package won.server.service.impl;

import com.hp.hpl.jena.graph.Graph;
import won.protocol.exception.NoSuchConnectionException;
import won.protocol.exception.NoSuchNeedException;
import won.protocol.exception.WonProtocolException;
import won.protocol.model.Connection;
import won.protocol.model.Need;
import won.protocol.model.NeedState;
import won.protocol.owner.OwnerProtocolOwnerService;
import won.protocol.repository.NeedRepository;
import won.protocol.service.ConnectionCommunicationService;
import won.protocol.service.NeedInformationService;

import java.net.URI;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * User: fkleedorfer
 * Date: 02.11.12
 */
public class NeedInformationServiceImpl implements NeedInformationService
{
  private OwnerProtocolOwnerService ownerClient;
  private NeedRepository needRepository;
  private ConnectionCommunicationService connectionCommunicationService;


  @Override
  public Collection<URI> listNeedURIs()
  {
    //TODO: provide a repository method for listing just the need URIs
    List<Need> allNeeds = needRepository.findAll();
    List<URI> needURIs = new ArrayList<URI>(allNeeds.size());
    for (Need need: allNeeds) {
      needURIs.add(need.getNeedURI());
    }
    return needURIs;
  }


  @Override
  public Collection<URI> listConnectionURIs(final URI needURI) throws NoSuchNeedException
  {
    Need need = loadNeed(needURI);
    //TODO: list connections!
    return null;
  }

  @Override
  public Need readNeed(final URI needURI) throws NoSuchNeedException
  {
    return(loadNeed(needURI));
  }

  //TODO implement RDF handling!
  @Override
  public Graph readNeedContent(final URI needURI) throws NoSuchNeedException
  {
    return null;
  }

  @Override
  public Connection readConnection(final URI connectionURI) throws NoSuchNeedException
  {
    return loadConnection(connectionURI);
  }

  //TODO implement RDF handling!
  @Override
  public Graph readConnectionContent(final URI connectionURI) throws NoSuchNeedException
  {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  /**
   * Loads the specified need from the database and raises an exception if it is not found.
   *
   * @param needURI
   * @throws won.protocol.exception.NoSuchNeedException
   * @return the connection
   */
  private Need loadNeed(final URI needURI) throws NoSuchNeedException
  {
    List<Need> needs = needRepository.findByNeedURI(needURI);
    if (needs.size() == 0) throw new NoSuchNeedException(needURI);
    if (needs.size() > 0) throw new WonProtocolException(MessageFormat.format("Inconsistent database state detected: multiple needs found with URI {0}",needURI));
    return needs.get(0);
  }

  private Connection loadConnection(final URI connectionURI) throws NoSuchConnectionException
  {
    //TODO: load connections from repository!
    //List<Connection> connections = connectionRepository.findByNeedURI(connectionURI);
    List<Connection> connections = new ArrayList<Connection>();
    if (connections.size() == 0) throw new NoSuchNeedException(connectionURI);
    if (connections.size() > 0) throw new WonProtocolException(MessageFormat.format("Inconsistent database state detected: multiple needs found with URI {0}",connectionURI));
    return connections.get(0);
  }

  private boolean isNeedActive(final Need need) {
    return NeedState.ACTIVE == need.getState();
  }

}

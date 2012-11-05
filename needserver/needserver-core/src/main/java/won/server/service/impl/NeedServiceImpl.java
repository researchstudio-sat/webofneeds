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
import won.protocol.exception.*;
import won.protocol.model.*;
import won.protocol.owner.NodeToOwnerSender;
import won.protocol.repository.NeedRepository;
import won.server.service.ConnectionService;
import won.server.service.NeedService;

import java.net.URI;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * User: fkleedorfer
 * Date: 02.11.12
 */
public class NeedServiceImpl implements NeedService
{
  private NodeToOwnerSender ownerSender;
  private NeedRepository needRepository;
  private ConnectionService connectionService;

  @Override
  public URI createNeed(final URI ownerURI, final Graph content, final boolean activate) throws IllegalNeedContentException
  {

    Need need = new Need();
    need.setState(activate ? NeedState.ACTIVE : NeedState.INACTIVE);
    need.setOwnerURI(ownerURI);
    need = needRepository.save(need);
    return need.getNeedURI();
  }

  @Override
  public void activate(final URI needURI) throws NoSuchNeedException
  {
    Need need = loadNeed(needURI);
    need.setState(NeedState.ACTIVE);
    need = needRepository.save(need);
  }

  @Override
  public void deactivate(final URI needURI) throws NoSuchNeedException
  {
    Need need = loadNeed(needURI);
    need.setState(NeedState.INACTIVE);
    need = needRepository.save(need);
    //close all connections
    //TODO: add a filter to the method/repo to filter only non-closed connections
    Collection<URI> connectionURIs = listConnectionURIs(need.getNeedURI());
    for (URI connURI : connectionURIs){
      connectionService.close(connURI);
    }
  }

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
  public void hint(final URI needURI, final URI otherNeed, final double score, final URI originator) throws NoSuchNeedException, IllegalMessageForNeedStateException
  {
    Need need = loadNeed(needURI);
    if (! isNeedActive(need)) throw new IllegalMessageForNeedStateException(needURI, NeedMessage.HINT.name(), need.getState());
    ownerSender.sendHintReceived(needURI,otherNeed,score,originator);
  }

  @Override
  public Collection<Match> getMatches(final URI needURI) throws NoSuchNeedException
  {
    Need need = loadNeed(needURI);
    //TODO: list matches!
    return null;
  }

  @Override
  public URI connectTo(final URI needURI, final URI otherNeedURI, final String message) throws NoSuchNeedException, IllegalMessageForNeedStateException, ConnectionAlreadyExistsException
  {
    //Load need (throws exception if not found)
    Need need = loadNeed(needURI);
    if (! isNeedActive(need)) throw new IllegalMessageForNeedStateException(needURI, NeedMessage.CONNECT_TO.name(), need.getState());
    //Create new connection object
    Connection con = new Connection();
    con.setNeedURI(needURI);
    con.setState(ConnectionState.REQUEST_SENT);
    con.setRemoteNeedURI(otherNeedURI);
    //TODO: save con in database - this will set the connection URI
    //Set connection
    return null;
  }


  @Override
  public void connectionRequested(final URI needURI, final URI otherNeedURI, final URI otherConnectionURI, final String message) throws NoSuchNeedException, IllegalMessageForNeedStateException, ConnectionAlreadyExistsException
  {
    //Load need (throws exception if not found)
    Need need = loadNeed(needURI);
    if (! isNeedActive(need)) throw new IllegalMessageForNeedStateException(needURI, NeedMessage.CONNECTION_REQUESTED.name(), need.getState());
    //Create new connection object on our side
    Connection con = new Connection();
    con.setNeedURI(needURI);
    con.setState(ConnectionState.REQUEST_RECEIVED);
    con.setRemoteNeedURI(otherNeedURI);
    //TODO: save con in database - this will set the connection URI
    //Set connection

  }

  @Override
  public Collection<URI> listConnectionURIs(final URI needURI) throws NoSuchNeedException
  {
    Need need = loadNeed(needURI);
    //TODO: list connections!
    return null;
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

  private boolean isNeedActive(final Need need) {
    return NeedState.ACTIVE == need.getState();
  }

}

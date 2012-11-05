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
import won.protocol.exception.IllegalNeedContentException;
import won.protocol.exception.NoSuchNeedException;
import won.protocol.exception.WonProtocolException;
import won.protocol.model.Match;
import won.protocol.model.Need;
import won.protocol.model.NeedState;
import won.protocol.owner.OwnerProtocolOwnerService;
import won.protocol.repository.NeedRepository;
import won.protocol.service.ConnectionCommunicationService;
import won.protocol.service.NeedInformationService;
import won.protocol.service.NeedManagementService;

import java.net.URI;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;

/**
 * User: fkleedorfer
 * Date: 02.11.12
 */
public class NeedManagementServiceImpl implements NeedManagementService
{
  private OwnerProtocolOwnerService ownerClient;
  private NeedRepository needRepository;
  private ConnectionCommunicationService connectionCommunicationService;
  private NeedInformationService needInformationService;


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
    Collection<URI> connectionURIs = needInformationService.listConnectionURIs(need.getNeedURI());
    for (URI connURI : connectionURIs){
      connectionCommunicationService.close(connURI);
    }
  }

  @Override
  public Collection<Match> getMatches(final URI needURI) throws NoSuchNeedException
  {
    Need need = loadNeed(needURI);
    //TODO: list matches!
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

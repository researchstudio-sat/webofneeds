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

import won.protocol.exception.ConnectionAlreadyExistsException;
import won.protocol.exception.IllegalMessageForNeedStateException;
import won.protocol.exception.NoSuchNeedException;
import won.protocol.exception.WonProtocolException;
import won.protocol.model.*;
import won.protocol.owner.OwnerProtocolOwnerService;
import won.protocol.repository.NeedRepository;
import won.protocol.service.ConnectionCommunicationService;
import won.protocol.service.MatcherFacingNeedCommunicationService;
import won.protocol.service.NeedFacingNeedCommunicationService;
import won.protocol.service.OwnerFacingNeedCommunicationService;

import java.net.URI;
import java.text.MessageFormat;
import java.util.List;

/**
 * User: fkleedorfer
 * Date: 02.11.12
 */
public class NeedCommunicationServiceImpl implements
    OwnerFacingNeedCommunicationService,
    NeedFacingNeedCommunicationService,
    MatcherFacingNeedCommunicationService
{
  private OwnerProtocolOwnerService ownerClient;
  private NeedRepository needRepository;
  private ConnectionCommunicationService connectionCommunicationService;


  @Override
  public void hint(final URI needURI, final URI otherNeed, final double score, final URI originator) throws NoSuchNeedException, IllegalMessageForNeedStateException
  {
    Need need = loadNeed(needURI);
    if (! isNeedActive(need)) throw new IllegalMessageForNeedStateException(needURI, NeedMessage.HINT.name(), need.getState());
    ownerClient.hintReceived(needURI, otherNeed, score, originator);
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

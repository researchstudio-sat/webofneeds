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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import won.protocol.exception.ConnectionAlreadyExistsException;
import won.protocol.exception.IllegalMessageForNeedStateException;
import won.protocol.exception.NoSuchNeedException;
import won.protocol.model.*;
import won.protocol.need.NeedProtocolNeedService;
import won.protocol.owner.OwnerProtocolOwnerService;
import won.protocol.repository.ConnectionRepository;
import won.protocol.repository.NeedRepository;
import won.protocol.service.MatcherFacingNeedCommunicationService;
import won.protocol.service.NeedFacingNeedCommunicationService;
import won.protocol.service.OwnerFacingNeedCommunicationService;

import java.net.URI;

/**
 * User: fkleedorfer
 * Date: 02.11.12
 */
@Component
public class NeedCommunicationServiceImpl implements
    OwnerFacingNeedCommunicationService,
    NeedFacingNeedCommunicationService,
    MatcherFacingNeedCommunicationService
{
  /**
   * Client talking to the owner side via the owner protocol
   */
  private OwnerProtocolOwnerService ownerProtocolOwnerService;
  /**
   * Client talking another need via the need protocol
   */
  private NeedProtocolNeedService needProtocolNeedService;

  private URIService URIService;

  @Autowired
  private NeedRepository needRepository;
  @Autowired
  private ConnectionRepository connectionRepository;

  @Override
  public void hint(final URI needURI, final URI otherNeed, final double score, final URI originator) throws NoSuchNeedException, IllegalMessageForNeedStateException
  {
    //Load need (throws exception if not found)
    Need need = DataAccessUtils.loadNeed(needRepository, needURI);
    if (! isNeedActive(need)) throw new IllegalMessageForNeedStateException(needURI, NeedMessage.HINT.name(), need.getState());
    ownerProtocolOwnerService.hintReceived(needURI, otherNeed, score, originator);
  }

  @Override
  public URI connectTo(final URI needURI, final URI otherNeedURI, final String message) throws NoSuchNeedException, IllegalMessageForNeedStateException, ConnectionAlreadyExistsException
  {
    //Load need (throws exception if not found)
    Need need = DataAccessUtils.loadNeed(needRepository, needURI);
    if (! isNeedActive(need)) throw new IllegalMessageForNeedStateException(needURI, NeedMessage.CONNECT_TO.name(), need.getState());
    //Create new connection object
    Connection con = new Connection();
    con.setNeedURI(needURI);
    con.setState(ConnectionState.REQUEST_SENT);
    con.setRemoteNeedURI(otherNeedURI);
    //save connection (this creates a new id)
    con = connectionRepository.save(con);
    //create and set new uri
    con.setConnectionURI(URIService.createConnectionURI(con));
    con = connectionRepository.save(con);

    //send to need
    URI remoteConnectionURI = needProtocolNeedService.connectionRequested(otherNeedURI, needURI, con.getConnectionURI(), message);
    con.setRemoteConnectionURI(remoteConnectionURI);
    con = connectionRepository.save(con);
    return con.getConnectionURI();
  }


  @Override
  public URI connectionRequested(final URI needURI, final URI otherNeedURI, final URI otherConnectionURI, final String message) throws NoSuchNeedException, IllegalMessageForNeedStateException, ConnectionAlreadyExistsException
  {
    //Load need (throws exception if not found)
    Need need = DataAccessUtils.loadNeed(needRepository,needURI);
    if (! isNeedActive(need)) throw new IllegalMessageForNeedStateException(needURI, NeedMessage.CONNECTION_REQUESTED.name(), need.getState());
    //Create new connection object on our side
    Connection con = new Connection();
    con.setNeedURI(needURI);
    con.setState(ConnectionState.REQUEST_RECEIVED);
    con.setRemoteNeedURI(otherNeedURI);
    con.setRemoteConnectionURI(otherConnectionURI);
    //save connection (this creates a new URI)
    con = connectionRepository.save(con);
    //create and set new uri
    con.setConnectionURI(URIService.createConnectionURI(con));
    con = connectionRepository.save(con);

    //TODO: do we save the connection message? where? as a chat message?
    ownerProtocolOwnerService.connectionRequested(needURI, otherNeedURI, con.getConnectionURI(), message);
    //return the URI of the new connection
    return con.getConnectionURI();
  }


  private boolean isNeedActive(final Need need) {
    return NeedState.ACTIVE == need.getState();
  }

  public void setOwnerProtocolOwnerService(final OwnerProtocolOwnerService ownerProtocolOwnerService)
  {
    this.ownerProtocolOwnerService = ownerProtocolOwnerService;
  }

  public void setNeedRepository(final NeedRepository needRepository)
  {
    this.needRepository = needRepository;
  }

  public void setConnectionRepository(final ConnectionRepository connectionRepository)
  {
    this.connectionRepository = connectionRepository;
  }

  public void setNeedProtocolNeedService(final NeedProtocolNeedService needProtocolNeedService)
  {
    this.needProtocolNeedService = needProtocolNeedService;
  }
}

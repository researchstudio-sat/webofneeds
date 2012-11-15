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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import won.protocol.exception.*;
import won.protocol.model.*;
import won.protocol.need.NeedProtocolNeedService;
import won.protocol.owner.OwnerProtocolOwnerService;
import won.protocol.repository.ConnectionRepository;
import won.protocol.repository.MatchRepository;
import won.protocol.repository.NeedRepository;
import won.protocol.service.ConnectionCommunicationService;
import won.protocol.service.MatcherFacingNeedCommunicationService;
import won.protocol.service.NeedFacingNeedCommunicationService;
import won.protocol.service.OwnerFacingNeedCommunicationService;

import java.net.URI;
import java.util.List;
import java.util.concurrent.ExecutorService;


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
  final Logger logger = LoggerFactory.getLogger(NeedCommunicationServiceImpl.class);

  /**
   * Client talking to the owner side via the owner protocol
   */
  private OwnerProtocolOwnerService ownerProtocolOwnerService;
  /**
   * Client talking another need via the need protocol
   */
  private NeedProtocolNeedService needProtocolNeedService;

  /**
   * Client talking to this need service from the need side
   */
  private ConnectionCommunicationService needFacingConnectionCommunicationService;

  /**
   * Client talking to this need service from the owner side
   */
  private ConnectionCommunicationService ownerFacingConnectionCommunicationService;

  private URIService URIService;

  private ExecutorService executorService;

  @Autowired
  private NeedRepository needRepository;

  @Autowired
  private ConnectionRepository connectionRepository;

  @Autowired
  private MatchRepository matchRepository;

  @Override
  public void hint(final URI needURI, final URI otherNeedURI, final double score, final URI originator) throws NoSuchNeedException, IllegalMessageForNeedStateException
  {
    logger.info("HINT received for need {} referring to need {} with score {} from originator {}", new Object[]{needURI, otherNeedURI,score,originator});
    if (needURI == null) throw new IllegalArgumentException("needURI is not set");
    if (otherNeedURI == null) throw new IllegalArgumentException("otherNeedURI is not set");
    if (score < 0 || score > 1) throw new IllegalArgumentException("score is not in [0,1]");
    if (originator == null) throw new IllegalArgumentException("originator is not set");
    if (needURI.equals(otherNeedURI)) throw new IllegalArgumentException("needURI and otherNeedURI are the same");


    //Load need (throws exception if not found)
    Need need = DataAccessUtils.loadNeed(needRepository, needURI);
    if (! isNeedActive(need)) throw new IllegalMessageForNeedStateException(needURI, NeedMessage.HINT.name(), need.getState());

    //save match
    Match match = new Match();
    match.setFromNeed(needURI);
    match.setToNeed(otherNeedURI);
    match.setScore(score);
    match.setOriginator(originator);
    matchRepository.saveAndFlush(match);

    executorService.execute(new Runnable()
    {
      @Override
      public void run()
      {
        //TODO: somewhere, we'll have to use the need's owner URI to determine where to send the request to..
        //should we access the database again in the implementation of the owner protocol owner client?
        //here, we don't really need to handle exceptions, as we don't want to flood matching services with error messages

        try {
          ownerProtocolOwnerService.hintReceived(needURI, otherNeedURI, score, originator);
        } catch (NoSuchNeedException e) {
          logger.debug("caught NoSuchNeedException:", e);
        }

      }
    });
  }

  @Override
  public URI connectTo(final URI needURI, final URI otherNeedURI, final String message) throws NoSuchNeedException, IllegalMessageForNeedStateException, ConnectionAlreadyExistsException
  {
    logger.info("CONNECT_TO received for need {} referring to need {} with message {}",new Object[]{needURI,otherNeedURI,message});
    if (needURI == null) throw new IllegalArgumentException("needURI is not set");
    if (otherNeedURI == null) throw new IllegalArgumentException("otherNeedURI is not set");
    if (needURI.equals(otherNeedURI)) throw new IllegalArgumentException("needURI and otherNeedURI are the same");

    //Load need (throws exception if not found)
    Need need = DataAccessUtils.loadNeed(needRepository, needURI);
    if (! isNeedActive(need)) throw new IllegalMessageForNeedStateException(needURI, NeedMessage.CONNECT_TO.name(), need.getState());

    //check if there already exists a connection between those two
    //we have multiple options:
    //a) no connection exists -> create new
    //b) a connection exists in state ESTABLISHED -> error message
    //c) a connection exists in state REQUEST_SENT. The call must be a
    //   duplicate (or re-sent after the remote end hasn't replied for some time) -> error message
    //d) a connection exists in state REQUEST_RECEIVED. The remote end tried to connect before we did.
    //   -> error message
    //e) a connection exists in state CLOSED -> create new
    List<Connection> existingConnections = connectionRepository.findByNeedURIAndRemoteNeedURI(needURI, otherNeedURI);
    if (existingConnections.size() > 0){
      for(Connection conn: existingConnections){
        if (ConnectionState.ESTABLISHED == conn.getState()
            || ConnectionState.REQUEST_RECEIVED == conn.getState()
            || ConnectionState.REQUEST_SENT == conn.getState()) {
          throw new ConnectionAlreadyExistsException(conn.getConnectionURI(),needURI,otherNeedURI);
        }
      }
    }
    //Create new connection object
    Connection con = new Connection();
    con.setNeedURI(needURI);
    con.setState(ConnectionState.REQUEST_SENT);
    con.setRemoteNeedURI(otherNeedURI);
    //save connection (this creates a new id)
    con = connectionRepository.saveAndFlush(con);
    //create and set new uri
    con.setConnectionURI(URIService.createConnectionURI(con));
    con = connectionRepository.saveAndFlush(con);

    final Connection connectionForRunnable = con;
    //send to need
    executorService.execute(new Runnable()
    {
      @Override
      public void run()
      {
        try {
          URI remoteConnectionURI = needProtocolNeedService.connectionRequested(otherNeedURI, needURI, connectionForRunnable.getConnectionURI(), message);
          connectionForRunnable.setRemoteConnectionURI(remoteConnectionURI);
          connectionRepository.saveAndFlush(connectionForRunnable);
        } catch (WonProtocolException e){
          // we can't open the connection. we send a close back to the owner
          // TODO should we introduce a new protocol method connectionFailed (because it's not an owner deny but some protocol-level error)?
          // For now, we call the close method as if it had been called from the remote side
          // TODO: even with this workaround, it would be good to send a message along with the close (so we can explain what happened).
          try {
            NeedCommunicationServiceImpl.this.needFacingConnectionCommunicationService.close(connectionForRunnable.getConnectionURI());
          } catch (NoSuchConnectionException e1) {
            logger.debug("caught NoSuchConnectionException:", e1);
          } catch (IllegalMessageForConnectionStateException e1) {
            logger.debug("caught IllegalMessageForConnectionStateException:", e1);
          }
        }
      }
    });
    return con.getConnectionURI();
  }


  @Override
  public URI connectionRequested(final URI needURI, final URI otherNeedURI, final URI otherConnectionURI, final String message) throws NoSuchNeedException, IllegalMessageForNeedStateException, ConnectionAlreadyExistsException
  {
    logger.info("CONNECTION_REQUESTED received for need {} referring to need {} (connection {}) with message '{}'", new Object[]{needURI, otherNeedURI, otherConnectionURI, message});
    if (needURI == null) throw new IllegalArgumentException("needURI is not set");
    if (otherNeedURI == null) throw new IllegalArgumentException("otherNeedURI is not set");
    if (otherConnectionURI == null) throw new IllegalArgumentException("otherConnectionURI is not set");
    if (needURI.equals(otherNeedURI)) throw new IllegalArgumentException("needURI and otherNeedURI are the same");

    //Load need (throws exception if not found)
    Need need = DataAccessUtils.loadNeed(needRepository,needURI);
    if (! isNeedActive(need)) throw new IllegalMessageForNeedStateException(needURI, NeedMessage.CONNECTION_REQUESTED.name(), need.getState());
    //Create new connection object on our side

    //check if there already exists a connection between those two
    //we have multiple options:
    //a) no connection exists -> create new
    //b) a connection exists in state ESTABLISHED -> error message
    //c) a connection exists in state REQUEST_SENT. Our request was first, we won't accept a request
    //   from the other side. They have to accept/deny ours! -> error message
    //d) a connection exists in state REQUEST_RECEIVED. The remote side contacts us repeatedly, it seems.
    //   -> error message
    //e) a connection exists in state CLOSED -> create new
    List<Connection> existingConnections = connectionRepository.findByNeedURIAndRemoteNeedURI(needURI, otherNeedURI);
    if (existingConnections.size() > 0){
      for(Connection conn: existingConnections){
        if (ConnectionState.ESTABLISHED == conn.getState()
            || ConnectionState.REQUEST_RECEIVED == conn.getState()
            || ConnectionState.REQUEST_SENT == conn.getState()) {
          throw new ConnectionAlreadyExistsException(conn.getConnectionURI(),needURI,otherNeedURI);
        }
      }
    }


    Connection con = new Connection();
    con.setNeedURI(needURI);
    con.setState(ConnectionState.REQUEST_RECEIVED);
    con.setRemoteNeedURI(otherNeedURI);
    con.setRemoteConnectionURI(otherConnectionURI);
    //save connection (this creates a new URI)
    con = connectionRepository.saveAndFlush(con);
    //create and set new uri
    con.setConnectionURI(URIService.createConnectionURI(con));
    con = connectionRepository.saveAndFlush(con);

    //TODO: do we save the connection message? where? as a chat message?

    final Connection connectionForRunnable = con;
    executorService.execute(new Runnable()
    {
      @Override
      public void run()
      {
        try {
          ownerProtocolOwnerService.connectionRequested(needURI, otherNeedURI, connectionForRunnable.getConnectionURI(), message);
        } catch (WonProtocolException e) {
          // we can't open the connection. we send a deny back to the owner
          // TODO should we introduce a new protocol method connectionFailed (because it's not an owner deny but some protocol-level error)?
          // For now, we call the close method as if it had been called from the owner side
          // TODO: even with this workaround, it would be good to send a message along with the close (so we can explain what happened).
          try {
            NeedCommunicationServiceImpl.this.ownerFacingConnectionCommunicationService.close(connectionForRunnable.getConnectionURI());
          } catch (NoSuchConnectionException e1) {
            logger.debug("caught NoSuchConnectionException:", e1);
          } catch (IllegalMessageForConnectionStateException e1) {
            logger.debug("caught IllegalMessageForConnectionStateException:", e1);
          }
        }
      }
    });

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

  public void setURIService(final URIService URIService)
  {
    this.URIService = URIService;
  }

  public void setExecutorService(final ExecutorService executorService)
  {
    this.executorService = executorService;
  }

  public void setNeedFacingConnectionCommunicationService(final ConnectionCommunicationService needFacingConnectionCommunicationService)
  {
    this.needFacingConnectionCommunicationService = needFacingConnectionCommunicationService;
  }

  public void setOwnerFacingConnectionCommunicationService(final ConnectionCommunicationService ownerFacingConnectionCommunicationService)
  {
    this.ownerFacingConnectionCommunicationService = ownerFacingConnectionCommunicationService;
  }
}

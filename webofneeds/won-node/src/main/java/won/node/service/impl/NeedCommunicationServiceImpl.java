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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import won.protocol.exception.*;
import won.protocol.model.*;
import won.protocol.need.NeedProtocolNeedService;
import won.protocol.owner.OwnerProtocolOwnerService;
import won.protocol.repository.ConnectionRepository;
import won.protocol.repository.EventRepository;
import won.protocol.repository.MatchRepository;
import won.protocol.repository.NeedRepository;
import won.protocol.service.ConnectionCommunicationService;
import won.protocol.service.MatcherFacingNeedCommunicationService;
import won.protocol.service.NeedFacingNeedCommunicationService;
import won.protocol.service.OwnerFacingNeedCommunicationService;
import won.protocol.util.DataAccessUtils;

import javax.persistence.PersistenceException;
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
  @Autowired
  private EventRepository eventRepository;
  @Autowired
  private RDFStorageService rdfStorageService;

  @Override
  public void hint(final URI needURI, final URI otherNeedURI, final double score, final URI originator, final Model content) throws NoSuchNeedException, IllegalMessageForNeedStateException
  {
    logger.info("HINT received for need {} referring to need {} with score {} from originator {} and content {}", new Object[]{needURI, otherNeedURI, score, originator, content});
    if (needURI == null) throw new IllegalArgumentException("needURI is not set");
    if (otherNeedURI == null) throw new IllegalArgumentException("otherNeedURI is not set");
    if (score < 0 || score > 1) throw new IllegalArgumentException("score is not in [0,1]");
    if (originator == null) throw new IllegalArgumentException("originator is not set");
    if (needURI.equals(otherNeedURI)) throw new IllegalArgumentException("needURI and otherNeedURI are the same");


    //Load need (throws exception if not found)
    Need need = DataAccessUtils.loadNeed(needRepository, needURI);
    if (!isNeedActive(need))
      throw new IllegalMessageForNeedStateException(needURI, ConnectionEventType.MATCHER_HINT.name(), need.getState());


    //TODO: Remove Matchrepository
    //save match
    Match match = new Match();
    match.setFromNeed(needURI);
    match.setToNeed(otherNeedURI);
    match.setScore(score);
    match.setOriginator(originator);
    try {
      matchRepository.saveAndFlush(match);
    } catch (PersistenceException e) {
      //TODO: catch a more specific exception (EntityExistsException?)
      logger.warn("error while trying to store match", e);
      return;
    }

    /* Create connection */
    Connection con = new Connection();
    con.setNeedURI(needURI);
    con.setState(ConnectionState.SUGGESTED);
    con.setRemoteNeedURI(otherNeedURI);
    //save connection (this creates a new id)
    con = connectionRepository.saveAndFlush(con);
    //create and set new uri
    con.setConnectionURI(URIService.createConnectionURI(con));
    con = connectionRepository.saveAndFlush(con);

    ConnectionEvent event = new ConnectionEvent();
    event.setConnectionURI(con.getConnectionURI());
    event.setType(ConnectionEventType.MATCHER_HINT);
    event.setOriginatorUri(match.getOriginator());
    eventRepository.saveAndFlush(event);

    rdfStorageService.storeContent(event, content);

    executorService.execute(new Runnable()
    {
      @Override
      public void run()
      {
        //here, we don't really need to handle exceptions, as we don't want to flood matching services with error messages
        try {
          ownerProtocolOwnerService.hint(needURI, otherNeedURI, score, originator, content);
        } catch (NoSuchNeedException e) {
          logger.debug("error sending hint content to owner - no such need:", e);
        } catch (IllegalMessageForNeedStateException e) {
          logger.debug("error sending hint content to owner - illegal need state:", e);
        } catch (Exception e) {
          logger.debug("error sending hint content to owner:", e);
        }
      }
    });
  }

  @Override
  public URI connect(final URI needURI, final URI otherNeedURI, final Model content) throws NoSuchNeedException, IllegalMessageForNeedStateException, ConnectionAlreadyExistsException
  {
    logger.info("CONNECT_TO received for need {} referring to need {} with content {}", new Object[]{needURI, otherNeedURI, content});
    if (needURI == null) throw new IllegalArgumentException("needURI is not set");
    if (otherNeedURI == null) throw new IllegalArgumentException("otherNeedURI is not set");
    if (needURI.equals(otherNeedURI)) throw new IllegalArgumentException("needURI and otherNeedURI are the same");

    //Load need (throws exception if not found)
    Need need = DataAccessUtils.loadNeed(needRepository, needURI);
    Connection con = null;
    if (!isNeedActive(need))
      throw new IllegalMessageForNeedStateException(needURI, ConnectionEventType.OWNER_OPEN.name(), need.getState());

    /**
     * check if there already exists a connection between those two
     * we have multiple options:
     * a) no connection exists -> create new
     * b) a connection exists in state CONNECTED -> error message
     * c) a connection exists in state REQUEST_SENT. The call must be a
     * duplicate (or re-sent after the remote end hasn't replied for some time) -> error message
     * d) a connection exists in state REQUEST_RECEIVED. The remote end tried to connect before we did.
     * -> error message
     * e) a connection exists in state CLOSED -> create new
     */
    List<Connection> existingConnections = connectionRepository.findByNeedURIAndRemoteNeedURI(needURI, otherNeedURI);
    if (existingConnections.size() > 0) {
      for (Connection conn : existingConnections) {
        //TODO: Move this to the transition() - Method in ConnectionState
        if (ConnectionState.CONNECTED == conn.getState() ||
            ConnectionState.REQUEST_SENT == conn.getState()) {
          throw new ConnectionAlreadyExistsException(conn.getConnectionURI(), needURI, otherNeedURI);
        } else {
          conn.setState(conn.getState().transit(ConnectionEventType.OWNER_OPEN));
          con = connectionRepository.saveAndFlush(conn);
        }
      }
    } else {
      //Create new connection object
      con = new Connection();
      con.setNeedURI(needURI);
      con.setState(ConnectionState.REQUEST_SENT);
      con.setRemoteNeedURI(otherNeedURI);
      //save connection (this creates a new id)
      con = connectionRepository.saveAndFlush(con);
      //create and set new uri
      con.setConnectionURI(URIService.createConnectionURI(con));
      con = connectionRepository.saveAndFlush(con);
    }


    if (con == null) {
      //TODO: make this more beautiful
      //This should not happen
      throw new IllegalStateException("con is null");
    } else {

      ConnectionEvent event = new ConnectionEvent();
      event.setConnectionURI(con.getConnectionURI());
      event.setType(ConnectionEventType.OWNER_OPEN);
      event.setOriginatorUri(con.getConnectionURI());
      eventRepository.saveAndFlush(event);

      rdfStorageService.storeContent(event, content);

      final Connection connectionForRunnable = con;
      //send to need
      executorService.execute(new Runnable()
      {
        @Override
        public void run()
        {
          try {
            URI remoteConnectionURI = needProtocolNeedService.connect(otherNeedURI, needURI, connectionForRunnable.getConnectionURI(), content);
            connectionForRunnable.setRemoteConnectionURI(remoteConnectionURI);
            connectionRepository.saveAndFlush(connectionForRunnable);
          } catch (WonProtocolException e) {
            // we can't connect the connection. we send a close back to the owner
            // TODO should we introduce a new protocol method connectionFailed (because it's not an owner deny but some protocol-level error)?
            // For now, we call the close method as if it had been called from the remote side
            // TODO: even with this workaround, it would be good to send a content along with the close (so we can explain what happened).
            try {
              NeedCommunicationServiceImpl.this.needFacingConnectionCommunicationService.close(connectionForRunnable.getConnectionURI(), content);
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
  }

  @Override
  public URI connect(final URI needURI, final URI otherNeedURI, final URI otherConnectionURI, final Model content) throws NoSuchNeedException, IllegalMessageForNeedStateException, ConnectionAlreadyExistsException
  {
    logger.info("CONNECTION_REQUESTED received for need {} referring to need {} (connection {}) with content '{}'", new Object[]{needURI, otherNeedURI, otherConnectionURI, content});
    if (needURI == null) throw new IllegalArgumentException("needURI is not set");
    if (otherNeedURI == null) throw new IllegalArgumentException("otherNeedURI is not set");
    if (otherConnectionURI == null) throw new IllegalArgumentException("otherConnectionURI is not set");
    if (needURI.equals(otherNeedURI)) throw new IllegalArgumentException("needURI and otherNeedURI are the same");

    //Load need (throws exception if not found)
    Need need = DataAccessUtils.loadNeed(needRepository, needURI);
    Connection con = null;
    if (!isNeedActive(need))
      throw new IllegalMessageForNeedStateException(needURI, ConnectionEventType.PARTNER_OPEN.name(), need.getState());
    //Create new connection object on our side

    /**
     * check if there already exists a connection between those two
     * we have multiple options:
     * a) no connection exists -> create new
     * b) a connection exists in state ESTABLISHED -> error content
     * c) a connection exists in state REQUEST_SENT. Our request was first, we won't accept a request
     * from the other side. They have to accept/deny ours! -> error content
     * d) a connection exists in state REQUEST_RECEIVED. The remote side contacts us repeatedly, it seems.
     * -> error content
     * e) a connection exists in state CLOSED -> create new
     */
    List<Connection> existingConnections = connectionRepository.findByNeedURIAndRemoteNeedURI(needURI, otherNeedURI);
    if (existingConnections.size() > 0) {
      for (Connection conn : existingConnections) {
        //TODO: Move this to the transition() - Method in ConnectionState
        if (ConnectionState.CONNECTED == conn.getState() ||
            ConnectionState.REQUEST_RECEIVED == conn.getState()) {
          throw new ConnectionAlreadyExistsException(conn.getConnectionURI(), needURI, otherNeedURI);
        } else {
          conn.setState(conn.getState().transit(ConnectionEventType.PARTNER_OPEN));
          con = connectionRepository.saveAndFlush(conn);
        }
      }
    } else {
      con = new Connection();
      con.setNeedURI(needURI);
      con.setState(ConnectionState.REQUEST_RECEIVED);
      con.setRemoteNeedURI(otherNeedURI);
      con.setRemoteConnectionURI(otherConnectionURI);
      //save connection (this creates a new URI)
      con = connectionRepository.saveAndFlush(con);
      //create and set new uri
      con.setConnectionURI(URIService.createConnectionURI(con));
      con = connectionRepository.saveAndFlush(con);

      //TODO: do we save the connection content? where? as a chat content?
    }

    if (con == null) {
      //TODO: make this more beautiful
      //This should not happen
      throw new IllegalStateException("con is null");
    } else {

      ConnectionEvent event = new ConnectionEvent();
      event.setConnectionURI(con.getConnectionURI());
      event.setType(ConnectionEventType.PARTNER_OPEN);
      event.setOriginatorUri(con.getRemoteConnectionURI());
      eventRepository.saveAndFlush(event);

      rdfStorageService.storeContent(event, content);

      final Connection connectionForRunnable = con;
      executorService.execute(new Runnable()
      {
        @Override
        public void run()
        {
          try {
            ownerProtocolOwnerService.connect(needURI, otherNeedURI, connectionForRunnable.getConnectionURI(), content);
          } catch (WonProtocolException e) {
            // we can't connect the connection. we send a deny back to the owner
            // TODO should we introduce a new protocol method connectionFailed (because it's not an owner deny but some protocol-level error)?
            // For now, we call the close method as if it had been called from the owner side
            // TODO: even with this workaround, it would be good to send a content along with the close (so we can explain what happened).
            try {
              NeedCommunicationServiceImpl.this.ownerFacingConnectionCommunicationService.close(connectionForRunnable.getConnectionURI(), content);
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
  }

  private boolean isNeedActive(final Need need)
  {
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

  public void setRdfStorageService(final RDFStorageService rdfStorageService)
  {
    this.rdfStorageService = rdfStorageService;
  }
}

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
import won.protocol.exception.IllegalMessageForConnectionStateException;
import won.protocol.exception.NoSuchConnectionException;
import won.protocol.exception.WonProtocolException;
import won.protocol.model.*;
import won.protocol.repository.ChatMessageRepository;
import won.protocol.repository.ConnectionRepository;
import won.protocol.repository.EventRepository;
import won.protocol.service.ConnectionCommunicationService;
import won.protocol.util.DataAccessUtils;
import won.node.protocol.impl.MessageProducer;

import javax.jms.JMSException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.concurrent.ExecutorService;


/**
 * User: fkleedorfer
 * Date: 02.11.12
 */
public class OwnerFacingConnectionCommunicationServiceImpl implements ConnectionCommunicationService
{
  private final Logger logger = LoggerFactory.getLogger(getClass());

  private ConnectionCommunicationService needFacingConnectionClient;

  private ExecutorService executorService;


  @Autowired
  private ConnectionRepository connectionRepository;
  @Autowired
  private ChatMessageRepository chatMessageRepository;
  @Autowired
  private EventRepository eventRepository;
  @Autowired
  private RDFStorageService rdfStorageService;
  @Autowired
  private MessageProducer messageProducer;

  @Override
  public void open(final URI connectionURI, final Model content) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
    logger.info("OPEN received from the owner side for connection {0} with content {1}", connectionURI, content);
    if (connectionURI == null) throw new IllegalArgumentException("connectionURI is not set");
    //load connection, checking if it exists
    Connection con = DataAccessUtils.loadConnection(connectionRepository, connectionURI);
    //perform state transit
    ConnectionState nextState = performStateTransit(con, ConnectionEventType.OWNER_OPEN);
    //set new state and save in the db
    con.setState(nextState);
    //save in the db
    final Connection connectionForRunnable = connectionRepository.saveAndFlush(con);

    ConnectionEvent event = new ConnectionEvent();
    event.setConnectionURI(con.getConnectionURI());
    event.setType(ConnectionEventType.OWNER_OPEN);
    event.setOriginatorUri(con.getRemoteConnectionURI());
    eventRepository.saveAndFlush(event);

    rdfStorageService.storeContent(event, content);

    //inform the other side
    if (con.getRemoteConnectionURI() != null) {
      executorService.execute(new Runnable()
      {
        @Override
        public void run()
        {
          try {
            needFacingConnectionClient.open(connectionForRunnable.getRemoteConnectionURI(), content);
          } catch (WonProtocolException e) {
            logger.debug("caught Exception:", e);
          }
        }
      });
    }
  }

  @Override
  public void close(final URI connectionURI, final Model content) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
    logger.info("CLOSE received from the owner side for connection {0} with content {1}", connectionURI, content);
    if (connectionURI == null) throw new IllegalArgumentException("connectionURI is not set");
    //load connection, checking if it exists
    Connection con = DataAccessUtils.loadConnection(connectionRepository, connectionURI);
    //perform state transit
    ConnectionState nextState = performStateTransit(con, ConnectionEventType.OWNER_CLOSE);
    //set new state and save in the db
    con.setState(nextState);
    //save in the db
    final Connection connectionForRunnable = connectionRepository.saveAndFlush(con);

    ConnectionEvent event = new ConnectionEvent();
    event.setConnectionURI(con.getConnectionURI());
    event.setType(ConnectionEventType.OWNER_CLOSE);
    event.setOriginatorUri(con.getRemoteConnectionURI());
    eventRepository.saveAndFlush(event);

    rdfStorageService.storeContent(event, content);

    //inform the other side
    if (con.getRemoteConnectionURI() != null) {
      executorService.execute(new Runnable()
      {
        @Override
        public void run()
        {
          try {
            needFacingConnectionClient.close(connectionForRunnable.getRemoteConnectionURI(), content);
          } catch (WonProtocolException e) {
            logger.warn("caught WonProtocolException:", e);
          }
        }
      });
    }
  }
  /*
  @Override
  public void textMessage(final URI connectionURI, final String message) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
    logger.info("SEND_TEXT_MESSAGE received from the owner side for connection {} with message '{}'", connectionURI, message);
    if (connectionURI == null) throw new IllegalArgumentException("connectionURI is not set");
    if (message == null) throw new IllegalArgumentException("message is not set");
    //load connection, checking if it exists
    Connection con = DataAccessUtils.loadConnection(connectionRepository, connectionURI);
    //perform state transit (should not result in state change)
    //ConnectionState nextState = performStateTransit(con, ConnectionEventType.OWNER_MESSAGE);
    //construct chatMessage object to store in the db
    ChatMessage chatMessage = new ChatMessage();
    chatMessage.setCreationDate(new Date());
    chatMessage.setLocalConnectionURI(con.getConnectionURI());
    chatMessage.setMessage(message);
    chatMessage.setOriginatorURI(con.getNeedURI());
    //save in the db
    chatMessageRepository.saveAndFlush(chatMessage);
    final URI remoteConnectionURI = con.getRemoteConnectionURI();
    //inform the other side
    executorService.execute(new Runnable()
    {
      @Override
      public void run()
      {
        try {
          needFacingConnectionClient.textMessage(remoteConnectionURI, message);
        } catch (WonProtocolException e) {
          logger.warn("caught WonProtocolException:", e);
        }
      }
    });

  }  */

    //for Messaging
    @Override
    public void textMessage(final URI connectionURI, final String message) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
    {
        logger.debug("need-facing: SEND_TEXT_MESSAGE called for connection {} with message {}", connectionURI, message);
        try {
            URI brokerURI = new URI("vm://localhost:61617");

            messageProducer.textMessage(brokerURI, message);

        } catch (URISyntaxException e) {
            logger.warn("Wrong URI syntax", e);  //To change body of catch statement use File | Settings | File Templates.
        } catch (JMSException e) {
            logger.warn("JMS Exception", e);  //To change body of catch statement use File | Settings | File Templates.
        }
        /*
        logger.info("SEND_TEXT_MESSAGE received from the owner side for connection {} with message '{}'", connectionURI, message);
        if (connectionURI == null) throw new IllegalArgumentException("connectionURI is not set");
        if (message == null) throw new IllegalArgumentException("message is not set");
        //load connection, checking if it exists
        Connection con = DataAccessUtils.loadConnection(connectionRepository, connectionURI);
        //perform state transit (should not result in state change)
        //ConnectionState nextState = performStateTransit(con, ConnectionEventType.OWNER_MESSAGE);
        //construct chatMessage object to store in the db
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setCreationDate(new Date());
        chatMessage.setLocalConnectionURI(con.getConnectionURI());
        chatMessage.setMessage(message);
        chatMessage.setOriginatorURI(con.getNeedURI());
        //save in the db
        chatMessageRepository.saveAndFlush(chatMessage);
        final URI remoteConnectionURI = con.getRemoteConnectionURI();
        //inform the other side
        executorService.execute(new Runnable()
        {
            @Override
            public void run()
            {
                try {
                    needFacingConnectionClient.textMessage(remoteConnectionURI, message);
                } catch (WonProtocolException e) {
                    logger.warn("caught WonProtocolException:", e);
                }
            }
        });     */

    }


  public void setNeedFacingConnectionClient(final ConnectionCommunicationService needFacingConnectionClient)
  {
    this.needFacingConnectionClient = needFacingConnectionClient;
  }

  public void setConnectionRepository(final ConnectionRepository connectionRepository)
  {
    this.connectionRepository = connectionRepository;
  }

  public void setChatMessageRepository(final ChatMessageRepository chatMessageRepository)
  {
    this.chatMessageRepository = chatMessageRepository;
  }

  /**
   * Calculates the connectionState resulting from the message in the current connection state.
   * Checks if the specified message is allowed in the connection's state and throws an exception if not.
   *
   * @param con
   * @param msg
   * @return
   * @throws won.protocol.exception.IllegalMessageForConnectionStateException
   *          if the message is not allowed in the connection's current state
   */
  private ConnectionState performStateTransit(Connection con, ConnectionEventType msg) throws IllegalMessageForConnectionStateException
  {
    if (!msg.isMessageAllowed(con.getState())) {
      throw new IllegalMessageForConnectionStateException(con.getConnectionURI(), msg.name(), con.getState());
    }
    return con.getState().transit(msg);
  }

  public void setExecutorService(final ExecutorService executorService)
  {
    this.executorService = executorService;
  }

  public void setRdfStorageService(final RDFStorageService rdfStorageService)
  {
    this.rdfStorageService = rdfStorageService;
  }
}

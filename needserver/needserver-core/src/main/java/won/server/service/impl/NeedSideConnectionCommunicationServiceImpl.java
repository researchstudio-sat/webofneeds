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

import won.protocol.exception.IllegalMessageForConnectionStateException;
import won.protocol.exception.NoSuchConnectionException;
import won.protocol.model.Connection;
import won.protocol.model.ConnectionMessage;
import won.protocol.model.ConnectionState;
import won.protocol.service.ConnectionCommunicationService;

import java.net.URI;

/**
 * User: fkleedorfer
 * Date: 02.11.12
 */
public class NeedSideConnectionCommunicationServiceImpl implements ConnectionCommunicationService
{
  private ConnectionCommunicationService ownerSideConnectionClient;


  @Override
  public void accept(final URI connectionURI) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
    //load connection, checking if it exists and the message is allowed in its state
    Connection con = loadConnectionForMessage(connectionURI);
    //perform state transit (should not result in state change)
    ConnectionState nextState = performStateTransit(con, ConnectionMessage.PARTNER_ACCEPT);
    con.setState(nextState);
    //TODO: save in the db
    ownerSideConnectionClient.accept(connectionURI);
  }

  @Override
  public void deny(final URI connectionURI) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
    //load connection, checking if it exists and the message is allowed in its state
    Connection con = loadConnectionForMessage(connectionURI);
    //perform state transit (should not result in state change)
    ConnectionState nextState = performStateTransit(con, ConnectionMessage.PARTNER_DENY);
    con.setState(nextState);
    //TODO: save in the db
    ownerSideConnectionClient.deny(connectionURI);
  }

   @Override
  public void close(final URI connectionURI) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
    //load connection, checking if it exists and the message is allowed in its state
    Connection con = loadConnectionForMessage(connectionURI);
    //perform state transit (should not result in state change)
    ConnectionState nextState = performStateTransit(con, ConnectionMessage.PARTNER_CLOSE);
    con.setState(nextState);
    //TODO: save in the db
    ownerSideConnectionClient.close(connectionURI);
  }

  @Override
  public void sendTextMessage(final URI connectionURI, final String message) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
    //load connection, checking if it exists and the message is allowed in its state
    Connection con = loadConnectionForMessage(connectionURI);
    //perform state transit (should not result in state change)
    ConnectionState nextState = performStateTransit(con, ConnectionMessage.PARTNER_MESSAGE);
    ownerSideConnectionClient.sendTextMessage(connectionURI, message);
  }

  /**
   * Loads the specified connection from the database and raises an exception if it is not found.
   *
   * @param connectionURI
   * @throws won.protocol.exception.NoSuchConnectionException
   * @return the connection
   */
  private Connection loadConnectionForMessage(final URI connectionURI) throws NoSuchConnectionException
  {
    //TODO: load connection object from db
    Connection con = new Connection();
    if (con == null) throw new NoSuchConnectionException(connectionURI);
    return con;
  }

  /**
   * Calculates the connectionState resulting from the message in the current connection state.
   * Checks if the specified message is allowed in the connection's state and throws an exception if not.
   * @param con
   * @param msg
   * @return
   * @throws won.protocol.exception.IllegalMessageForConnectionStateException if the message is not allowed in the connection's current state
   */
  private ConnectionState performStateTransit(Connection con, ConnectionMessage msg) throws IllegalMessageForConnectionStateException{
    if (!msg.isMessageAllowed(con.getState())){
      throw new IllegalMessageForConnectionStateException(con.getURI(), msg.name(),con.getState());
    }
    return con.getState().transit(msg);
  }


}

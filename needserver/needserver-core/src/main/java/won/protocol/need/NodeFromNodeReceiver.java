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

package won.protocol.need;

import won.protocol.exception.*;

import java.net.URI;

/**
 * User: fkleedorfer
 * Date: 31.10.12
 */
public interface NodeFromNodeReceiver
{
  /**
   * Requests a connection from the need otherNeedURI. The other need refers to the
   * connection using the specified otherConnectionURI. A short message can be sent along with the
   * request.
   *
   *
   * @param need the URI of the need
   * @param otherNeedURI
   * @param otherConnectionURI
   * @param message
   *
   */
  public void connectionRequested(URI need, URI otherNeedURI, URI otherConnectionURI, String message) throws NoSuchNeedException, IllegalMessageForNeedStateException, ConnectionAlreadyExistsException;

  /**
   * NeedService-facing (i.e. connection-facing) method; Informs the connection object of the fact that the connection
   * has been accepted by the other side.
   * @param connectionURI the URI of the connection
   * @throws NoSuchConnectionException if ownConnectionURI does not refer to an existing connection
   * @throws IllegalMessageForConnectionStateException if the message is not allowed in the current state of the connection
   */
  public void connectionAccepted(URI connectionURI) throws NoSuchConnectionException, IllegalMessageForConnectionStateException;

  /**
   * NeedService-facing (i.e. connection-facing) method; Informs the connection object of the fact that the connection
   * has been denied by the other side.
   * @param connectionURI the URI of the connection
   * @throws NoSuchConnectionException if ownConnectionURI does not refer to an existing connection
   * @throws IllegalMessageForConnectionStateException if the message is not allowed in the current state of the connection
   */
  public void connectionDenied(URI connectionURI) throws NoSuchConnectionException, IllegalMessageForConnectionStateException;

  /**
   * NeedService-facing (i.e. connection-facing) method; Informs the connection object of the fact that the connection
   * has been closed by the other side, indicating success.
   * @param connectionURI the URI of the connection
   * @throws NoSuchConnectionException if ownConnectionURI does not refer to an existing connection
   * @throws IllegalMessageForConnectionStateException if the message is not allowed in the current state of the connection
   */
  public void connectionClosed(URI connectionURI) throws NoSuchConnectionException, IllegalMessageForConnectionStateException;

  /**
   * NeedService-facing method; receives a chat message from the remote partner.
   * @param connectionURI the local connection
   * @param message the chat message received from the remote connection
   * @throws NoSuchConnectionException if ownConnectionURI does not refer to an existing connection
   * @throws IllegalMessageForConnectionStateException if the message is not allowed in the current state of the connection
   */
  public void textMessageReceived(URI connectionURI, String message) throws NoSuchConnectionException, IllegalMessageForConnectionStateException;


}

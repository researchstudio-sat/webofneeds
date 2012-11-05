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
public interface NodeToNodeSender
{
  /**
   * Requests a connection from the need otherNeedURI. The other need refers to the
   * connection using the specified otherConnectionURI. A short message can be sent along with the
   * request.
   *
   * @param needURI the URI of the need
   * @param otherNeedURI
   * @param otherConnectionURI
   * @param message
   *
   * @throws NoSuchNeedException if needURI or otherNeedURI does not denote a need
   * @throws IllegalMessageForNeedStateException
   *                             if one of the needs is inactive
   * @throws ConnectionAlreadyExistsException
   *                             if the two needs are already connected
   */
  public void sendConnectionRequested(URI needURI, URI otherNeedURI, URI otherConnectionURI, String message) throws NoSuchNeedException, IllegalMessageForNeedStateException, ConnectionAlreadyExistsException;


  /**
   * OwnerService-facing method; accepts a connection initiated by a connect().
   * @param connectionURI the URI of the connection
   * @throws NoSuchConnectionException if ownConnectionURI does not refer to an existing connection
   * @throws IllegalMessageForConnectionStateException if the message is not allowed in the current state of the connection
   */
  public void sendConnectionAccepted(URI connectionURI) throws NoSuchConnectionException, IllegalMessageForConnectionStateException;

  /**
   * OwnerService-facing method; deny a connection initiated by a connect().
   * @param connectionURI the URI of the connection
   * @throws NoSuchConnectionException if ownConnectionURI does not refer to an existing connection
   * @throws IllegalMessageForConnectionStateException if the message is not allowed in the current state of the connection
   */
  public void sendConnectionDenied(URI connectionURI) throws NoSuchConnectionException, IllegalMessageForConnectionStateException;

  /**
   * OwnerService-facing method; closes the connection identified by the specified URI, indicating success.
   * @param connectionURI the URI of the connection
   * @throws NoSuchConnectionException if ownConnectionURI does not refer to an existing connection
   * @throws IllegalMessageForConnectionStateException if the message is not allowed in the current state of the connection
   */
  public void sendConnectionClosed(URI connectionURI) throws NoSuchConnectionException, IllegalMessageForConnectionStateException;

  /**
   * OwnerService-facing method; sends a chat message via the local connection identified by the specified connectionURI
   * to the remote partner.
   * @param connectionURI the local connection
   * @param message the chat message
   * @throws NoSuchConnectionException if ownConnectionURI does not refer to an existing connection
   * @throws IllegalMessageForConnectionStateException if the message is not allowed in the current state of the connection
   */
  public void sendTextMessageReceived(URI connectionURI, String message) throws NoSuchConnectionException, IllegalMessageForConnectionStateException;

}

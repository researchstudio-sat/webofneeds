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

package won.protocol.owner;

import won.protocol.exception.*;

import java.net.URI;

/**
 * User: fkleedorfer
 * Date: 31.10.12
 */
public interface NodeToOwnerSender
{
  /**
   * Informs the owner of a hint that has been received for the need.
   * @param otherNeed
   * @param score
   * @param originator
   * @throws NoSuchNeedException ownNeedURI does not refer to a known need
   */
  public void sendHintReceived(URI ownNeed, URI otherNeed, double score, URI originator) throws NoSuchNeedException;

  /**
   * Informs the owner of a connection initiated by the need identified by otherNeedURI to the
   * need identified by ownNeedURI. The connection URI ownConnectionURI has been created automatically by the
   * needserver upon being contacted and is passed here to serve as a connection handle.
   *
   * @param ownNeedURI
   * @param otherNeedURI
   * @param ownConnectionURI
   * @throws NoSuchNeedException if ownNeedURI or otherNeedURI does not denote a need
   * @throws IllegalMessageForNeedStateException
   *                             if one of the needs is inactive
   * @throws ConnectionAlreadyExistsException
   *                             if the two needs are already connected
   *
   */
  public void sendConnectionRequested(URI ownNeedURI, URI otherNeedURI, URI ownConnectionURI) throws NoSuchNeedException, ConnectionAlreadyExistsException, IllegalMessageForNeedStateException;

  /**
   * Informs the owner of the fact that their connection request has been accepted by the other side.
   * @param ownConnectionURI
   * @throws NoSuchConnectionException if ownConnectionURI does not refer to an existing connection
   * @throws IllegalMessageForConnectionStateException if the message is not allowed in the current state of the connection
   */
  public void sendConnectionAccepted(URI ownConnectionURI) throws NoSuchConnectionException, IllegalMessageForConnectionStateException;

  /**
   * Informs the owner of the fact that their connection request has been denied by the other side.
   * @param ownConnectionURI
   * @throws NoSuchConnectionException if ownConnectionURI does not refer to an existing connection
   * @throws IllegalMessageForConnectionStateException if the message is not allowed in the current state of the connection
   */
  public void sendConnectionDenied(URI ownConnectionURI) throws NoSuchConnectionException, IllegalMessageForConnectionStateException;

  /**
   * Informs the owner of the fact that the connection has been closed by the other side, indicating success.
   * @param ownConnectionURI
   * @throws NoSuchConnectionException if ownConnectionURI does not refer to an existing connection
   * @throws IllegalMessageForConnectionStateException if the message is not allowed in the current state of the connection
   */
  public void sendConnectionClosed(URI ownConnectionURI) throws NoSuchConnectionException, IllegalMessageForConnectionStateException;

  /**
   * Informs the owner of the specified chat message received via the specified connection.
   * to the remote partner.
   * @param ownConnectionURI the local connection
   * @param message the chat message received via the connection
   * @throws NoSuchConnectionException if ownConnectionURI does not refer to an existing connection
   * @throws IllegalMessageForConnectionStateException if the message is not allowed in the current state of the connection
   */
  public void sendMessageReceived(URI ownConnectionURI, String message) throws NoSuchConnectionException, IllegalMessageForConnectionStateException;
}

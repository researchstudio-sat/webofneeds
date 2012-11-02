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
import won.protocol.model.Match;

import java.net.URI;
import java.util.Collection;

/**
 * User: fkleedorfer
 * Date: 31.10.12
 */
public interface OwnerToNodeSender
{
  /**
   * Creates a new need with the specified content; the new need will be active or inactive as specified.
   * TODO replace String with the type used to hold the need content
   *
   * @param content
   * @param activate if true, the need will be active, otherwise it will be inactive
   * @return the URI of the newly created need
   * @throws IllegalNeedContentException if the content does not conform to the specification TODO: link to specification
   */
  public URI sendCreateNeed(String content, boolean activate) throws IllegalNeedContentException;

  /**
   * Activates the specified need.
   *
   * @param needURI
   * @throws NoSuchNeedException if the URI does not denote a need.
   */
  public void sendActivateNeed(URI needURI) throws NoSuchNeedException;

  /**
   * Deactivates the specified need.
   *
   * @param needURI
   * @throws NoSuchNeedException if the URI does not denote a need.
   */
  public void sendDeactivateNeed(URI needURI) throws NoSuchNeedException;

  /**
   * OwnerService-facing method; causes a connectio to the need identified by otherNeedURI to be requested by this need.
   * A short message (max 140 chars) can be sent along with the request. The other need is contacted by calling
   * its requestConnection method.
   * <p/>
   * The returned URI is the new connection's URI, generated automatically and passed to the
   * remote need. When the connection is accepted by the other side, the remote need will generate
   * a connection URI in its own domain and link it to the URI generated here.
   *
   * @param needURI      the URI of the need
   * @param otherNeedURI the remote need to connect to
   * @param message      (optional) a message for the remote need's owner
   * @return an URI identifying the connection
   * @throws NoSuchNeedException if needURI or otherNeedURI does not denote a need
   * @throws IllegalMessageForNeedStateException
   *                             if one of the needs is inactive
   * @throws ConnectionAlreadyExistsException
   *                             if the two needs are already connected
   */
  public URI sendConnectTo(URI needURI, URI otherNeedURI, String message) throws NoSuchNeedException, IllegalMessageForNeedStateException, ConnectionAlreadyExistsException;

  /**
   * OwnerService-facing method; accepts a connection initiated by a connect().
   *
   * @param connectionURI the URI of the connection
   * @throws NoSuchConnectionException if connectionURI does not refer to a connection
   * @throws IllegalMessageForConnectionStateException
   *                                   if the message is not allowed in the current connection state
   */
  public void sendAccept(URI connectionURI) throws NoSuchConnectionException, IllegalMessageForConnectionStateException;

  /**
   * OwnerService-facing method; deny a connection initiated by a connect().
   *
   * @param connectionURI the URI of the connection
   * @throws NoSuchConnectionException if connectionURI does not refer to a connection
   * @throws IllegalMessageForConnectionStateException
   *                                   if the message is not allowed in the current connection state
   */
  public void sendDeny(URI connectionURI) throws NoSuchConnectionException, IllegalMessageForConnectionStateException;

  /**
   * OwnerService-facing method; Aborts the connection identified by the specified URI, indicating failure.
   *
   * @param connectionURI the URI of the connection
   * @throws NoSuchConnectionException if connectionURI does not refer to a connection
   * @throws IllegalMessageForConnectionStateException
   *                                   if the message is not allowed in the current connection state
   */
  public void sendAbort(URI connectionURI) throws NoSuchConnectionException, IllegalMessageForConnectionStateException;

  /**
   * OwnerService-facing method; closes the connection identified by the specified URI, indicating success.
   *
   * @param connectionURI the URI of the connection
   * @throws NoSuchConnectionException if connectionURI does not refer to a connection
   * @throws IllegalMessageForConnectionStateException
   *                                   if the message is not allowed in the current connection state
   */
  public void sendClose(URI connectionURI) throws NoSuchConnectionException, IllegalMessageForConnectionStateException;

  /**
   * OwnerService-facing method; sends a chat message via the local connection identified by the specified connectionURI
   * to the remote partner.
   *
   * @param connectionURI the local connection
   * @param message       the chat message
   * @throws NoSuchConnectionException if connectionURI does not refer to a connection
   * @throws IllegalMessageForConnectionStateException
   *                                   if the message is not allowed in the current connection state
   */
  public void sendMessage(URI connectionURI, String message) throws NoSuchConnectionException, IllegalMessageForConnectionStateException;

  /**
   * Retrieves a list of all needs on the needserver.
   *
   * @return a collection of all need URIs.
   * @throws NoSuchConnectionException if connectionURI does not refer to a connection
   * @throws IllegalMessageForConnectionStateException
   *                                   if the message is not allowed in the current connection state
   */
  public Collection<URI> sendListNeedURIs();

  /**
   * Returns all matches for the need.
   *
   * @param needURI the URI of the need
   * @return a collection of matches
   * @throws NoSuchNeedException if needURI does not refer to a need
   */
  public Collection<Match> sendGetMatches(URI needURI) throws NoSuchNeedException;


  /**
   * Retrieves all connection URIs (regardless of state) for the specified local need URI.
   *
   * @param needURI the URI of the need
   * @return a collection of connection URIs.
   * @throws NoSuchNeedException if needURI does not refer to a need
   */
  public Collection<URI> sendListConnectionURIs(URI needURI) throws NoSuchNeedException;
}

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

import com.hp.hpl.jena.graph.Graph;
import won.protocol.exception.*;
import won.protocol.model.Match;

import java.net.URI;
import java.util.Collection;

/**
 * User: fkleedorfer
 * Date: 31.10.12
 */
public interface NodeFromOwnerReceiver
{
  /**
   * Creates a new need with the specified content.
   *
   * @param ownerURI the ownerService endpoint URI used to communicate with the owner
   * @param content
   * @param activate if true, the newly created need object will be active, else inactive
   * @return the URI of the newly created need
   * @throws IllegalNeedContentException if the content does not conform to the specification TODO: link to specification
   */
  public URI createNeed(URI ownerURI, Graph content, boolean activate) throws IllegalNeedContentException;

  /**
   * Activates the need object.
   *
   * @param needURI
   * @throws NoSuchNeedException if needURI does not refer to an existing need
   */
  public void activate(URI needURI) throws NoSuchNeedException;

  /**
   * Deactivates the need object, aborting all its established connections.
   *
   * @param needURI
   * @throws NoSuchNeedException if needURI does not refer to an existing need
   */
  public void deactivate(URI needURI) throws NoSuchNeedException;

  /**
   * Causes a connection to the need identified by otherNeedURI to be requested by this need.
   * A short message (max 140 chars) can be sent along with the request. The other need is contacted by calling
   * its requestConnection method.
   * <p/>
   * The returned URI is the new connection's URI, generated automatically and passed to the
   * remote need. When the connection is accepted by the other side, the remote need will generate
   * a connection URI in its own domain and link it to the URI generated here.
   *
   * @param need         the URI of the need
   * @param otherNeedURI the remote need to connect to
   * @param message      (optional) a message for the remote need's owner
   * @return an URI identifying the connection
   * @throws NoSuchNeedException if needURI or otherNeedURI does not denote a need
   * @throws IllegalMessageForNeedStateException
   *                             if one of the needs is inactive
   * @throws ConnectionAlreadyExistsException
   *                             if the two needs are already connected
   */
  public URI connectTo(URI need, URI otherNeedURI, String message) throws NoSuchNeedException, IllegalMessageForNeedStateException, ConnectionAlreadyExistsException;

  /**
   * OwnerService-facing method; accepts a connection initiated by a connect().
   *
   * @param connectionURI the URI of the connection
   * @throws NoSuchConnectionException if connectionURI does not refer to a connection
   * @throws IllegalMessageForConnectionStateException
   *                                   if the message is not allowed in the current connection state
   */
  public void accept(URI connectionURI) throws NoSuchConnectionException, IllegalMessageForConnectionStateException;

  /**
   * OwnerService-facing method; deny a connection initiated by a connect().
   *
   * @param connectionURI the URI of the connection
   * @throws NoSuchConnectionException if connectionURI does not refer to a connection
   * @throws IllegalMessageForConnectionStateException
   *                                   if the message is not allowed in the current connection state
   */
  public void deny(URI connectionURI) throws NoSuchConnectionException, IllegalMessageForConnectionStateException;

  /**
   * OwnerService-facing method; closes the connection identified by the specified URI, indicating success.
   *
   * @param connectionURI the URI of the connection
   * @throws NoSuchConnectionException if connectionURI does not refer to a connection
   * @throws IllegalMessageForConnectionStateException
   *                                   if the message is not allowed in the current connection state
   */
  public void close(URI connectionURI) throws NoSuchConnectionException, IllegalMessageForConnectionStateException;

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
   */
  public Collection<URI> listNeedURIs();

  /**
   * Returns all matches for the need.
   *
   * @param needURI the URI of the need
   * @return a collection of matches
   * @throws NoSuchNeedException if needURI does not refer to an existing need
   */
  public Collection<Match> getMatches(URI needURI) throws NoSuchNeedException;

  /**
   * Retrieves all connection URIs (regardless of state) for the specified local need URI.
   *
   * @param needURI the URI of the need
   * @return a collection of connection URIs.
   * @throws NoSuchNeedException if needURI does not refer to an existing need
   */
  public Collection<URI> listConnectionURIs(URI needURI) throws NoSuchNeedException;

}

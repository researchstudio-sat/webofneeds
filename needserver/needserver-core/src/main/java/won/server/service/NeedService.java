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

package won.server.service;

import won.protocol.exception.ConnectionAlreadyExistsException;
import won.protocol.exception.IllegalMessageForNeedStateException;
import won.protocol.exception.IllegalNeedContentException;
import won.protocol.exception.NoSuchNeedException;
import won.protocol.model.Match;

import java.net.URI;
import java.util.Collection;

/**
 * Interface defining methods for need manipulation.
 */
public interface NeedService
{

  /**
   * Creates a new need with the specified content.
   * TODO replace String with the type used to hold the need content
   *
   * @param content
   * @return the URI of the newly created need
   */
  public URI createNeed(String content) throws IllegalNeedContentException;

  /**
   * Retrieves a list of all needs on the needserver.
   * @return a collection of all need URIs.
   */
  public Collection<URI> listNeedURIs();

  /**
   * Notifies the need of the fact that it attains the specified match score with otherNeed. Originator
   * identifies the entity making the call. Normally, originator is a matching service.
   *
   * @param needURI the URI of the need
   * @param otherNeed URI of the other need (may be on the local needserver)
   * @param score      match score between 0.0 (bad) and 1.0 (good). Implementations treat lower values as 0.0 and higher values as 1.0.
   * @param originator an URI identifying the calling entity
   * @throws NoSuchNeedException if needURI is not a known need URI
   */
  public void hint(URI needURI, URI otherNeed, double score, URI originator) throws NoSuchNeedException;

  /**
   * Returns all matches for the need.
   *
   * @param needURI the URI of the need
   * @throws NoSuchNeedException if needURI is not a known need URI
   * @return a collection of matches
   */
  public Collection<Match> getMatches(URI needURI) throws NoSuchNeedException;

  /**
   * OwnerService-facing method; causes a connectio to the need identified by otherNeedURI to be requested by this need.
   * A short message (max 140 chars) can be sent along with the request. The other need is contacted by calling
   * its requestConnection method.
   *
   * The returned URI is the new connection's URI, generated automatically and passed to the
   * remote need. When the connection is accepted by the other side, the remote need will generate
   * a connection URI in its own domain and link it to the URI generated here.
   *
   * @param needURI the URI of the need
   * @param otherNeedURI the remote need to connect to
   * @param message (optional) a message for the remote need's owner
   * @throws NoSuchNeedException if needURI is not a known need URI
   * @throws IllegalMessageForNeedStateException if the need is not in active state
   * @throws ConnectionAlreadyExistsException if there already is a connection between the specified needs
   * @return an URI identifying the connection
   */
  public URI connectTo(URI needURI, URI otherNeedURI, String message) throws NoSuchNeedException, IllegalMessageForNeedStateException, ConnectionAlreadyExistsException;

  /**
   * NeedService-facing method; requests a connection from the need otherNeedURI. The other need refers to the
   * connection using the specified otherConnectionURI. A short message can be sent along with the
   * request.
   *
   *
   * @param needURI the URI of the need
   * @param otherNeedURI
   * @param otherConnectionURI
   * @param message
   * @throws NoSuchNeedException if needURI is not a known need URI
   * @throws IllegalMessageForNeedStateException if the need is not in active state
   * @throws ConnectionAlreadyExistsException if there already is a connection between the specified needs
   */
  public void connectionRequested(URI needURI, URI otherNeedURI, URI otherConnectionURI, String message) throws NoSuchNeedException, IllegalMessageForNeedStateException, ConnectionAlreadyExistsException;

  /**
   * Retrieves all connection URIs (regardless of state) for the specified local need URI.
   * @param needURI the URI of the need
   * @throws NoSuchNeedException if needURI is not a known need URI
   * @return a collection of connection URIs.
   */
  public Collection<URI> listConnectionURIs(URI needURI) throws NoSuchNeedException;


}

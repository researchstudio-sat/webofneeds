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
   * Creates a new need with the specified content.
   * TODO replace String with the type used to hold the need content
   *
   * @param content
   * @return the URI of the newly created need
   */
  public URI sendCreateNeed(String content);

  /**
   * Replace the content of the need with the specified content.
   * TODO replace String with the type used to hold the need content
   *
   * @param need the URI of the need
   * @param content
   * @return the URI of the newly created need
   */
  public void sendUpdate(URI need, String content);

  /**
   * @param need the URI of the need
   * Delete the need.
   *
   */
  public void sendDelete(URI need);


  /**
   * OwnerService-facing method; causes a connectio to the need identified by otherNeedURI to be requested by this need.
   * A short message (max 140 chars) can be sent along with the request. The other need is contacted by calling
   * its requestConnection method.
   *
   * The returned URI is the new connection's URI, generated automatically and passed to the
   * remote need. When the connection is accepted by the other side, the remote need will generate
   * a connection URI in its own domain and link it to the URI generated here.
   *
   * @param need the URI of the need
   * @param otherNeedURI the remote need to connect to
   * @param message (optional) a message for the remote need's owner
   * @return an URI identifying the connection
   */
  public URI sendConnectTo(URI need, URI otherNeedURI, String message);

  /**
   * OwnerService-facing method; accepts a connection initiated by a connect().
   * @param connectionURI the URI of the connection
   */
  public void sendAccept(URI connectionURI);

  /**
   * OwnerService-facing method; deny a connection initiated by a connect().
   * @param connectionURI the URI of the connection
   */
  public void sendDeny(URI connectionURI);

  /**
   * OwnerService-facing method; Aborts the connection identified by the specified URI, indicating failure.
   * @param connectionURI the URI of the connection
   */
  public void sendAbort(URI connectionURI);

  /**
   * OwnerService-facing method; closes the connection identified by the specified URI, indicating success.
   * @param connectionURI the URI of the connection
   */
  public void sendClose(URI connectionURI);

  /**
   * OwnerService-facing method; sends a chat message via the local connection identified by the specified connectionURI
   * to the remote partner.
   * @param connectionURI the local connection
   * @param message the chat message
   */
  public void sendMessage(URI connectionURI, String message);

  /**
   * Retrieves a list of all needs on the needserver.
   * @return a collection of all need URIs.
   */
  public Collection<URI> sendListNeedURIs();

  /**
   * Returns all matches for the need.
   *
   * @param need the URI of the need
   * @return a collection of matches
   */
  public Collection<Match> sendGetMatches(URI need);


  /**
   * Retrieves all connection URIs (regardless of state) for the specified local need URI.
   * @param need the URI of the need
   * @return a collection of connection URIs.
   */
  public Collection<URI> sendListConnectionURIs(URI need);
}

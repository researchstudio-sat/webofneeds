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

package won.protocol.service;

import won.protocol.exception.ConnectionAlreadyExistsException;
import won.protocol.exception.IllegalMessageForNeedStateException;
import won.protocol.exception.NoSuchNeedException;

import java.net.URI;

/**
 * Interface defining methods for need communication.
 */
public interface OwnerFacingNeedCommunicationService
{

  /**
   * Causes the need identified by needURI to open a connection to the need identified by otherNeedURI.
   * A short message (max 140 chars) can be sent along with the request. The other need is contacted by calling
   * its requestConnection method.
   *
   * The returned URI is the new connection's URI, generated automatically and passed to the
   * remote need. When the connection is accepted by the other side, the remote need will generate
   * a connection URI in its own domain and link it to the URI generated here.
   *
   * The other need object is contacted asynchronously. If the other need object is inactive, the owner of this
   * need object will receive a deny message.
   *
   * @param needURI the URI of the need
   * @param otherNeedURI the remote need to connect to
   * @param message (optional) a message for the remote need's owner
   * @throws won.protocol.exception.NoSuchNeedException if needURI is not a known need URI
   * @throws won.protocol.exception.IllegalMessageForNeedStateException if the need is not in active state
   * @throws won.protocol.exception.ConnectionAlreadyExistsException if there already is a connection between the specified needs
   * @return an URI identifying the connection
   */
  public URI connectTo(URI needURI, URI otherNeedURI, String message) throws NoSuchNeedException, IllegalMessageForNeedStateException, ConnectionAlreadyExistsException;

}

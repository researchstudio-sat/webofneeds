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
public interface NeedFacingNeedCommunicationService
{

  /**
   * Requests a connection to the need identified by needURI. The request is coming from the need otherNeedURI via its
   * connection otherConnectionURI. A short message can be sent along with the request.
   * A new connection will be created and the request will be forwarded to the owner of the need.
   * The URI of the newly created connection is returned.
   *
   * @param needURI the URI of the need
   * @param otherNeedURI
   * @param otherConnectionURI
   * @param message
   * @throws won.protocol.exception.NoSuchNeedException if needURI is not a known need URI
   * @throws won.protocol.exception.IllegalMessageForNeedStateException if the need is not in active state
   * @throws won.protocol.exception.ConnectionAlreadyExistsException if there already is a connection between the specified needs
   * @return the URI of the newly created connection
   */
  public URI connectionRequested(URI needURI, URI otherNeedURI, URI otherConnectionURI, String message) throws NoSuchNeedException, IllegalMessageForNeedStateException, ConnectionAlreadyExistsException;


}

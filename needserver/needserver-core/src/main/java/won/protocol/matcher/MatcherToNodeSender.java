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

package won.protocol.matcher;

import won.protocol.exception.NoSuchNeedException;

import java.net.URI;
import java.util.Collection;

/**
 * User: fkleedorfer
 * Date: 31.10.12
 */
public interface MatcherToNodeSender
{
  /**
   * Sends a hint message to the specified need.
   *
   * @param needURI the need to send the hint to
   * @param otherNeed the need that the match is about
   * @param score the score for the match (between 0 and 1)
   * @param originator the URI of the matching service
   * @throws NoSuchNeedException if needURI does not refer to an existing need object
   */
  public void sendHint(URI needURI, URI otherNeed, double score, URI originator) throws NoSuchNeedException;

  /**
   * Retrieves a list of all needs on the needserver.
   *
   * @return a collection of all need URIs.
   */
  public Collection<URI> sendListNeedURIs();

  /**
   * Retrieves all connection URIs (regardless of state) for the specified local need URI.
   *
   * @param needURI the URI of the need
   * @return a collection of connection URIs.
   * @throws NoSuchNeedException if needURI does not refer to an existing need
   */
  public Collection<URI> sendListConnectionURIs(URI needURI) throws NoSuchNeedException;
}

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

package won.server.protocol;

import java.net.URI;

/**
 * User: fkleedorfer
 * Date: 23.10.12
 */
public interface OwnerService
{

  /**
   * Informs the owner of a hint that has been received for the need.
   * @param otherNeed
   * @param score
   * @param originator
   */
  public void hintReceived(URI ownNeed, URI otherNeed, double score, URI originator);

  /**
   * Informs the owner of a connection initiated by the need identified by otherNeedURI to the
   * need identified by ownNeedURI. The connection URI ownConnectionURI has been created automatically by the
   * needserver upon being contacted and is passed here to serve as a connection handle.
   *
   * @param ownNeedURI
   * @param otherNeedURI
   * @param ownConnectionURI
   */
  public void connectionRequested(URI ownNeedURI, URI otherNeedURI, URI ownConnectionURI);

  /**
   * Informs the owner of the fact that their connection request has been accepted by the other side.
   * @param ownConnectionURI
   */
  public void connectionAccepted(URI ownConnectionURI);

  /**
   * Informs the owner of the fact that their connection request has been denied by the other side.
   * @param ownConnectionURI
   */
  public void connectionDenied(URI ownConnectionURI);

  /**
   * Informs the owner of the fact that the connection has been aborted by the other side, indicating failure.
   * @param ownConnectionURI
   */
  public void connectionAborted(URI ownConnectionURI);

  /**
   * Informs the owner of the fact that the connection has been closed by the other side, indicating success.
   * @param ownConnectionURI
   */
  public void connectionClosed(URI ownConnectionURI);
}

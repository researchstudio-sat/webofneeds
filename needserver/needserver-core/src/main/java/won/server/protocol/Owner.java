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
public interface Owner
{

  /**
   * Informs the owner of a connection initiated by the need identified by otherNeedURI to the
   * need identified by ownNeedURI. The transaction URI ownTransactionURI has been created automatically by the
   * needserver upon being contacted and is passed here to serve as a connection handle.
   *
   * @param ownNeedURI
   * @param otherNeedURI
   * @param ownTransactionURI
   */
  public void connectionRequested(URI ownNeedURI, URI otherNeedURI, URI ownTransactionURI);

  /**
   * Informs the owner of the fact that their connection request has been accepted by the other side.
   * @param ownTransactionURI
   */
  public void connectionAccepted(URI ownTransactionURI);

  /**
   * Informs the owner of the fact that the connection has been aborted by the other side, indicating failure.
   * @param ownTransactionURI
   */
  public void connectionAborted(URI ownTransactionURI);

  /**
   * Informs the owner of the fact that the connection has been closed by the other side, indicating success.
   * @param ownTransactionURI
   */
  public void connectionClosed(URI ownTransactionURI);
}

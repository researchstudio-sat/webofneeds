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

import java.net.URI;

/**
 * User: fkleedorfer
 * Date: 23.10.12
 */
public interface ConnectionService
{
  /**
   * OwnerService-facing method; accepts a connection initiated by a connect().
   * @param connectionURI the URI of the connection
   */
  public void accept(URI connectionURI);

  /**
   * OwnerService-facing method; deny a connection initiated by a connect().
   * @param connectionURI the URI of the connection
   */
  public void deny(URI connectionURI);

  /**
   * OwnerService-facing method; Aborts the connection identified by the specified URI, indicating failure.
   * @param connectionURI the URI of the connection
   */
  public void abort(URI connectionURI);

  /**
   * OwnerService-facing method; closes the connection identified by the specified URI, indicating success.
   * @param connectionURI the URI of the connection
   */
  public void close(URI connectionURI);


  /**
   * OwnerService-facing method; sends a chat message via the local connection identified by the specified connectionURI
   * to the remote partner.
   * @param connectionURI the local connection
   * @param message the chat message
   */
  public void sendMessage(URI connectionURI, String message);

  /**
   * NeedService-facing (i.e. connection-facing) method; Informs the connection object of the fact that the connection
   * has been accepted by the other side.
   * @param connectionURI the URI of the connection
   */
  public void connectionAccepted(URI connectionURI);

  /**
   * NeedService-facing (i.e. connection-facing) method; Informs the connection object of the fact that the connection
   * has been denied by the other side.
   * @param connectionURI the URI of the connection
   */
  public void connectionDenied(URI connectionURI);

  /**
   * NeedService-facing (i.e. connection-facing) method; Informs the connection object of the fact that the connection
   * has been aborted by the other side, indicating failure.
   * @param connectionURI the URI of the connection
   */
  public void connectionAborted(URI connectionURI);

  /**
   * NeedService-facing (i.e. connection-facing) method; Informs the connection object of the fact that the connection
   * has been closed by the other side, indicating success.
   * @param connectionURI the URI of the connection
   */
  public void connectionClosed(URI connectionURI);

  /**
   * NeedService-facing method; receives a chat message from the remote connection.
   * to the remote partner.
   * @param connectionURI the local connection
   * @param message the chat message received from the remote connection
   */
  public void messageReceived(URI connectionURI, String message);


}

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
public interface Transaction
{
  /**
   * Owner-facing method; accepts a connection initiated by a connect().
   */
  public URI acceptConnection();

  /**
   * Owner-facing method; Aborts the transaction identified by the specified URI, indicating failure.
   */
  public void abort();

  /**
   * Owner-facing method; closes the transaction identified by the specified URI, indicating success.
   */
  public void close();

  /**
   * Need-facing (i.e. transaction-facing) method; Informs the transaction object of the fact that the connection
   * has been accepted by the other side.
   */
  public void connectionAccepted();

  /**
   * Need-facing (i.e. transaction-facing) method; Informs the transaction object of the fact that the connection
   * has been aborted by the other side, indicating failure.
   */
  public void connectionAborted();

  /**
   * Need-facing (i.e. transaction-facing) method; Informs the transaction object of the fact that the connection
   * has been closed by the other side, indicating success.
   */
  public void connectionClosed();

  /**
   * Retrieves a representation of the transaction with the specified URI.
   * TODO replace String with the type used to hold the transaction content
   * @return a representation of the transaction
   */
  public String read();

}

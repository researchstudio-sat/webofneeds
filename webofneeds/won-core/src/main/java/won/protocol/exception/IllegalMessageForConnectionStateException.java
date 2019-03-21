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

package won.protocol.exception;

import won.protocol.model.ConnectionState;

import java.net.URI;
import java.text.MessageFormat;

/**
 * User: fkleedorfer Date: 02.11.12
 */
public class IllegalMessageForConnectionStateException extends WonProtocolException {
  private String methodName;
  private ConnectionState connectionState;
  private URI connectionURI;

  public IllegalMessageForConnectionStateException(final URI connectionURI, final String methodName,
      final ConnectionState connectionState) {
    super(MessageFormat.format(
        "It is  not allowed to call method {0} on connection {1}, as it is currently in state {2}.", methodName,
        safeToString(connectionURI), safeToString(connectionState)));
    this.methodName = methodName;
    this.connectionState = connectionState;
    this.connectionURI = connectionURI;
  }

  public URI getConnectionURI() {
    return connectionURI;
  }

  public String getMethodName() {
    return methodName;
  }

  public ConnectionState getConnectionState() {
    return connectionState;
  }

}

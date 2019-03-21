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

import java.net.URI;
import java.text.MessageFormat;

/**
 * User: fkleedorfer
 * Date: 02.11.12
 */
public class ConnectionAlreadyExistsException extends WonProtocolException
{
  private URI connectionURI;
  private URI fromNeedURI;
  private URI toNeedURI;


  public ConnectionAlreadyExistsException(final URI connectionURI, final URI fromNeedURI, final URI toNeedURI)
  {
    super(MessageFormat.format("The connection between needs {0} and {1} that you wanted to connect already exists with this URI: {2}", fromNeedURI.toString(), toNeedURI, connectionURI));
    this.connectionURI = connectionURI;
    this.fromNeedURI = fromNeedURI;
    this.toNeedURI = toNeedURI;
  }

  public URI getFromNeedURI()
  {
    return fromNeedURI;
  }

  public URI getToNeedURI()
  {
    return toNeedURI;
  }

  public URI getConnectionURI()
  {
    return connectionURI;
  }
}

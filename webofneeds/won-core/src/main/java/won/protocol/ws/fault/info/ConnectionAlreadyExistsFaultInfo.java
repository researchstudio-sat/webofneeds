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

package won.protocol.ws.fault.info;

import java.net.URI;

/**
 * User: fkleedorfer
 * Date: 24.05.13
 */
public class ConnectionAlreadyExistsFaultInfo
{
  private String message;
  private URI connectionURI;
  private URI fromNeedURI;
  private URI toNeedURI;

  @Override
  public String toString()
  {
    return "ConnectionAlreadyExistsFaultInfo{" +
        "message='" + message + '\'' +
        ", connectionURI=" + connectionURI +
        ", fromNeedURI=" + fromNeedURI +
        ", toNeedURI=" + toNeedURI +
        '}';
  }

  public URI getFromNeedURI()
  {
    return fromNeedURI;
  }

  public void setFromNeedURI(final URI fromNeedURI)
  {
    this.fromNeedURI = fromNeedURI;
  }

  public URI getToNeedURI()
  {
    return toNeedURI;
  }

  public void setToNeedURI(final URI toNeedURI)
  {
    this.toNeedURI = toNeedURI;
  }

  public URI getConnectionURI()
  {

    return connectionURI;
  }

  public void setConnectionURI(final URI connectionURI)
  {
    this.connectionURI = connectionURI;
  }

  public String getMessage()
  {
    return message;
  }

  public void setMessage(final String message)
  {
    this.message = message;
  }

}

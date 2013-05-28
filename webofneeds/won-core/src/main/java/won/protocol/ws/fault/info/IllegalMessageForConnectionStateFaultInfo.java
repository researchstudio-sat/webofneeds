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

import won.protocol.model.ConnectionState;

import java.net.URI;

/**
 * User: fkleedorfer
 * Date: 24.05.13
 */
public class IllegalMessageForConnectionStateFaultInfo
{
  private String message;
  private String methodName;
  private ConnectionState connectionState;
  private URI connectionURI;

  @Override
  public String toString()
  {
    return "IllegalMessageForConnectionStateFaultInfo{" +
        "message='" + message + '\'' +
        ", methodName='" + methodName + '\'' +
        ", connectionState=" + connectionState +
        ", connectionURI=" + connectionURI +
        '}';
  }

  public String getMethodName()
  {
    return methodName;
  }

  public void setMethodName(final String methodName)
  {
    this.methodName = methodName;
  }

  public ConnectionState getConnectionState()
  {
    return connectionState;
  }

  public void setConnectionState(final ConnectionState connectionState)
  {
    this.connectionState = connectionState;
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

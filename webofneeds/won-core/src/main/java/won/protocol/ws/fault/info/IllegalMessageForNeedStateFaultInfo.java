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

import won.protocol.model.NeedState;

import java.net.URI;

/**
 * User: fkleedorfer
 * Date: 24.05.13
 */
public class IllegalMessageForNeedStateFaultInfo
{
  private String methodName;
  private NeedState needState;
  private URI needURI;
  private String message;

  @Override
  public String toString()
  {
    return "IllegalMessageForNeedStateFaultInfo{" +
        "methodName='" + methodName + '\'' +
        ", needState=" + needState +
        ", needURI=" + needURI +
        ", message='" + message + '\'' +
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

  public NeedState getNeedState()
  {
    return needState;
  }

  public void setNeedState(final NeedState needState)
  {
    this.needState = needState;
  }

  public URI getNeedURI()
  {
    return needURI;
  }

  public void setNeedURI(final URI needURI)
  {
    this.needURI = needURI;
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

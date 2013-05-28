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

package won.protocol.ws.fault;

import won.protocol.exception.IllegalMessageForConnectionStateException;
import won.protocol.ws.fault.info.IllegalMessageForConnectionStateFaultInfo;

import javax.xml.ws.WebFault;

/**
 * User: fkleedorfer
 * Date: 02.11.12
 */
@WebFault(faultBean="IllegalMessageForConnectionStateFaultInfo")
public class IllegalMessageForConnectionStateFault extends WonProtocolFault
{
  private IllegalMessageForConnectionStateFaultInfo faultInfo;

  public IllegalMessageForConnectionStateFault(final String message)
  {
    super(message);
  }

  public IllegalMessageForConnectionStateFault(final String message, final IllegalMessageForConnectionStateFaultInfo faultInfo)
  {
    super(message);
    this.faultInfo = faultInfo;
  }

  public IllegalMessageForConnectionStateFault(final String message, final IllegalMessageForConnectionStateFaultInfo faultInfo, final Throwable cause)
  {
    super(message, cause);
    this.faultInfo = faultInfo;
  }

  public static IllegalMessageForConnectionStateFault fromException(IllegalMessageForConnectionStateException e) {
    IllegalMessageForConnectionStateFaultInfo faultInfo = new IllegalMessageForConnectionStateFaultInfo();
    faultInfo.setMethodName(e.getMethodName());
    faultInfo.setConnectionState(e.getConnectionState());
    faultInfo.setConnectionURI(e.getConnectionURI());
    return new IllegalMessageForConnectionStateFault(e.getMessage(), faultInfo, e.getCause());
  }

  public static IllegalMessageForConnectionStateException toException(IllegalMessageForConnectionStateFault fault) {
    IllegalMessageForConnectionStateFaultInfo faultInfo = fault.getFaultInfo();
    return new IllegalMessageForConnectionStateException(faultInfo.getConnectionURI(), faultInfo.getMethodName(), faultInfo.getConnectionState());
  }

  public IllegalMessageForConnectionStateFaultInfo getFaultInfo()
  {
    return faultInfo;
  }
}

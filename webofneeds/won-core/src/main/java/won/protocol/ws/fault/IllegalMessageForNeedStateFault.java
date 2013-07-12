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

import won.protocol.exception.IllegalMessageForNeedStateException;
import won.protocol.ws.fault.info.IllegalMessageForNeedStateFaultInfo;

import javax.xml.ws.WebFault;

/**
 * User: fkleedorfer
 * Date: 02.11.12
 */
@WebFault(faultBean="IllegalMessageForNeedStateFaultInfo")
public class IllegalMessageForNeedStateFault extends WonProtocolFault
{

  private IllegalMessageForNeedStateFaultInfo faultInfo;

  public IllegalMessageForNeedStateFault(final String message)
  {
    super(message);
  }

  public IllegalMessageForNeedStateFault(final String message, final IllegalMessageForNeedStateFaultInfo faultInfo)
  {
    super(message);
    this.faultInfo = faultInfo;
  }

  public IllegalMessageForNeedStateFault(final String message, final IllegalMessageForNeedStateFaultInfo faultInfo, final Throwable cause)
  {
    super(message, cause);
    this.faultInfo = faultInfo;
  }

  public static IllegalMessageForNeedStateFault fromException(IllegalMessageForNeedStateException e){
    IllegalMessageForNeedStateFaultInfo faultInfo = new IllegalMessageForNeedStateFaultInfo();
    faultInfo.setMethodName(e.getMethodName());
    faultInfo.setNeedState(e.getNeedState());
    faultInfo.setNeedURI(e.getNeedURI());
    return new IllegalMessageForNeedStateFault(e.getMessage(), faultInfo, e.getCause());
  }

  public static IllegalMessageForNeedStateException toException(IllegalMessageForNeedStateFault fault){
    IllegalMessageForNeedStateFaultInfo faultInfo = fault.getFaultInfo();
    return new IllegalMessageForNeedStateException(faultInfo.getNeedURI(), faultInfo.getMethodName(), faultInfo.getNeedState());
  }

  public IllegalMessageForNeedStateFaultInfo getFaultInfo()
  {
    return faultInfo;
  }
}

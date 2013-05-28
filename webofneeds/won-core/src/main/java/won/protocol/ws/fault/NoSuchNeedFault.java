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

import won.protocol.exception.NoSuchNeedException;
import won.protocol.ws.fault.info.NoSuchNeedFaultInfo;

import javax.xml.ws.WebFault;

/**
 * User: fkleedorfer
 * Date: 02.11.12
 */
@WebFault(faultBean = "NoSuchNeedFaultInfo")
public class NoSuchNeedFault extends WonProtocolFault
{
  private NoSuchNeedFaultInfo faultInfo;

  public NoSuchNeedFault(final String message)
  {
    super(message);
  }

  public NoSuchNeedFault(final String message, final NoSuchNeedFaultInfo faultInfo)
  {
    super(message);
    this.faultInfo = faultInfo;
  }

  public NoSuchNeedFault(final String message, final NoSuchNeedFaultInfo faultInfo, final Throwable cause)
  {
    super(message, cause);
    this.faultInfo = faultInfo;
  }

  public static NoSuchNeedFault fromException(NoSuchNeedException e){
    NoSuchNeedFaultInfo faultInfo = new NoSuchNeedFaultInfo();
    faultInfo.setUnknownNeedURI(e.getUnknownNeedURI());
    return new NoSuchNeedFault(e.getMessage(), faultInfo, e.getCause());
  }

  public static NoSuchNeedException toException(NoSuchNeedFault fault){
    return new NoSuchNeedException(fault.getFaultInfo().getUnknownNeedURI());
  }

  public NoSuchNeedFaultInfo getFaultInfo()
  {
    return faultInfo;
  }
}

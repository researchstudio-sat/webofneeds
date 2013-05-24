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

import won.protocol.exception.ConnectionAlreadyExistsException;
import won.protocol.ws.fault.info.ConnectionAlreadyExistsFaultInfo;

import javax.xml.ws.WebFault;

/**
 * User: fkleedorfer
 * Date: 02.11.12
 */
@WebFault(faultBean="ConnectionAlreadyExistsFaultInfo")
public class ConnectionAlreadyExistsFault extends WonProtocolFault
{
  ConnectionAlreadyExistsFaultInfo faultInfo;

  public ConnectionAlreadyExistsFault(final String message)
  {
    super(message);
  }

  public ConnectionAlreadyExistsFault(final String message, ConnectionAlreadyExistsFaultInfo faultInfo, final Throwable cause)
  {
    super(message, cause);
    this.faultInfo = faultInfo;
  }

  public ConnectionAlreadyExistsFault(String message, ConnectionAlreadyExistsFaultInfo faultInfo)
  {
    super(message);
    this.faultInfo = faultInfo;
  }

  public static ConnectionAlreadyExistsFault fromException(ConnectionAlreadyExistsException e){
    ConnectionAlreadyExistsFaultInfo faultInfo = new ConnectionAlreadyExistsFaultInfo();
    faultInfo.setConnectionURI(e.getConnectionURI());
    faultInfo.setFromNeedURI(e.getFromNeedURI());
    faultInfo.setToNeedURI(e.getToNeedURI());
    return new ConnectionAlreadyExistsFault(e.getMessage(),faultInfo,e.getCause());
  }

  public static ConnectionAlreadyExistsException toException(ConnectionAlreadyExistsFault fault) {
    ConnectionAlreadyExistsFaultInfo faultInfo = fault.getFaultInfo();
    return new ConnectionAlreadyExistsException(faultInfo.getConnectionURI(), faultInfo.getFromNeedURI(), faultInfo.getToNeedURI());
  }

  public ConnectionAlreadyExistsFaultInfo getFaultInfo(){
    return faultInfo;
  }
}

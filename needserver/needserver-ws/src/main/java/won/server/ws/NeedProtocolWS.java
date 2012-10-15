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

package won.server.ws;


import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import java.net.URI;

@WebService(name = "NeedProtocolWS", targetNamespace = "http://webofneeds.org/needProtocol")
@SOAPBinding(style = SOAPBinding.Style.RPC)
public interface NeedProtocolWS
{


  @WebMethod
  @WebResult(name = "connectResponse", partName = "connectResponse")
  public boolean connect(
      @WebParam(name = "remoteNeedUri", partName = "remoteNeedUri")
      String remoteNeedUriString,

      @WebParam(name = "remoteNeedProtocolEndpointUri", partName = "remoteNeedProtocolEndpointUri")
      String remoteNeedProtocolEndpointUriString,

      @WebParam(name = "transactionID", partName = "transactionID")
      String transactionIDString);

//  @WebMethod
//  @WebResult(name = "acceptConnectResponse", partName = "acceptConnectResponse")
//  public boolean acceptConnect(
//      @WebParam(name = "transactionID", partName = "transactionID")
//      String transactionIDString);
//
//  @WebMethod
//  @WebResult(name = "refuseConnectResponse", partName = "refuseConnectResponse")
//  public boolean refuseConnect(
//      @WebParam(name = "transactionID", partName = "transactionID")
//      String transactionIDString);
}



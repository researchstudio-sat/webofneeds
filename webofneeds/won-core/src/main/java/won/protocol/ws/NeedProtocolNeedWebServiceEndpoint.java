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

package won.protocol.ws;

import won.protocol.exception.NoSuchNeedException;
import won.protocol.ws.fault.ConnectionAlreadyExistsFault;
import won.protocol.ws.fault.IllegalMessageForConnectionStateFault;
import won.protocol.ws.fault.IllegalMessageForNeedStateFault;
import won.protocol.ws.fault.NoSuchConnectionFault;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import java.net.URI;

/**
 * User: fkleedorfer
 * Date: 29.11.12
 */
@WebService(
    serviceName="needProtocol",
    targetNamespace = "http://www.webofneeds.org/protocol/need/soap/1.0/",
    portName="NeedProtocolNeedWebServiceEndpointPort"
)
@SOAPBinding(style = SOAPBinding.Style.RPC)
public interface NeedProtocolNeedWebServiceEndpoint
{
  @WebMethod
  URI connect(
          @WebParam(name = "needURI") URI needURI,
          @WebParam(name = "otherNeedURI") URI otherNeedURI,
          @WebParam(name = "otherConnectionURI") URI otherConnectionURI,
          @WebParam(name = "message") String content)
        throws NoSuchNeedException, IllegalMessageForNeedStateFault, ConnectionAlreadyExistsFault;

  @WebMethod
  void open(@WebParam(name = "connectionURI") URI connectionURI,
               @WebParam(name = "content") String content) throws NoSuchConnectionFault, IllegalMessageForConnectionStateFault;

  @WebMethod
  void close(@WebParam(name = "connectionURI") URI connectionURI,
          @WebParam(name = "content") String content) throws NoSuchConnectionFault, IllegalMessageForConnectionStateFault;

  @WebMethod
  void sendTextMessage(
      @WebParam(name = "connectionURI") URI connectionURI,
      @WebParam(name = "message") String message) throws NoSuchConnectionFault, IllegalMessageForConnectionStateFault;
}

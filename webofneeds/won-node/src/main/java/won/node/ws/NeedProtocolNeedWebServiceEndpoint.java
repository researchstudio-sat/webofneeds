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

package won.node.ws;

import won.protocol.exception.*;

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
  URI connectionRequested(
      @WebParam(name = "needURI") URI needURI,
      @WebParam(name = "otherNeedURI") URI otherNeedURI,
      @WebParam(name = "otherConnectionURI") URI otherConnectionURI,
      @WebParam(name = "message") String message)
        throws NoSuchNeedException, IllegalMessageForNeedStateException, ConnectionAlreadyExistsException;

  @WebMethod
  void accept(@WebParam(name = "connectionURI") URI connectionURI) throws NoSuchConnectionException, IllegalMessageForConnectionStateException;

  @WebMethod
  void deny(@WebParam(name = "connectionURI") URI connectionURI) throws NoSuchConnectionException, IllegalMessageForConnectionStateException;

  @WebMethod
  void close(@WebParam(name = "connectionURI") URI connectionURI) throws NoSuchConnectionException, IllegalMessageForConnectionStateException;

  @WebMethod
  void sendTextMessage(
      @WebParam(name = "connectionURI") URI connectionURI,
      @WebParam(name = "message") String message) throws NoSuchConnectionException, IllegalMessageForConnectionStateException;
}

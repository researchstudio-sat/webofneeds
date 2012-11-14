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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;
import won.protocol.exception.*;
import won.server.protocol.impl.NeedProtocolNeedServiceImpl;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import java.net.URI;

/**
 * User: fkleedorfer
 * Date: 12.11.12
 */
@WebService(serviceName="NeedProtocol", targetNamespace = "http://www.webofneeds.org/protocol/need/soap/1.0/")
@Service("NeedProtocolNeedWebServiceEndpoint")
@SOAPBinding(style = SOAPBinding.Style.RPC)
public class NeedProtocolNeedWebServiceEndpoint extends SpringBeanAutowiringSupport
{
  @Autowired
  private NeedProtocolNeedServiceImpl needProtocolNeedService;

  @WebMethod
  public URI connectionRequested(
      @WebParam(name="needURI") final URI needURI,
      @WebParam(name="otherNeedURI")final URI otherNeedURI,
      @WebParam(name="otherConnectionURI") final URI otherConnectionURI,
      @WebParam(name="message")final String message)
        throws NoSuchNeedException, IllegalMessageForNeedStateException, ConnectionAlreadyExistsException
  {
    return needProtocolNeedService.connectionRequested(needURI, otherNeedURI, otherConnectionURI, message);
  }

  @WebMethod
  public void accept(@WebParam(name="connectionURI") final URI connectionURI) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
    needProtocolNeedService.accept(connectionURI);
  }

  @WebMethod
  public void deny(@WebParam(name="connectionURI") final URI connectionURI) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
    needProtocolNeedService.deny(connectionURI);
  }

  @WebMethod
  public void close(@WebParam(name="connectionURI") final URI connectionURI) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
    needProtocolNeedService.close(connectionURI);
  }

  @WebMethod
  public void sendTextMessage(
      @WebParam(name="connectionURI") final URI connectionURI,
      @WebParam(name="message") final String message) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
    needProtocolNeedService.sendTextMessage(connectionURI, message);
  }

  @WebMethod(exclude = true)
  public void setNeedProtocolNeedService(final NeedProtocolNeedServiceImpl needProtocolNeedService)
  {
    this.needProtocolNeedService = needProtocolNeedService;
  }
}

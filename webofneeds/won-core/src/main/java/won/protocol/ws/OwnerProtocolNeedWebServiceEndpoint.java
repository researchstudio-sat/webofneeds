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

import com.hp.hpl.jena.rdf.model.Model;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;
import won.protocol.exception.*;
import won.protocol.model.Connection;
import won.protocol.model.Match;
import won.protocol.model.Need;
import won.protocol.owner.OwnerProtocolNeedService;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import java.net.URI;
import java.util.Collection;

/**
 * User: fkleedorfer
 * Date: 13.11.12
 */
@WebService(serviceName = "ownerProtocol", targetNamespace = "http://www.webofneeds.org/protocol/owner/soap/1.0/")
@SOAPBinding(style = SOAPBinding.Style.RPC)
//TODO: shouldn't we extend one of our protocol interfaces here?
public interface OwnerProtocolNeedWebServiceEndpoint
{
  @WebMethod
  public void sendTextMessage(@WebParam(name="connectionURI") final URI connectionURI, @WebParam(name="message") final String message)
          throws NoSuchConnectionException, IllegalMessageForConnectionStateException;

  @WebMethod
  public void close(@WebParam(name="connectionURI") final URI connectionURI)
          throws NoSuchConnectionException, IllegalMessageForConnectionStateException;

  @WebMethod
  public void deny(@WebParam(name="connectionURI") final URI connectionURI)
          throws NoSuchConnectionException, IllegalMessageForConnectionStateException;

  @WebMethod
  public void accept(@WebParam(name="connectionURI") final URI connectionURI)
          throws NoSuchConnectionException, IllegalMessageForConnectionStateException;
  @WebMethod
  public URI connectTo(@WebParam(name="needURI") final URI needURI, @WebParam(name="otherNeedURI") final URI otherNeedURI, @WebParam(name="message") final String message)
          throws NoSuchNeedException, IllegalMessageForNeedStateException, ConnectionAlreadyExistsException;
  @WebMethod
  public void deactivate(@WebParam(name="needURI") final URI needURI) throws NoSuchNeedException;

  @WebMethod
  public void activate(@WebParam(name="needURI") final URI needURI) throws NoSuchNeedException;

  @WebMethod
  public URI createNeed(@WebParam(name="ownerURI")final URI ownerURI, @WebParam(name="content") final String content,
                        @WebParam(name="activate")final boolean activate)
          throws IllegalNeedContentException;
}

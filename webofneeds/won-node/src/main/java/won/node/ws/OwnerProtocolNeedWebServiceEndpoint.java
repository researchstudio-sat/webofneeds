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

import com.hp.hpl.jena.graph.Graph;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;
import won.protocol.exception.*;
import won.protocol.model.Connection;
import won.protocol.model.Match;
import won.protocol.model.Need;
import won.node.protocol.impl.OwnerProtocolNeedServiceImpl;

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
@WebService(serviceName = "OwnerProtocol", targetNamespace = "http://www.webofneeds.org/protocol/owner/soap/1.0/")
@Service("OwnerProtocolNeedWebServiceEndpoint")
@SOAPBinding(style = SOAPBinding.Style.RPC)
public class OwnerProtocolNeedWebServiceEndpoint extends SpringBeanAutowiringSupport
{
  @Autowired
  private OwnerProtocolNeedServiceImpl ownerProtocolNeedService;

  @WebMethod
  public String readConnectionContent(@WebParam(name="connectionURI") final URI connectionURI) throws NoSuchConnectionException
  {
    //TODO: remove this workaround when we have the linked data service running
    Graph ret = ownerProtocolNeedService.readConnectionContent(connectionURI);
    return (ret!= null)? ret.toString():null;
  }

  @WebMethod
  public Connection readConnection(@WebParam(name="connectionURI") final URI connectionURI) throws NoSuchConnectionException
  {
      return ownerProtocolNeedService.readConnection(connectionURI);
  }

  @WebMethod
  public String readNeedContent(@WebParam(name="needURI") final URI needURI) throws NoSuchNeedException
  {
    //TODO: remove this workaround when we have the linked data service running
    Graph ret = ownerProtocolNeedService.readNeedContent(needURI);
    return (ret!= null)? ret.toString():null;
  }

  @WebMethod
  public Need readNeed(@WebParam(name="needURI") final URI needURI) throws NoSuchNeedException
  {
    return ownerProtocolNeedService.readNeed(needURI);
  }

  @WebMethod
  public URI[] listConnectionURIs(@WebParam(name="needURI") final URI needURI) throws NoSuchNeedException
  {
    Collection<URI> coll = ownerProtocolNeedService.listConnectionURIs(needURI);
    if (coll == null) return null;
    return coll.toArray(new URI[coll.size()]);
  }

  @WebMethod
  public Match[] getMatches(@WebParam(name="needURI") final URI needURI) throws NoSuchNeedException
  {
    Collection<Match> coll = ownerProtocolNeedService.getMatches(needURI);
    if (coll == null) return null;
    return coll.toArray(new Match[coll.size()]);
  }

  @WebMethod
  public URI[] listNeedURIs()
  {
    Collection<URI> coll = ownerProtocolNeedService.listNeedURIs();
    if (coll == null) return null;
    return coll.toArray(new URI[coll.size()]);
  }

  @WebMethod
  public void sendTextMessage(@WebParam(name="connectionURI") final URI connectionURI, @WebParam(name="message") final String message) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
    ownerProtocolNeedService.sendTextMessage(connectionURI, message);
  }

  @WebMethod
  public void close(@WebParam(name="connectionURI") final URI connectionURI) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
    ownerProtocolNeedService.close(connectionURI);
  }

  @WebMethod
  public void deny(@WebParam(name="connectionURI") final URI connectionURI) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
    ownerProtocolNeedService.deny(connectionURI);
  }

  @WebMethod
  public void accept(@WebParam(name="connectionURI") final URI connectionURI) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
    ownerProtocolNeedService.accept(connectionURI);
  }

  @WebMethod
  public URI connectTo(@WebParam(name="needURI") final URI needURI, @WebParam(name="otherNeedURI") final URI otherNeedURI, @WebParam(name="message") final String message) throws NoSuchNeedException, IllegalMessageForNeedStateException, ConnectionAlreadyExistsException
  {
    return ownerProtocolNeedService.connectTo(needURI, otherNeedURI, message);
  }

  @WebMethod
  public void deactivate(@WebParam(name="needURI") final URI needURI) throws NoSuchNeedException
  {
    ownerProtocolNeedService.deactivate(needURI);
  }

  @WebMethod
  public void activate(@WebParam(name="needURI") final URI needURI) throws NoSuchNeedException
  {
    ownerProtocolNeedService.activate(needURI);
  }

  @WebMethod
  public URI createNeed(@WebParam(name="ownerURI")final URI ownerURI, @WebParam(name="content") final String content, @WebParam(name="activate")final boolean activate) throws IllegalNeedContentException
  {
    //TODO: when we start processing RDF, create Graph here!
    return ownerProtocolNeedService.createNeed(ownerURI, null, activate);
  }

  @WebMethod(exclude = true)
  public void setOwnerProtocolNeedService(final OwnerProtocolNeedServiceImpl ownerProtocolNeedService)
  {
    this.ownerProtocolNeedService = ownerProtocolNeedService;
  }
}

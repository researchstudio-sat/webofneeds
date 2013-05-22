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

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import org.springframework.beans.factory.annotation.Autowired;
import won.protocol.exception.*;
import won.protocol.need.NeedProtocolNeedService;
import won.protocol.util.LazySpringBeanAutowiringSupport;
import won.protocol.util.RdfUtils;
import won.protocol.ws.NeedProtocolNeedWebServiceEndpoint;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import java.io.StringReader;
import java.net.URI;

/**
 * User: fkleedorfer
 * Date: 12.11.12
 */
@WebService(
    serviceName="needProtocol",
    targetNamespace = "http://www.webofneeds.org/protocol/need/soap/1.0/",
    portName="NeedProtocolNeedWebServiceEndpointPort"
    )
@SOAPBinding(style = SOAPBinding.Style.RPC)
public class NeedProtocolNeedWebServiceEndpointImpl extends LazySpringBeanAutowiringSupport implements NeedProtocolNeedWebServiceEndpoint
{
  @Autowired
  private NeedProtocolNeedService needProtocolNeedService;
  @Autowired
  private RdfUtils rdfUtils;

  @Override
  @WebMethod
  public URI connect(
          @WebParam(name = "needURI") final URI needURI,
          @WebParam(name = "otherNeedURI") final URI otherNeedURI,
          @WebParam(name = "otherConnectionURI") final URI otherConnectionURI,
          @WebParam(name = "content") final String content)
        throws NoSuchNeedException, IllegalMessageForNeedStateException, ConnectionAlreadyExistsException
  {
    wireDependenciesLazily();
    return needProtocolNeedService.connect(needURI, otherNeedURI, otherConnectionURI, rdfUtils.toModel(content));
  }

  @Override
  @WebMethod
  public void open(@WebParam(name = "connectionURI") final URI connectionURI,
                   @WebParam(name = "content") final String content) throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
      wireDependenciesLazily();
      needProtocolNeedService.open(connectionURI, rdfUtils.toModel(content));
  }

  @Override
  @WebMethod
  public void close(@WebParam(name = "connectionURI") final URI connectionURI,
                    @WebParam(name = "content") final String content) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
    wireDependenciesLazily();
    needProtocolNeedService.close(connectionURI, rdfUtils.toModel(content));
  }

  @Override
  @WebMethod
  public void sendTextMessage(
      @WebParam(name = "connectionURI") final URI connectionURI,
      @WebParam(name = "message") final String message) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
    wireDependenciesLazily();
    needProtocolNeedService.sendTextMessage(connectionURI, message);
  }

  @WebMethod(exclude = true)
  public void setNeedProtocolNeedService(final NeedProtocolNeedService needProtocolNeedService)
  {
    this.needProtocolNeedService = needProtocolNeedService;
  }

  @Override
  protected boolean isWired() {
    return this.needProtocolNeedService != null;
  }
}

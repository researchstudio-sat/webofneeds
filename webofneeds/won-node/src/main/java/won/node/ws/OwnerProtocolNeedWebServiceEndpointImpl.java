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
import com.hp.hpl.jena.util.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import won.protocol.exception.*;
import won.protocol.owner.OwnerProtocolNeedService;
import won.protocol.util.LazySpringBeanAutowiringSupport;
import won.protocol.util.RdfUtils;
import won.protocol.ws.OwnerProtocolNeedWebServiceEndpoint;
import won.protocol.ws.fault.*;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import java.net.URI;

/**
 * User: fkleedorfer
 * Date: 13.11.12
 */

@WebService(serviceName = "ownerProtocol", targetNamespace = "http://www.webofneeds.org/protocol/owner/soap/1.0/")
@SOAPBinding(style = SOAPBinding.Style.RPC)
public class OwnerProtocolNeedWebServiceEndpointImpl extends LazySpringBeanAutowiringSupport implements OwnerProtocolNeedWebServiceEndpoint {
    @Autowired
    private OwnerProtocolNeedService ownerProtocolNeedService;

    protected boolean isWired() {
        return ownerProtocolNeedService != null;
    }

    @WebMethod
    public void textMessage(@WebParam(name = "connectionURI") final URI connectionURI, @WebParam(name = "content") final String message) throws NoSuchConnectionFault, IllegalMessageForConnectionStateFault {
        wireDependenciesLazily();
      try {
        Model messageConvert = RdfUtils.toModel(message);
        ownerProtocolNeedService.textMessage(connectionURI, messageConvert);
      } catch (NoSuchConnectionException e) {
        throw NoSuchConnectionFault.fromException(e);
      } catch (IllegalMessageForConnectionStateException e) {
        throw IllegalMessageForConnectionStateFault.fromException(e);
      }
    }

    @WebMethod
    public void open(@WebParam(name = "connectionURI") final URI connectionURI, @WebParam(name = "content") final String content)
      throws NoSuchConnectionFault, IllegalMessageForConnectionStateFault, IllegalMessageForNeedStateException {
        wireDependenciesLazily();
      try {
        ownerProtocolNeedService.open(connectionURI, RdfUtils.readRdfSnippet(content, FileUtils.langTurtle));
      } catch (NoSuchConnectionException e) {
        throw NoSuchConnectionFault.fromException(e);
      } catch (IllegalMessageForConnectionStateException e) {
        throw IllegalMessageForConnectionStateFault.fromException(e);
      }
    }

    @WebMethod
    public void close(@WebParam(name = "connectionURI") final URI connectionURI, @WebParam(name = "content") final String content) throws NoSuchConnectionFault, IllegalMessageForConnectionStateFault {
        wireDependenciesLazily();
      try {
        ownerProtocolNeedService.close(connectionURI, RdfUtils.readRdfSnippet(content, FileUtils.langTurtle));
      } catch (NoSuchConnectionException e) {
        throw NoSuchConnectionFault.fromException(e);
      } catch (IllegalMessageForConnectionStateException e) {
        throw IllegalMessageForConnectionStateFault.fromException(e);
      }
    }

    @WebMethod
    public URI createNeed(@WebParam(name = "ownerURI") final URI ownerURI, @WebParam(name = "content") final String content, @WebParam(name = "activate") final boolean activate) throws IllegalNeedContentFault
    {
        wireDependenciesLazily();
      try {
        return ownerProtocolNeedService.createNeed(ownerURI, RdfUtils.readRdfSnippet(content, FileUtils.langTurtle), activate, null);
      } catch (IllegalNeedContentException e) {
        throw IllegalNeedContentFault.fromException(e);
      }
    }

    @WebMethod
    public URI connect(@WebParam(name = "needURI") final URI needURI, @WebParam(name = "otherNeedURI") final URI otherNeedURI, @WebParam(name = "content") final String content) throws NoSuchNeedFault, IllegalMessageForNeedStateFault, ConnectionAlreadyExistsFault {
        wireDependenciesLazily();
      try {
        return ownerProtocolNeedService.connect(needURI, otherNeedURI, RdfUtils.readRdfSnippet(content, FileUtils.langTurtle));
      } catch (NoSuchNeedException e) {
        throw NoSuchNeedFault.fromException(e);
      } catch (IllegalMessageForNeedStateException e) {
        throw IllegalMessageForNeedStateFault.fromException(e);
      } catch (ConnectionAlreadyExistsException e) {
        throw ConnectionAlreadyExistsFault.fromException(e);
      }
    }

    @WebMethod
    public void deactivate(@WebParam(name = "needURI") final URI needURI) throws NoSuchNeedFault, NoSuchConnectionFault, IllegalMessageForConnectionStateFault {
        wireDependenciesLazily();
      try {
        ownerProtocolNeedService.deactivate(needURI);
      } catch (NoSuchNeedException e) {
        throw NoSuchNeedFault.fromException(e);
      } catch (NoSuchConnectionException e) {
          throw NoSuchConnectionFault.fromException(e);
      } catch (IllegalMessageForConnectionStateException e) {
          throw IllegalMessageForConnectionStateFault.fromException(e);  //To change body of catch statement use File | Settings | File Templates.
      }
    }

    @WebMethod
    public void activate(@WebParam(name = "needURI") final URI needURI) throws NoSuchNeedFault {
        wireDependenciesLazily();
      try {
        ownerProtocolNeedService.activate(needURI);
      } catch (NoSuchNeedException e) {
        throw NoSuchNeedFault.fromException(e);
      }
    }

    @WebMethod(exclude = true)
    public void setOwnerProtocolNeedService(final OwnerProtocolNeedService ownerProtocolNeedService) {
        this.ownerProtocolNeedService = ownerProtocolNeedService;
    }
}

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
import won.protocol.owner.OwnerProtocolNeedService;
import won.protocol.util.LazySpringBeanAutowiringSupport;
import won.protocol.util.RdfUtils;
import won.protocol.ws.OwnerProtocolNeedWebServiceEndpoint;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import java.io.StringReader;
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
    @Autowired
    private RdfUtils rdfUtils;

    protected boolean isWired() {
        return ownerProtocolNeedService != null;
    }

    @WebMethod
    public void sendTextMessage(@WebParam(name = "connectionURI") final URI connectionURI, @WebParam(name = "message") final String message) throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
        wireDependenciesLazily();
        ownerProtocolNeedService.sendTextMessage(connectionURI, message);
    }

    @WebMethod
    public void open(@WebParam(name = "connectionURI") final URI connectionURI, @WebParam(name = "content") final String content) throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
        wireDependenciesLazily();
        ownerProtocolNeedService.open(connectionURI, rdfUtils.toModel(content));
    }

    @WebMethod
    public void close(@WebParam(name = "connectionURI") final URI connectionURI, @WebParam(name = "content") final String content) throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
        wireDependenciesLazily();
        ownerProtocolNeedService.close(connectionURI, rdfUtils.toModel(content));
    }

    @WebMethod
    public URI createNeed(@WebParam(name = "ownerURI") final URI ownerURI, @WebParam(name = "content") final String content, @WebParam(name = "activate") final boolean activate) throws IllegalNeedContentException {
        wireDependenciesLazily();
        return ownerProtocolNeedService.createNeed(ownerURI, rdfUtils.toModel(content), activate);
    }

    @WebMethod
    public URI connect(@WebParam(name = "needURI") final URI needURI, @WebParam(name = "otherNeedURI") final URI otherNeedURI, @WebParam(name = "content") final String content) throws NoSuchNeedException, IllegalMessageForNeedStateException, ConnectionAlreadyExistsException {
        wireDependenciesLazily();
        return ownerProtocolNeedService.connect(needURI, otherNeedURI, rdfUtils.toModel(content));
    }

    @WebMethod
    public void deactivate(@WebParam(name = "needURI") final URI needURI) throws NoSuchNeedException {
        wireDependenciesLazily();
        ownerProtocolNeedService.deactivate(needURI);
    }

    @WebMethod
    public void activate(@WebParam(name = "needURI") final URI needURI) throws NoSuchNeedException {
        wireDependenciesLazily();
        ownerProtocolNeedService.activate(needURI);
    }

    @WebMethod(exclude = true)
    public void setOwnerProtocolNeedService(final OwnerProtocolNeedService ownerProtocolNeedService) {
        this.ownerProtocolNeedService = ownerProtocolNeedService;
    }
}

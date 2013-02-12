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
import com.hp.hpl.jena.rdf.model.Model;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;
import org.springframework.web.context.support.WebApplicationContextUtils;
import won.node.protocol.impl.OwnerProtocolNeedServiceImpl;
import won.protocol.exception.*;
import won.protocol.model.Connection;
import won.protocol.model.Match;
import won.protocol.model.Need;
import won.protocol.owner.OwnerProtocolNeedService;
import won.protocol.util.LazySpringBeanAutowiringSupport;
import won.protocol.ws.OwnerProtocolNeedWebServiceEndpoint;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.servlet.ServletContext;
import javax.xml.ws.WebServiceContext;
import java.net.URI;
import java.util.Collection;

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
    public String readConnectionContent(@WebParam(name = "connectionURI") final URI connectionURI) throws NoSuchConnectionException {
        //TODO: remove this workaround when we have the linked data service running
        wireDependenciesLazily();
        Model ret = ownerProtocolNeedService.readConnectionContent(connectionURI);
        return (ret != null) ? ret.toString() : null;
    }

    @WebMethod
    public Connection readConnection(@WebParam(name = "connectionURI") final URI connectionURI) throws NoSuchConnectionException {
        wireDependenciesLazily();
        return ownerProtocolNeedService.readConnection(connectionURI);
    }

    @WebMethod
    public String readNeedContent(@WebParam(name = "needURI") final URI needURI) throws NoSuchNeedException {
        //TODO: remove this workaround when we have the linked data service running
        wireDependenciesLazily();
        Model ret = ownerProtocolNeedService.readNeedContent(needURI);
        return (ret != null) ? ret.toString() : null;
    }

    @WebMethod
    public Need readNeed(@WebParam(name = "needURI") final URI needURI) throws NoSuchNeedException {
        wireDependenciesLazily();
        return ownerProtocolNeedService.readNeed(needURI);
    }

    @WebMethod
    public URI[] listConnectionURIs(@WebParam(name = "needURI") final URI needURI) throws NoSuchNeedException {
        wireDependenciesLazily();
        Collection<URI> coll = ownerProtocolNeedService.listConnectionURIs(needURI);
        if (coll == null) return null;
        return coll.toArray(new URI[coll.size()]);
    }

    @WebMethod
    public Match[] getMatches(@WebParam(name = "needURI") final URI needURI) throws NoSuchNeedException {
        wireDependenciesLazily();
        Collection<Match> coll = ownerProtocolNeedService.getMatches(needURI);
        if (coll == null) return null;
        return coll.toArray(new Match[coll.size()]);
    }

    @WebMethod
    public URI[] listNeedURIs() {
        wireDependenciesLazily();
        Collection<URI> coll = ownerProtocolNeedService.listNeedURIs();
        if (coll == null) return null;
        return coll.toArray(new URI[coll.size()]);
    }

    @WebMethod
    public void sendTextMessage(@WebParam(name = "connectionURI") final URI connectionURI, @WebParam(name = "message") final String message) throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
        wireDependenciesLazily();
        ownerProtocolNeedService.sendTextMessage(connectionURI, message);
    }

    @WebMethod
    public void close(@WebParam(name = "connectionURI") final URI connectionURI) throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
        wireDependenciesLazily();
        ownerProtocolNeedService.close(connectionURI);
    }

    @WebMethod
    public void deny(@WebParam(name = "connectionURI") final URI connectionURI) throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
        wireDependenciesLazily();
        ownerProtocolNeedService.deny(connectionURI);
    }

    @WebMethod
    public void accept(@WebParam(name = "connectionURI") final URI connectionURI) throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
        wireDependenciesLazily();
        ownerProtocolNeedService.accept(connectionURI);
    }

    @WebMethod
    public URI connectTo(@WebParam(name = "needURI") final URI needURI, @WebParam(name = "otherNeedURI") final URI otherNeedURI, @WebParam(name = "message") final String message) throws NoSuchNeedException, IllegalMessageForNeedStateException, ConnectionAlreadyExistsException {
        wireDependenciesLazily();
        return ownerProtocolNeedService.connectTo(needURI, otherNeedURI, message);
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

    @WebMethod
    public URI createNeed(@WebParam(name = "ownerURI") final URI ownerURI, @WebParam(name = "content") final String content, @WebParam(name = "activate") final boolean activate) throws IllegalNeedContentException {
        wireDependenciesLazily();
        //TODO: when we start processing RDF, create Graph here!

        return ownerProtocolNeedService.createNeed(ownerURI, null, activate);
    }

    @WebMethod(exclude = true)
    public void setOwnerProtocolNeedService(final OwnerProtocolNeedService ownerProtocolNeedService) {
        this.ownerProtocolNeedService = ownerProtocolNeedService;
    }
}

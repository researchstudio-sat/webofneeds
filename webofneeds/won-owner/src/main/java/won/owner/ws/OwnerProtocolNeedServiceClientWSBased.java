/*
 * Copyright 2012  Research Studios Austria Forschungsges.m.b.H.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package won.owner.ws;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.hp.hpl.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import won.protocol.exception.*;
import won.protocol.message.WonMessage;
import won.protocol.model.Connection;
import won.protocol.model.ConnectionEvent;
import won.protocol.model.Need;
import won.protocol.owner.OwnerProtocolNeedServiceClientSide;
import won.protocol.repository.MatchRepository;
import won.protocol.util.ConnectionModelMapper;
import won.protocol.util.NeedModelMapper;
import won.protocol.util.RdfUtils;
import won.protocol.ws.OwnerProtocolNeedWebServiceEndpoint;
import won.protocol.ws.fault.*;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.Collection;
import java.util.List;

/**
 * User: LEIH-NB
 * Date: 17.10.13
 */
public class OwnerProtocolNeedServiceClientWSBased implements OwnerProtocolNeedServiceClientSide {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private OwnerProtocolNeedClientFactory clientFactory;
    private ApplicationContext ownerApplicationContext;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private NeedModelMapper needModelMapper;

    @Autowired
    private ConnectionModelMapper connectionModelMapper;

    //@Override
    public void open(URI connectionURI, Model content)
      throws NoSuchConnectionException, IllegalMessageForConnectionStateException, IllegalMessageForNeedStateException {
        try {
            OwnerProtocolNeedWebServiceEndpoint proxy = clientFactory.getOwnerProtocolEndpointForConnection(connectionURI);
            proxy.open(connectionURI, RdfUtils.toString(content));
        } catch (MalformedURLException e) {
            logger.warn("couldn't create URL for needProtocolEndpoint", e);
        } catch (IllegalMessageForConnectionStateFault illegalMessageForConnectionStateFault) {
            throw IllegalMessageForConnectionStateFault.toException(illegalMessageForConnectionStateFault);
        } catch (NoSuchConnectionFault noSuchConnectionFault) {
            throw NoSuchConnectionFault.toException(noSuchConnectionFault);
        }
    }
    //@Override
    public void close(URI connectionURI, Model content) throws
            NoSuchConnectionException, IllegalMessageForConnectionStateException {
        try {
            OwnerProtocolNeedWebServiceEndpoint proxy = clientFactory.getOwnerProtocolEndpointForConnection(connectionURI);
            proxy.close(connectionURI, RdfUtils.toString(content));
        } catch (NoSuchConnectionFault noSuchConnectionFault) {
            throw NoSuchConnectionFault.toException(noSuchConnectionFault);
        } catch (IllegalMessageForConnectionStateFault illegalMessageForConnectionStateFault) {
            throw IllegalMessageForConnectionStateFault.toException(illegalMessageForConnectionStateFault);
        } catch (MalformedURLException e) {
            logger.warn("couldn't create URL for needProtocolEndpoint", e);
        }
    }

    //@Override
    public void sendMessage(URI connectionURI, Model message) throws
            NoSuchConnectionException, IllegalMessageForConnectionStateException {
        String messageString = RdfUtils.toString(message);
        try {
            OwnerProtocolNeedWebServiceEndpoint proxy = clientFactory.getOwnerProtocolEndpointForConnection(connectionURI);
            proxy.sendMessage(connectionURI, messageString);
        } catch (MalformedURLException e) {
            logger.warn("couldn't create URL for needProtocolEndpoint", e);
        } catch (IllegalMessageForConnectionStateFault illegalMessageForConnectionStateFault) {
            throw IllegalMessageForConnectionStateFault.toException(illegalMessageForConnectionStateFault);
        } catch (NoSuchConnectionFault noSuchConnectionFault) {
            throw NoSuchConnectionFault.toException(noSuchConnectionFault);
        }
    }


  @Override
  public void processMessage(final WonMessage wonMessage) {

  }

  @Override
    public String register(URI endpointURI) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    //@Override
    public ListenableFuture<URI> createNeed(URI ownerURI, Model content, boolean activate) throws IllegalNeedContentException {
        return createNeed(ownerURI, content, activate,null);
    }

    //@Override
    public ListenableFuture<URI> createNeed(URI ownerURI, Model content, boolean activate, URI wonNodeUri) throws IllegalNeedContentException {
        //TODO: make asynchonous
        try {
            OwnerProtocolNeedWebServiceEndpoint proxy = clientFactory.getOwnerProtocolEndpoint(wonNodeUri);
            content.setNsPrefix("",ownerURI.toString());
            String modelAsString = RdfUtils.toString(content);
            URI result = proxy.createNeed(ownerURI, modelAsString , activate);
            SettableFuture<URI> futureResult = SettableFuture.create();
            futureResult.set(result);
            return  futureResult;
        } catch (MalformedURLException e) {
            logger.warn("couldn't create URL for needProtocolEndpoint", e);
        } catch (NoSuchNeedException e) {
            logger.warn("caught NoSuchNeedException:", e);
        } catch (IllegalNeedContentFault illegalNeedContentFault) {
            throw IllegalNeedContentFault.toException(illegalNeedContentFault);
        }
        return null;
    }

    //@Override
    public void activate(URI needURI) throws NoSuchNeedException {
        try {
            OwnerProtocolNeedWebServiceEndpoint proxy = clientFactory.getOwnerProtocolEndpointForNeed(needURI);
            proxy.activate(needURI);
        } catch (MalformedURLException e) {
            logger.warn("couldn't create URL for needProtocolEndpoint", e);
        } catch (NoSuchNeedFault noSuchNeedFault) {
            throw NoSuchNeedFault.toException(noSuchNeedFault);
        }
    }
    //@Override
    public void deactivate(URI needURI) throws NoSuchNeedException, NoSuchConnectionFault, IllegalMessageForConnectionStateFault {
        try {
            OwnerProtocolNeedWebServiceEndpoint proxy = clientFactory.getOwnerProtocolEndpointForNeed(needURI);
            proxy.deactivate(needURI);
        } catch (MalformedURLException e) {
            logger.warn("couldn't create URL for needProtocolEndpoint", e);
        } catch (NoSuchNeedFault noSuchNeedFault) {
            throw NoSuchNeedFault.toException(noSuchNeedFault);
        }
    }

    //@Override
    public ListenableFuture<URI> connect(URI needURI, URI otherNeedURI, Model content) throws
            NoSuchNeedException, IllegalMessageForNeedStateException, ConnectionAlreadyExistsException {
        //TODO: make asynchonous
        try {
            OwnerProtocolNeedWebServiceEndpoint proxy = clientFactory.getOwnerProtocolEndpointForNeed(needURI);
            SettableFuture futureResult = SettableFuture.create();
            futureResult.set(proxy.connect(needURI, otherNeedURI, RdfUtils.toString(content)));
            return futureResult;
        } catch (MalformedURLException e) {
            logger.warn("couldn't create URL for needProtocolEndpoint", e);
        } catch (NoSuchNeedFault noSuchNeedFault) {
            throw NoSuchNeedFault.toException(noSuchNeedFault);
        } catch (ConnectionAlreadyExistsFault connectionAlreadyExistsFault) {
            throw ConnectionAlreadyExistsFault.toException(connectionAlreadyExistsFault);
        } catch (IllegalMessageForNeedStateFault illegalMessageForNeedStateFault) {
            throw IllegalMessageForNeedStateFault.toException(illegalMessageForNeedStateFault);
        }
        return null;
    }
    //@Override
    public Collection<URI> listNeedURIs() {
        throw new UnsupportedOperationException("not implemented");
    }

    //@Override
    public Collection<URI> listNeedURIs(int page) {
        throw new UnsupportedOperationException("not implemented");
    }

    //@Override
    public Collection<URI> listConnectionURIs(URI needURI) throws NoSuchNeedException {
        throw new UnsupportedOperationException("not implemented");
    }

    //@Override
    public Collection<URI> listConnectionURIs() {
        throw new UnsupportedOperationException("not implemented");
    }

    //@Override
    public Collection<URI> listConnectionURIs(int page) {
        throw new UnsupportedOperationException("not implemented");
    }

    //@Override
    public Collection<URI> listConnectionURIs(URI needURI, int page) throws NoSuchNeedException {
        throw new UnsupportedOperationException("not implemented");
    }

    //@Override
    public Need readNeed(URI needURI) throws NoSuchNeedException {
        throw new UnsupportedOperationException("not implemented");
    }

    //@Override
    public Model readNeedContent(URI needURI) throws NoSuchNeedException {
        throw new UnsupportedOperationException("not implemented");
    }

    //@Override
    public Connection readConnection(URI connectionURI) throws NoSuchConnectionException {
        throw new UnsupportedOperationException("not implemented");
    }

    //@Override
    public List<ConnectionEvent> readEvents(URI connectionURI) throws NoSuchConnectionException {
        throw new UnsupportedOperationException("not implemented");
    }
    //@Override
    public Model readConnectionContent(URI connectionURI) throws NoSuchConnectionException {
        throw new UnsupportedOperationException("not implemented");
    }

    public void setClientFactory(OwnerProtocolNeedClientFactory clientFactory) {
        //To change body of created methods use File | Settings | File Templates.
    }


    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {

    }
}

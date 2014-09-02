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

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import won.node.protocol.impl.NeedProtocolNeedClientFactory;
import won.protocol.exception.*;
import won.protocol.model.Connection;
import won.protocol.need.NeedProtocolNeedClientSide;
import won.protocol.util.RdfUtils;
import won.protocol.ws.NeedProtocolNeedWebServiceEndpoint;
import won.protocol.ws.fault.ConnectionAlreadyExistsFault;
import won.protocol.ws.fault.IllegalMessageForConnectionStateFault;
import won.protocol.ws.fault.IllegalMessageForNeedStateFault;
import won.protocol.ws.fault.NoSuchConnectionFault;

import java.net.MalformedURLException;
import java.net.URI;

/**
 * User: fkleedorfer
 * Date: 28.11.12
 */
public class NeedProtocolNeedClientImplWSBased implements NeedProtocolNeedClientSide
{
  final Logger logger = LoggerFactory.getLogger(getClass());

  @Autowired
  private NeedProtocolNeedClientFactory clientFactory;

  @Override
  public ListenableFuture<URI> connect(final URI needUri, final URI otherNeedUri,
                                       final URI otherConnectionUri, final Model content,
                                       final Dataset messageEvent)
          throws NoSuchNeedException, IllegalMessageForNeedStateException, ConnectionAlreadyExistsException
  {
    try {
      //TODO: make asynchronous
      NeedProtocolNeedWebServiceEndpoint proxy = clientFactory.getNeedProtocolEndpointForNeed(needUri);
      URI result = proxy.connect(needUri, otherNeedUri, otherConnectionUri, RdfUtils.toString(content));
      SettableFuture<URI> futureResult = SettableFuture.create();
      futureResult.set(result);
    } catch (MalformedURLException e) {
      //TODO think this through: what happens if we return null here?
      logger.warn("couldn't create URL for needProtocolEndpoint", e);
    } catch (ConnectionAlreadyExistsFault connectionAlreadyExistsFault) {
      throw ConnectionAlreadyExistsFault.toException(connectionAlreadyExistsFault);
    } catch (IllegalMessageForNeedStateFault illegalMessageForNeedStateFault) {
      throw IllegalMessageForNeedStateFault.toException(illegalMessageForNeedStateFault);
    }
    return null;
  }

    @Override
    public void open(final Connection connection, final Model content, final Dataset messageEvent)
      throws NoSuchConnectionException, IllegalMessageForConnectionStateException, IllegalMessageForNeedStateException {
        try {
            NeedProtocolNeedWebServiceEndpoint proxy = clientFactory.getNeedProtocolEndpointForConnection(connection.getRemoteConnectionURI());
            proxy.open(connection.getRemoteConnectionURI(), RdfUtils.toString(content));
        } catch (MalformedURLException e) {
            logger.warn("couldnt create URL for needProtocolEndpoint", e);
        } catch (IllegalMessageForConnectionStateFault illegalMessageForConnectionStateFault) {
          throw IllegalMessageForConnectionStateFault.toException(illegalMessageForConnectionStateFault);
        } catch (NoSuchConnectionFault noSuchConnectionFault) {
          throw NoSuchConnectionFault.toException(noSuchConnectionFault);
        }
    }

  @Override
  public void close(final Connection connection, final Model content, final Dataset messageEvent)
          throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
    try {
      NeedProtocolNeedWebServiceEndpoint proxy = clientFactory.getNeedProtocolEndpointForConnection(connection.getRemoteConnectionURI());
      proxy.close(connection.getRemoteConnectionURI(), RdfUtils.toString(content));
    } catch (MalformedURLException e) {
      logger.warn("couldn't create URL for needProtocolEndpoint", e);
    } catch (IllegalMessageForConnectionStateFault illegalMessageForConnectionStateFault) {
      throw IllegalMessageForConnectionStateFault.toException(illegalMessageForConnectionStateFault);
    } catch (NoSuchConnectionFault noSuchConnectionFault) {
      throw NoSuchConnectionFault.toException(noSuchConnectionFault);
    }
  }

  @Override
  public void sendMessage(final Connection connection, final Model message, final Dataset messageEvent)
          throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
    try {
      NeedProtocolNeedWebServiceEndpoint proxy = clientFactory.getNeedProtocolEndpointForConnection(connection.getRemoteConnectionURI());
      String messageConvert = RdfUtils.toString(message);
      proxy.sendMessage(connection.getRemoteConnectionURI(), messageConvert);
    } catch (MalformedURLException e) {
      logger.warn("couldn't create URL for needProtocolEndpoint", e);
    } catch (IllegalMessageForConnectionStateFault illegalMessageForConnectionStateFault) {
      throw IllegalMessageForConnectionStateFault.toException(illegalMessageForConnectionStateFault);
    } catch (NoSuchConnectionFault noSuchConnectionFault) {
      throw NoSuchConnectionFault.toException(noSuchConnectionFault);
    }
  }

  public void setClientFactory(final NeedProtocolNeedClientFactory clientFactory)
  {
    this.clientFactory = clientFactory;
  }


}
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import won.node.protocol.impl.NeedProtocolNeedClientFactory;
import won.protocol.need.NeedProtocolNeedClientSide;
import won.protocol.util.RdfUtils;
import won.protocol.ws.NeedProtocolNeedWebServiceEndpoint;
import won.protocol.exception.*;
import won.protocol.need.NeedProtocolNeedService;
import won.protocol.ws.fault.ConnectionAlreadyExistsFault;
import won.protocol.ws.fault.IllegalMessageForConnectionStateFault;
import won.protocol.ws.fault.IllegalMessageForNeedStateFault;
import won.protocol.ws.fault.NoSuchConnectionFault;

import java.net.MalformedURLException;
import java.net.URI;
import java.text.MessageFormat;

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
  public URI connect(final URI needURI, final URI otherNeedURI, final URI otherConnectionURI, final Model content) throws NoSuchNeedException, IllegalMessageForNeedStateException, ConnectionAlreadyExistsException
  {

    logger.info("need-facing: CONNECT called for other need {}, own need {}, own connection {}, and content {}",
        new Object[]{needURI, otherNeedURI, otherConnectionURI, content});
    try {
      NeedProtocolNeedWebServiceEndpoint proxy = clientFactory.getNeedProtocolEndpointForNeed(needURI);
      return proxy.connect(needURI, otherNeedURI, otherConnectionURI, RdfUtils.toString(content));
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
    public void open(final URI connectionURI, final Model content) throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
        logger.info(MessageFormat.format("need-facing: OPEN called for connection {0}", connectionURI));
        try {
            NeedProtocolNeedWebServiceEndpoint proxy = clientFactory.getNeedProtocolEndpointForConnection(connectionURI);
            proxy.open(connectionURI, RdfUtils.toString(content));
        } catch (MalformedURLException e) {
            logger.warn("couldnt create URL for needProtocolEndpoint", e);
        } catch (IllegalMessageForConnectionStateFault illegalMessageForConnectionStateFault) {
          throw IllegalMessageForConnectionStateFault.toException(illegalMessageForConnectionStateFault);
        } catch (NoSuchConnectionFault noSuchConnectionFault) {
          throw NoSuchConnectionFault.toException(noSuchConnectionFault);
        }
    }

  @Override
  public void close(final URI connectionURI, final Model content) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
    logger.info("need-facing: CLOSE called for connection {}", connectionURI);
    try {
      NeedProtocolNeedWebServiceEndpoint proxy = clientFactory.getNeedProtocolEndpointForConnection(connectionURI);
      proxy.close(connectionURI, RdfUtils.toString(content));
    } catch (MalformedURLException e) {
      logger.warn("couldn't create URL for needProtocolEndpoint", e);
    } catch (IllegalMessageForConnectionStateFault illegalMessageForConnectionStateFault) {
      throw IllegalMessageForConnectionStateFault.toException(illegalMessageForConnectionStateFault);
    } catch (NoSuchConnectionFault noSuchConnectionFault) {
      throw NoSuchConnectionFault.toException(noSuchConnectionFault);
    }
  }

  @Override
  public void textMessage(final URI connectionURI, final String message) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
    logger.info("need-facing: SEND_TEXT_MESSAGE called for connection {} with message {}", connectionURI, message);
    try {
      NeedProtocolNeedWebServiceEndpoint proxy = clientFactory.getNeedProtocolEndpointForConnection(connectionURI);
      proxy.textMessage(connectionURI, message);
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
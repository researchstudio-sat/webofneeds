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

package won.node.protocol.impl;

import com.hp.hpl.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import won.protocol.util.RdfUtils;
import won.protocol.ws.NeedProtocolNeedWebServiceEndpoint;
import won.protocol.exception.*;
import won.protocol.need.NeedProtocolNeedService;

import java.net.MalformedURLException;
import java.net.URI;
import java.text.MessageFormat;

/**
 * User: fkleedorfer
 * Date: 28.11.12
 */
public class NeedProtocolNeedClientImpl implements NeedProtocolNeedService
{
  final Logger logger = LoggerFactory.getLogger(getClass());

  @Autowired
  private NeedProtocolNeedClientFactory clientFactory;

  @Autowired
  private RdfUtils rdfUtils;

  @Override
  public URI connect(final URI needURI, final URI otherNeedURI, final URI otherConnectionURI, final Model content) throws NoSuchNeedException, IllegalMessageForNeedStateException, ConnectionAlreadyExistsException
  {
    logger.info(MessageFormat.format("need-facing: CONNECTION_REQUESTED called for other need {0}, own need {1}, own connection {2} and message {3}", needURI, otherNeedURI, otherConnectionURI, content));
    try {
      NeedProtocolNeedWebServiceEndpoint proxy = clientFactory.getNeedProtocolEndpointForNeed(needURI);
      return proxy.connect(needURI, otherNeedURI, otherConnectionURI, rdfUtils.toString(content));
    } catch (MalformedURLException e) {
      //TODO think this through: what happens if we return null here?
      logger.warn("couldnt create URL for needProtocolEndpoint", e);
    }
    return null;
  }

    @Override
    public void open(final URI connectionURI, final Model content) throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
        logger.info(MessageFormat.format("need-facing: OPEN called for connection {0}", connectionURI));
        try {
            NeedProtocolNeedWebServiceEndpoint proxy = clientFactory.getNeedProtocolEndpointForConnection(connectionURI);
            proxy.open(connectionURI, rdfUtils.toString(content));
        } catch (MalformedURLException e) {
            logger.warn("couldnt create URL for needProtocolEndpoint", e);
        }
    }

  @Override
  public void close(final URI connectionURI, final Model content) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
    logger.info(MessageFormat.format("need-facing: CLOSE called for connection {0}", connectionURI));
    try {
      NeedProtocolNeedWebServiceEndpoint proxy = clientFactory.getNeedProtocolEndpointForConnection(connectionURI);
      proxy.close(connectionURI, rdfUtils.toString(content));
    } catch (MalformedURLException e) {
      logger.warn("couldnt create URL for needProtocolEndpoint", e);
    }
  }

  @Override
  public void sendTextMessage(final URI connectionURI, final String message) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
    logger.info(MessageFormat.format("need-facing: SEND_TEXT_MESSAGE called for connection {0} with message {1}", connectionURI, message));
    try {
      NeedProtocolNeedWebServiceEndpoint proxy = clientFactory.getNeedProtocolEndpointForConnection(connectionURI);
      proxy.sendTextMessage(connectionURI, message);
    } catch (MalformedURLException e) {
      logger.warn("couldnt create URL for needProtocolEndpoint", e);
    }
  }

  public void setClientFactory(final NeedProtocolNeedClientFactory clientFactory)
  {
    this.clientFactory = clientFactory;
  }

  public void setRdfUtils(final RdfUtils rdfUtils)
  {
    this.rdfUtils = rdfUtils;
  }

}
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
import won.protocol.rest.LinkedDataRestClient;
import won.node.ws.NeedProtocolNeedWebServiceClient;
import won.protocol.ws.NeedProtocolNeedWebServiceEndpoint;
import won.protocol.exception.*;
import won.protocol.vocabulary.WON;
import won.protocol.need.NeedProtocolNeedService;

import java.io.StringWriter;
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

  private LinkedDataRestClient linkedDataRestClient;


  @Override
  public URI connect(final URI needURI, final URI otherNeedURI, final URI otherConnectionURI, final Model content) throws NoSuchNeedException, IllegalMessageForNeedStateException, ConnectionAlreadyExistsException
  {
    logger.info(MessageFormat.format("need-facing: CONNECTION_REQUESTED called for other need {0}, own need {1}, own connection {2} and message {3}", needURI, otherNeedURI, otherConnectionURI, content));
    try {
      NeedProtocolNeedWebServiceEndpoint proxy = getNeedProtocolEndpointForNeed(needURI);
      StringWriter sw = new StringWriter();
      content.write(sw, "TTL");
      return proxy.connect(needURI, otherNeedURI, otherConnectionURI, sw.toString());
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
            NeedProtocolNeedWebServiceEndpoint proxy = getNeedProtocolEndpointForConnection(connectionURI);
            StringWriter sw = new StringWriter();
            content.write(sw, "TTL");
            proxy.open(connectionURI, sw.toString());
        } catch (MalformedURLException e) {
            logger.warn("couldnt create URL for needProtocolEndpoint", e);
        }
    }

  @Override
  public void close(final URI connectionURI, final Model content) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
    logger.info(MessageFormat.format("need-facing: CLOSE called for connection {0}", connectionURI));
    try {
      NeedProtocolNeedWebServiceEndpoint proxy = getNeedProtocolEndpointForConnection(connectionURI);
      StringWriter sw = new StringWriter();
      content.write(sw, "TTL");
      proxy.close(connectionURI, sw.toString());
    } catch (MalformedURLException e) {
      logger.warn("couldnt create URL for needProtocolEndpoint", e);
    }
  }

  @Override
  public void sendTextMessage(final URI connectionURI, final String message) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
    logger.info(MessageFormat.format("need-facing: SEND_TEXT_MESSAGE called for connection {0} with message {1}", connectionURI, message));
    try {
      NeedProtocolNeedWebServiceEndpoint proxy = getNeedProtocolEndpointForConnection(connectionURI);
      proxy.sendTextMessage(connectionURI, message);
    } catch (MalformedURLException e) {
      logger.warn("couldnt create URL for needProtocolEndpoint", e);
    }
  }

  private NeedProtocolNeedWebServiceEndpoint getNeedProtocolEndpointForNeed(URI needURI) throws NoSuchNeedException, MalformedURLException
  {
    //TODO: fetch endpoint information for the need and store in db?
    URI needProtocolEndpoint = linkedDataRestClient.getURIPropertyForResource(needURI, WON.NEED_PROTOCOL_ENDPOINT);
    logger.info("need protocol endpoint of need {} is {}", needURI.toString(), needProtocolEndpoint.toString());
    if (needProtocolEndpoint == null) throw new NoSuchNeedException(needURI);
    NeedProtocolNeedWebServiceClient client = new NeedProtocolNeedWebServiceClient(URI.create(needProtocolEndpoint.toString() + "?wsdl").toURL());
    return client.getNeedProtocolNeedWebServiceEndpointPort();
  }

  private NeedProtocolNeedWebServiceEndpoint getNeedProtocolEndpointForConnection(URI connectionURI) throws NoSuchConnectionException, MalformedURLException
  {
    //TODO: fetch endpoint information for the need and store in db?
    URI needProtocolEndpoint = linkedDataRestClient.getURIPropertyForResource(connectionURI, WON.NEED_PROTOCOL_ENDPOINT);
    logger.info("need protocol endpoint of connection {} is {}", connectionURI.toString(), needProtocolEndpoint.toString());
    if (needProtocolEndpoint == null) throw new NoSuchConnectionException(connectionURI);
    NeedProtocolNeedWebServiceClient client = new NeedProtocolNeedWebServiceClient(URI.create(needProtocolEndpoint.toString() + "?wsdl").toURL());
    return client.getNeedProtocolNeedWebServiceEndpointPort();
  }

  public void setLinkedDataRestClient(final LinkedDataRestClient linkedDataRestClient)
  {
    this.linkedDataRestClient = linkedDataRestClient;
  }
}

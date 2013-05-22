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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import won.node.ws.OwnerProtocolOwnerWebServiceClient;
import won.protocol.exception.*;
import won.protocol.model.Need;
import won.protocol.owner.OwnerProtocolOwnerService;
import won.protocol.repository.ConnectionRepository;
import won.protocol.repository.NeedRepository;
import won.protocol.rest.LinkedDataRestClient;
import won.protocol.ws.OwnerProtocolOwnerWebServiceEndpoint;

import java.net.MalformedURLException;
import java.net.URI;
import java.text.MessageFormat;

/**
 * TODO: Empty owner client implementation to be replaced by WS client.
 */
public class OwnerProtocolOwnerClientEmptyImpl implements OwnerProtocolOwnerService
{
  final Logger logger = LoggerFactory.getLogger(getClass());

  @Autowired
  private LinkedDataRestClient linkedDataRestClient;

  @Autowired
  private NeedRepository needRepository;

  @Autowired
  private ConnectionRepository connectionRepository;

  public void setNeedRepository(NeedRepository re) {
    needRepository = re;
  }
  public void setNeedRepository(ConnectionRepository re) {
      connectionRepository = re;
  }

  @Override
  public void hintReceived(final URI ownNeedURI, final URI otherNeedURI, final double score, final URI originatorURI) throws NoSuchNeedException
  {
    logger.info("owner-facing: HINT_RECEIVED called for own need {}, other need {}, with score {} from originator {}",
        new Object[]{ownNeedURI, otherNeedURI, score, originatorURI});
      try {
          OwnerProtocolOwnerWebServiceEndpoint proxy = getOwnerProtocolEndpointForNeed(ownNeedURI);
          proxy.hintReceived(ownNeedURI, otherNeedURI, score, originatorURI);
      } catch (MalformedURLException e) {
          logger.warn("couldn't create URL for needProtocolEndpoint", e);
      } catch (IllegalMessageForNeedStateException e) {
          logger.warn("caught IllegalMessageForNeedStateException:", e);
      }
  }

  @Override
  public void connectionRequested(final URI ownNeedURI, final URI otherNeedURI, final URI ownConnectionURI, final String message) throws NoSuchNeedException, ConnectionAlreadyExistsException, IllegalMessageForNeedStateException
  {
    logger.info("owner-facing: CONNECTION_REQUESTED called for own need {}, other need {}, own connection {} and message ''{}''",
        new Object[]{ownNeedURI, otherNeedURI, ownConnectionURI, message});
      try {
          OwnerProtocolOwnerWebServiceEndpoint proxy = getOwnerProtocolEndpointForNeed(ownNeedURI);
          proxy.connectionRequested(ownNeedURI, otherNeedURI, ownConnectionURI, message);
      } catch (MalformedURLException e) {
          logger.warn("couldn't create URL for needProtocolEndpoint", e);
      } catch (IllegalMessageForNeedStateException e) {
          logger.warn("caught IllegalMessageForConnectionStateException", e);
      }
  }

  @Override
  public void close(final URI connectionURI) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
    logger.info("owner-facing: CLOSE called for connection {}",connectionURI);
      try {
          OwnerProtocolOwnerWebServiceEndpoint proxy = getOwnerProtocolEndpointForConnection(connectionURI);
          proxy.close(connectionURI);
      } catch (MalformedURLException e) {
          logger.warn("couldn't create URL for needProtocolEndpoint", e);
      } catch (NoSuchNeedException e) {
          logger.warn("caught NoSuchConnectionException:", e);
      }
  }

  @Override
  public void sendTextMessage(final URI connectionURI, final String message) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
    logger.info("owner-facing: SEND_TEXT_MESSAGE called for connection {} with message {}", connectionURI, message);
      try {
          OwnerProtocolOwnerWebServiceEndpoint proxy = getOwnerProtocolEndpointForConnection(connectionURI);
          proxy.sendTextMessage(connectionURI, message);
      } catch (MalformedURLException e) {
          logger.warn("couldn't create URL for needProtocolEndpoint", e);
      } catch (NoSuchNeedException e) {
          logger.warn("caught NoSuchConnectionException:", e);
      }
  }


    private OwnerProtocolOwnerWebServiceEndpoint getOwnerProtocolEndpointForNeed(URI needURI) throws NoSuchNeedException, MalformedURLException
    {
        //TODO: fetch endpoint information for the need and store in db?
      //  URI needProtocolEndpoint = linkedDataRestClient.getURIPropertyForResource(needURI, WON.OWNER_PROTOCOL_ENDPOINT);
      //  logger.debug("need protocol endpoint of need {} is {}", needURI, needProtocolEndpoint);
      //  if (needProtocolEndpoint == null) throw new NoSuchNeedException(needURI);

        Need own = needRepository.findByNeedURI(needURI).get(0);
        OwnerProtocolOwnerWebServiceClient client = new OwnerProtocolOwnerWebServiceClient(URI.create(own.getOwnerURI().toString() + "?wsdl").toURL());
        return client.getOwnerProtocolOwnerWebServiceEndpointPort();
    }

    private OwnerProtocolOwnerWebServiceEndpoint getOwnerProtocolEndpointForConnection(URI connectionURI) throws NoSuchConnectionException, MalformedURLException,
            NoSuchNeedException {
        //TODO: fetch endpoint information for the need and store in db?
      //  URI needProtocolEndpoint = linkedDataRestClient.getURIPropertyForResource(connectionURI, WON.OWNER_PROTOCOL_ENDPOINT);
      //  logger.debug("need protocol endpoint of connection {} is {}", connectionURI, needProtocolEndpoint);
      //  if (needProtocolEndpoint == null) throw new NoSuchConnectionException(connectionURI);

        return getOwnerProtocolEndpointForNeed(connectionRepository.findByConnectionURI(connectionURI).get(0).getNeedURI());
    }

  public void setLinkedDataRestClient(final LinkedDataRestClient linkedDataRestClient)
  {
    this.linkedDataRestClient = linkedDataRestClient;
  }
}

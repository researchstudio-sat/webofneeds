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
import won.node.ws.OwnerProtocolOwnerWebServiceClient;
import won.protocol.exception.*;
import won.protocol.model.Need;
import won.protocol.owner.OwnerProtocolOwnerService;
import won.protocol.repository.ConnectionRepository;
import won.protocol.repository.NeedRepository;
import won.protocol.rest.LinkedDataRestClient;
import won.protocol.ws.OwnerProtocolOwnerWebServiceEndpoint;

import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.text.MessageFormat;

public class OwnerProtocolOwnerClientImpl implements OwnerProtocolOwnerService
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
  public void hint(final URI ownNeedURI, final URI otherNeedURI, final double score, final URI originatorURI, final Model content) throws NoSuchNeedException
  {
    logger.info(MessageFormat.format("owner-facing: HINT_RECEIVED called for own need {0}, other need {1}, with score {2} from originator {3} and content {4}",  ownNeedURI,otherNeedURI,score,originatorURI, content));
      try {
          OwnerProtocolOwnerWebServiceEndpoint proxy = getOwnerProtocolEndpointForNeed(ownNeedURI);
          StringWriter sw = new StringWriter();
          content.write(sw, "TTL");
          proxy.hint(ownNeedURI, otherNeedURI, score, originatorURI, sw.toString());
      } catch (MalformedURLException e) {
          logger.warn("couldn't create URL for needProtocolEndpoint", e);
      } catch (IllegalMessageForNeedStateException e) {
          e.printStackTrace();
      }
  }

  @Override
  public void connect(final URI ownNeedURI, final URI otherNeedURI, final URI ownConnectionURI, final Model content) throws NoSuchNeedException, ConnectionAlreadyExistsException, IllegalMessageForNeedStateException
  {
    logger.info(MessageFormat.format("owner-facing: CONNECTION_REQUESTED called for own need {0}, other need {1}, own connection {2} and message ''{3}''", ownNeedURI,otherNeedURI,ownConnectionURI, content));
      try {
          OwnerProtocolOwnerWebServiceEndpoint proxy = getOwnerProtocolEndpointForNeed(ownNeedURI);
          StringWriter sw = new StringWriter();
          content.write(sw, "TTL");
          proxy.connect(ownNeedURI, otherNeedURI, ownConnectionURI, sw.toString());
      } catch (MalformedURLException e) {
          logger.warn("couldn't create URL for needProtocolEndpoint", e);
      } catch (IllegalMessageForNeedStateException e) {
          e.printStackTrace();
      }
  }

    @Override
    public void open(final URI connectionURI, final Model content) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
    {
       logger.info(MessageFormat.format("owner-facing: CLOSE called for connection {0}",connectionURI));
       try {
           OwnerProtocolOwnerWebServiceEndpoint proxy = getOwnerProtocolEndpointForConnection(connectionURI);

           StringWriter sw = new StringWriter();
           content.write(sw, "TTL");
           proxy.open(connectionURI, sw.toString());
       } catch (MalformedURLException e) {
           logger.warn("couldn't create URL for needProtocolEndpoint", e);
       } catch (NoSuchNeedException e) {
           e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
       }
    }

    @Override
  public void close(final URI connectionURI, final Model content) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
    logger.info(MessageFormat.format("owner-facing: CLOSE called for connection {0}",connectionURI));
      try {
          OwnerProtocolOwnerWebServiceEndpoint proxy = getOwnerProtocolEndpointForConnection(connectionURI);
          StringWriter sw = new StringWriter();
          content.write(sw, "TTL");
          proxy.close(connectionURI, sw.toString());
      } catch (MalformedURLException e) {
          logger.warn("couldn't create URL for needProtocolEndpoint", e);
      } catch (NoSuchNeedException e) {
          e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
      }
  }

  @Override
  public void sendTextMessage(final URI connectionURI, final String message) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
    logger.info(MessageFormat.format("owner-facing: SEND_TEXT_MESSAGE called for connection {0} with message {1}",connectionURI, message));
      try {
          OwnerProtocolOwnerWebServiceEndpoint proxy = getOwnerProtocolEndpointForConnection(connectionURI);
          proxy.sendTextMessage(connectionURI, message);
      } catch (MalformedURLException e) {
          logger.warn("couldn't create URL for needProtocolEndpoint", e);
      } catch (NoSuchNeedException e) {
          e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
      }
  }


    private OwnerProtocolOwnerWebServiceEndpoint getOwnerProtocolEndpointForNeed(URI needURI) throws NoSuchNeedException, MalformedURLException
    {
        //TODO: fetch endpoint information for the need and store in db?
      //  URI needProtocolEndpoint = linkedDataRestClient.getURIPropertyForResource(needURI, WON.OWNER_PROTOCOL_ENDPOINT);
      //  logger.info("need protocol endpoint of need {} is {}", needURI.toString(), needProtocolEndpoint.toString());
      //  if (needProtocolEndpoint == null) throw new NoSuchNeedException(needURI);

        Need own = needRepository.findByNeedURI(needURI).get(0);
        OwnerProtocolOwnerWebServiceClient client = new OwnerProtocolOwnerWebServiceClient(URI.create(own.getOwnerURI().toString() + "?wsdl").toURL());
        return client.getOwnerProtocolOwnerWebServiceEndpointPort();
    }

    private OwnerProtocolOwnerWebServiceEndpoint getOwnerProtocolEndpointForConnection(URI connectionURI) throws NoSuchConnectionException, MalformedURLException,
            NoSuchNeedException {
        //TODO: fetch endpoint information for the need and store in db?
      //  URI needProtocolEndpoint = linkedDataRestClient.getURIPropertyForResource(connectionURI, WON.OWNER_PROTOCOL_ENDPOINT);
      //  logger.info("need protocol endpoint of connection {} is {}", connectionURI.toString(), needProtocolEndpoint.toString());
      //  if (needProtocolEndpoint == null) throw new NoSuchConnectionException(connectionURI);

        return getOwnerProtocolEndpointForNeed(connectionRepository.findByConnectionURI(connectionURI).get(0).getNeedURI());
    }

  public void setLinkedDataRestClient(final LinkedDataRestClient linkedDataRestClient)
  {
    this.linkedDataRestClient = linkedDataRestClient;
  }
}
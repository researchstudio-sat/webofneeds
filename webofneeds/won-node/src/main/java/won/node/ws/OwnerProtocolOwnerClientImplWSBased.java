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
import won.node.protocol.impl.OwnerProtocolOwnerClientFactory;
import won.protocol.exception.*;
import won.protocol.jms.MessagingService;
import won.protocol.owner.OwnerProtocolOwnerServiceClientSide;
import won.protocol.repository.ConnectionRepository;
import won.protocol.repository.NeedRepository;
import won.protocol.util.RdfUtils;
import won.protocol.ws.OwnerProtocolOwnerWebServiceEndpoint;
import won.protocol.ws.fault.*;

import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URI;

public class OwnerProtocolOwnerClientImplWSBased implements OwnerProtocolOwnerServiceClientSide
{
  final Logger logger = LoggerFactory.getLogger(getClass());


  @Autowired
  private OwnerProtocolOwnerClientFactory clientFactory;

  private MessagingService messagingService;

  @Autowired
  private NeedRepository needRepository;

    @Autowired
    private ConnectionRepository connectionRepository;

    @Override
  public void hint(final URI ownNeedUri, final URI otherNeedUri, final double score, final URI originatorUri, final Model content) throws NoSuchNeedException, IllegalMessageForNeedStateException
  {
    try {
      OwnerProtocolOwnerWebServiceEndpoint proxy = clientFactory.getOwnerProtocolEndpointForNeed(ownNeedUri);
      StringWriter sw = new StringWriter();
      content.write(sw, "TTL");
      proxy.hint(ownNeedUri, otherNeedUri, score, originatorUri, sw.toString());
    } catch (MalformedURLException e) {
      logger.warn("couldn't create URL for needProtocolEndpoint", e);
    } catch (NoSuchNeedFault noSuchNeedFault) {
      throw NoSuchNeedFault.toException(noSuchNeedFault);
    } catch (IllegalMessageForNeedStateFault illegalMessageForNeedStateFault) {
      logger.warn("couldn't send hint", illegalMessageForNeedStateFault);
    }
  }


    @Override
    public void connect(final URI ownNeedURI, final URI otherNeedURI, final URI ownConnectionURI, final Model content) throws NoSuchNeedException, ConnectionAlreadyExistsException, IllegalMessageForNeedStateException
    {
      try {
        OwnerProtocolOwnerWebServiceEndpoint proxy = clientFactory.getOwnerProtocolEndpointForNeed(ownNeedURI);
        StringWriter sw = new StringWriter();
        content.write(sw, "TTL");
        proxy.connect(ownNeedURI, otherNeedURI, ownConnectionURI, sw.toString());
      } catch (MalformedURLException e) {
        logger.warn("couldn't create URL for needProtocolEndpoint", e);
      } catch (NoSuchNeedFault noSuchNeedFault) {
        NoSuchNeedFault.toException(noSuchNeedFault);
      } catch (ConnectionAlreadyExistsFault connectionAlreadyExistsFault) {
        throw ConnectionAlreadyExistsFault.toException(connectionAlreadyExistsFault);
      } catch (IllegalMessageForNeedStateFault illegalMessageForNeedStateFault) {
        logger.warn("couldn't send hint", illegalMessageForNeedStateFault);
      }
    }
    @Override
  public void open(final URI connectionURI, final Model content) throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
    try {
      OwnerProtocolOwnerWebServiceEndpoint proxy = clientFactory.getOwnerProtocolEndpointForConnection(connectionURI);
      proxy.open(connectionURI, RdfUtils.toString(content));
    } catch (MalformedURLException e) {
      logger.warn("couldn't create URL for needProtocolEndpoint", e);
    } catch (NoSuchNeedException e) {
      logger.warn("could not get owner protocol endpoint", e);
    }catch (IllegalMessageForConnectionStateFault illegalMessageForConnectionStateFault) {
      throw IllegalMessageForConnectionStateFault.toException(illegalMessageForConnectionStateFault);
    } catch (NoSuchConnectionFault noSuchConnectionFault) {
      throw NoSuchConnectionFault.toException(noSuchConnectionFault);
    }
  }


  @Override
  public void close(final URI connectionURI, final Model content) throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
    try {
      OwnerProtocolOwnerWebServiceEndpoint proxy = clientFactory.getOwnerProtocolEndpointForConnection(connectionURI);
      proxy.close(connectionURI, RdfUtils.toString(content));
    } catch (MalformedURLException e) {
      logger.warn("couldn't create URL for needProtocolEndpoint", e);
    } catch (NoSuchNeedException e) {
      logger.warn("could not get owner protocol endpoint", e);
    } catch (IllegalMessageForConnectionStateFault illegalMessageForConnectionStateFault) {
      illegalMessageForConnectionStateFault.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    } catch (NoSuchConnectionFault noSuchConnectionFault) {
      noSuchConnectionFault.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    }
  }

  @Override
  public void textMessage(final URI connectionURI, final Model message) throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
    try {
      OwnerProtocolOwnerWebServiceEndpoint proxy = clientFactory.getOwnerProtocolEndpointForConnection(connectionURI);
      String messageConvert = RdfUtils.toString(message);
      proxy.textMessage(connectionURI, messageConvert);
    } catch (MalformedURLException e) {
      logger.warn("couldn't create URL for needProtocolEndpoint", e);
    } catch (NoSuchNeedException e) {
      logger.warn("could not get owner protocol endpoint", e);
    } catch (IllegalMessageForConnectionStateFault illegalMessageForConnectionStateFault) {
      throw IllegalMessageForConnectionStateFault.toException(illegalMessageForConnectionStateFault);
    } catch (NoSuchConnectionFault noSuchConnectionFault) {
      throw NoSuchConnectionFault.toException(noSuchConnectionFault);
    }
  }

  public void setClientFactory(final OwnerProtocolOwnerClientFactory clientFactory)
  {
    this.clientFactory = clientFactory;
  }

    public void setNeedRepository(NeedRepository needRepository) {
        this.needRepository = needRepository;
    }

    public void setMessagingService(MessagingService messagingService) {
        this.messagingService = messagingService;
    }
}
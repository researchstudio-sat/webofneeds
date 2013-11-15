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
import won.protocol.exception.*;
import won.protocol.jms.MessagingService;
import won.protocol.need.NeedProtocolNeedService;
import won.protocol.util.RdfUtils;
import won.protocol.ws.NeedProtocolNeedWebServiceEndpoint;
import won.protocol.ws.fault.ConnectionAlreadyExistsFault;
import won.protocol.ws.fault.IllegalMessageForConnectionStateFault;
import won.protocol.ws.fault.IllegalMessageForNeedStateFault;
import won.protocol.ws.fault.NoSuchConnectionFault;

import java.net.MalformedURLException;
import java.net.URI;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * User: fkleedorfer
 * Date: 28.11.12
 */
public class NeedProtocolNeedClientImplJMSBased implements NeedProtocolNeedClientSide
{
  final Logger logger = LoggerFactory.getLogger(getClass());

  @Autowired
  private NeedProtocolNeedClientFactory clientFactory;

    public void setMessagingService(MessagingService messagingService) {
        this.messagingService = messagingService;
    }

    private MessagingService messagingService;

  @Override
  public Future<URI> connect(final URI needURI, final URI otherNeedURI, final URI otherConnectionURI, final Model content) throws NoSuchNeedException, IllegalMessageForNeedStateException, ConnectionAlreadyExistsException
  {
      Map headerMap = new HashMap<String, String>();
      headerMap.put("protocol","NeedProtocol");
      headerMap.put("needURI", needURI.toString()) ;
      headerMap.put("otherNeedURI", otherNeedURI.toString());
      headerMap.put("otherConnectionURI", otherConnectionURI.toString()) ;
      headerMap.put("content",RdfUtils.toString(content));
      Map properties = new HashMap();
      properties.put("methodName","connect");
      properties.put("protocol","NeedProtocol");
      return messagingService.sendInOutMessage(properties,headerMap,null, "outgoingMessages" );
  }
   /*
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
    }   */

    @Override
    public void open(final URI connectionURI, final Model content) throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
        logger.info(MessageFormat.format("need-facing: OPEN called for connection {0}", connectionURI));
        Map headerMap = new HashMap<String, String>();
        headerMap.put("protocol","NeedProtocol");
        headerMap.put("connectionURI", connectionURI.toString()) ;
        headerMap.put("content", RdfUtils.toString(content));
        Map properties = new HashMap();
        properties.put("methodName","open");
        messagingService.sendInOnlyMessage(properties,headerMap,null, "outgoingMessages" );
    }

    @Override
  public void close(final URI connectionURI, final Model content) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
      logger.info("need-facing: CLOSE called for connection {}", connectionURI);
      Map headerMap = new HashMap<String, String>();
      headerMap.put("protocol","NeedProtocol");
      headerMap.put("connectionURI", connectionURI.toString()) ;
      headerMap.put("content", RdfUtils.toString(content));
      Map properties = new HashMap();
      properties.put("methodName","close");
      messagingService.sendInOnlyMessage(properties,headerMap,null, "outgoingMessages" );

  }

  @Override
  public void textMessage(final URI connectionURI, final String message) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
      logger.info("need-facing: SEND_TEXT_MESSAGE called for connection {} with message {}", connectionURI, message);
      Map headerMap = new HashMap<String, String>();
      headerMap.put("protocol","NeedProtocol");
      headerMap.put("connectionURI", connectionURI.toString()) ;
      headerMap.put("content", message);
      Map properties = new HashMap();
      properties.put("methodName","textMessage");
      messagingService.sendInOnlyMessage(properties,headerMap,null, "outgoingMessages" );

  }

  public void setClientFactory(final NeedProtocolNeedClientFactory clientFactory)
  {
    this.clientFactory = clientFactory;
  }


}
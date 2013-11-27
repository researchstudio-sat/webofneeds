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
import org.apache.camel.CamelContext;
import org.apache.camel.CamelContextAware;
import org.apache.camel.Endpoint;
import org.apache.camel.component.seda.SedaEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import won.node.camel.routes.NeedProtocolDynamicRoutes;
import won.protocol.exception.*;
import won.protocol.jms.ActiveMQService;
import won.protocol.jms.MessagingService;
import won.protocol.model.Connection;
import won.protocol.need.NeedProtocolNeedClientSide;
import won.protocol.repository.ConnectionRepository;
import won.protocol.util.DataAccessUtils;
import won.protocol.util.RdfUtils;
import java.net.URI;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import static org.apache.activemq.camel.component.ActiveMQComponent.activeMQComponent;

/**
 * User: fkleedorfer
 * Date: 28.11.12
 */
public class NeedProtocolNeedClientImplJMSBased implements NeedProtocolNeedClientSide, CamelContextAware
{
  final Logger logger = LoggerFactory.getLogger(getClass());

    private MessagingService messagingService;

    private CamelContext camelContext;

    @Autowired
    private ConnectionRepository connectionRepository;

    @Autowired
    private NeedProtocolNeedClientFactory clientFactory;
    private ActiveMQService needProtocolActiveMQService;

    //TODO: debugging needed. when a established connection is closed then reconnected, both connections are in state "request sent"
  @Override
  public Future<URI> connect(final URI needURI, final URI otherNeedURI, final URI otherConnectionURI, final Model content) throws NoSuchNeedException, IllegalMessageForNeedStateException, ConnectionAlreadyExistsException
  {
      Map<String,String> headerMap = new HashMap<>();
      headerMap.put("protocol","NeedProtocol");
      headerMap.put("needURI", needURI.toString()) ;
      headerMap.put("otherNeedURI", otherNeedURI.toString());
      headerMap.put("otherConnectionURI", otherConnectionURI.toString()) ;
      headerMap.put("content",RdfUtils.toString(content));
      headerMap.put("methodName","connect");

      Map<String, String> propertyMap = new HashMap<>();
      //TODO: when shall be the remote won node unregistered?
      try {
          needProtocolActiveMQService.configureCamelEndpointForNeeds(needURI,otherNeedURI,"seda:NeedProtocol.out.connect");
      } catch (Exception e) {
          e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
      }

      headerMap.put("remoteBrokerEndpoint", needProtocolActiveMQService.getEndpoint());
      return messagingService.sendInOutMessageGeneric(propertyMap,headerMap,null,"seda:NeedProtocol.out.connect");



  }
    @Override
    public void open(final URI connectionURI, final Model content) throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
        logger.info(MessageFormat.format("need-facing: OPEN called for connection {0}", connectionURI));


        Map headerMap = new HashMap<String, String>();

        headerMap.put("protocol","NeedProtocol");
        headerMap.put("connectionURI", connectionURI.toString()) ;
        headerMap.put("content", RdfUtils.toString(content));
        headerMap.put("methodName","open");
        headerMap.put("remoteBrokerEndpoint", needProtocolActiveMQService.getEndpoint());
        try {
            needProtocolActiveMQService.configureCamelEndpointForConnection(connectionURI,"seda:NeedProtocol.out.open");
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        messagingService.sendInOnlyMessage(null,headerMap,null, "seda:NeedProtocol.out.open" );
    }

    @Override
  public void close(final URI connectionURI, final Model content) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
      logger.info("need-facing: CLOSE called for connection {}", connectionURI);
      Map headerMap = new HashMap<String, String>();
      headerMap.put("protocol","NeedProtocol");
      headerMap.put("connectionURI", connectionURI.toString()) ;
      headerMap.put("content", RdfUtils.toString(content));

      headerMap.put("methodName","close");

      try {
          needProtocolActiveMQService.configureCamelEndpointForConnection(connectionURI,"seda:NeedProtocol.out.close");
      } catch (Exception e) {
          e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
      }
      headerMap.put("remoteBrokerEndpoint", needProtocolActiveMQService.getEndpoint());
      messagingService.sendInOnlyMessage(null,headerMap,null, "seda:NeedProtocol.out.close" );

  }

  @Override
  public void textMessage(final URI connectionURI, final String message) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
      logger.info("need-facing: SEND_TEXT_MESSAGE called for connection {} with message {}", connectionURI, message);
      Map headerMap = new HashMap<String, String>();
      headerMap.put("protocol","NeedProtocol");
      headerMap.put("connectionURI", connectionURI.toString()) ;
      headerMap.put("content", message);
      headerMap.put("methodName","textMessage");

      try {
          needProtocolActiveMQService.configureCamelEndpointForConnection(connectionURI,"seda:NeedProtocol.out.textMessage");
      } catch (Exception e) {
          e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
      }
      headerMap.put("remoteBrokerEndpoint", needProtocolActiveMQService.getEndpoint());
      messagingService.sendInOnlyMessage(null,headerMap,null, "seda:NeedProtocol.out.textMessage" );

  }


    public void setMessagingService(MessagingService messagingService) {
        this.messagingService = messagingService;
    }


    public CamelContext getCamelContext() {
        return camelContext;
    }

    public void setCamelContext(CamelContext camelContext) {
        this.camelContext = camelContext;
    }

    public void setNeedProtocolActiveMQService(ActiveMQService needProtocolActiveMQService) {
        this.needProtocolActiveMQService = needProtocolActiveMQService;
    }
}
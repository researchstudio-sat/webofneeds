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

package won.node.messaging;

import com.google.common.util.concurrent.ListenableFuture;
import com.hp.hpl.jena.rdf.model.Model;
import org.apache.camel.CamelContext;
import org.apache.camel.CamelContextAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import won.node.protocol.impl.NeedProtocolNeedClientFactory;
import won.protocol.exception.*;
import won.protocol.jms.MessagingService;
import won.protocol.jms.NeedProtocolActiveMQService;
import won.protocol.model.Connection;
import won.protocol.need.NeedProtocolNeedClientSide;
import won.protocol.repository.ConnectionRepository;
import won.protocol.util.RdfUtils;

import java.net.URI;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

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
    private NeedProtocolActiveMQService needProtocolActiveMQService;

    //TODO: debugging needed. when a established connection is closed then reconnected, both connections are in state "request sent"
  @Override
  public ListenableFuture<URI> connect(final URI needURI, final URI otherNeedURI, final URI otherConnectionURI, final Model content) throws NoSuchNeedException, IllegalMessageForNeedStateException, ConnectionAlreadyExistsException
  {
      Map<String, String> headerMap = new HashMap<>();
      String endpoint=null;
      headerMap.put("protocol","NeedProtocol");
      headerMap.put("needURI", needURI.toString()) ;
      headerMap.put("otherNeedURI", otherNeedURI.toString());
      headerMap.put("otherConnectionURI", otherConnectionURI.toString()) ;
      headerMap.put("content",RdfUtils.toString(content));
      headerMap.put("methodName","connect");

      Map<String, String> propertyMap = new HashMap<>();
      //TODO: when shall be the remote won node unregistered?
      //TODO; shall be checked if the endpoint for the remote won node already exists. configuring remote endpoints for each message is inefficient
      try {
           endpoint = needProtocolActiveMQService.getCamelEndpointForNeed(otherNeedURI, needURI, "seda:NeedProtocol.out.connect");

      } catch (Exception e) {
          e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
      }

      headerMap.put("remoteBrokerEndpoint", endpoint);
      logger.info("sending connect message to remoteBrokerEndpoint {}",endpoint);
      return messagingService.sendInOutMessageGeneric(propertyMap,headerMap,null,"seda:NeedProtocol.out.connect");



  }

    public void open(final Connection connection, final Model content) throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
        logger.info(MessageFormat.format("need-facing: OPEN called for connection {0}", connection));

       // Connection con = DataAccessUtils.loadConnection(connectionRepository, connection);
        Map headerMap = new HashMap<String, String>();
        String endpoint = null;
        headerMap.put("protocol","NeedProtocol");
        //remoteConnectionURI is the connectionURI on the other node.
        headerMap.put("connectionURI", connection.getRemoteConnectionURI().toString()) ;
        headerMap.put("content", RdfUtils.toString(content));
        headerMap.put("methodName","open");


        try {

            endpoint = needProtocolActiveMQService.getCamelEndpointForConnection(connection.getConnectionURI(), "seda:NeedProtocol.out.open");
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        headerMap.put("remoteBrokerEndpoint", endpoint);
        messagingService.sendInOnlyMessage(null,headerMap,null, "seda:NeedProtocol.out.open" );
    }


  public void close(final Connection connection, final Model content) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
      logger.info("need-facing: CLOSE called for connection {}", connection);
      //Connection con = DataAccessUtils.loadConnection(connectionRepository, connection);
      Map headerMap = new HashMap<String, String>();
      String endpoint = null;
      headerMap.put("protocol","NeedProtocol");
      //remoteConnectionURI is the connectionURI on the other node.
      headerMap.put("connectionURI", connection.getRemoteConnectionURI().toString()) ;
      headerMap.put("content", RdfUtils.toString(content));
      headerMap.put("methodName","close");

      try {

          endpoint = needProtocolActiveMQService.getCamelEndpointForConnection(connection.getConnectionURI(), "seda:NeedProtocol.out.close");
      } catch (Exception e) {
          e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
      }
      headerMap.put("remoteBrokerEndpoint", endpoint);

      messagingService.sendInOnlyMessage(null,headerMap,null, "seda:NeedProtocol.out.close" ) ;

  }


  public void textMessage(final Connection connection, final Model message) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
      logger.info("need-facing: SEND_TEXT_MESSAGE called for connection {} with message {}", connection, message);
      //Connection con = DataAccessUtils.loadConnection(connectionRepository, connection);
      Map headerMap = new HashMap<String, String>();
      String endpoint = null;
      String messageConvert = RdfUtils.toString(message);
      headerMap.put("protocol","NeedProtocol");
      //remoteConnectionURI is the connectionURI on the other node.
      headerMap.put("connectionURI", connection.getRemoteConnectionURI().toString()) ;
      headerMap.put("content", messageConvert);
      headerMap.put("methodName","textMessage");

      try {

          endpoint = needProtocolActiveMQService.getCamelEndpointForConnection(connection.getConnectionURI(), "seda:NeedProtocol.out.textMessage");
          logger.info("retrieved endpoint for connection. Endpoint: {}", endpoint);
      } catch (Exception e) {
          e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
      }
      headerMap.put("remoteBrokerEndpoint", endpoint);
      logger.info("NeedProtocolNeedClientImpl: sending text message to remoteBrokerEndpoint: {}",endpoint);
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

public void setNeedProtocolActiveMQService(NeedProtocolActiveMQService needProtocolActiveMQService) {
        this.needProtocolActiveMQService = needProtocolActiveMQService;
}

        }
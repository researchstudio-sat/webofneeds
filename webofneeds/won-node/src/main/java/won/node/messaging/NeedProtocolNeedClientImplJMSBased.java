/*
 * Copyright 2012  Research Studios Austria Forschungsges.m.b.H.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package won.node.messaging;

import com.google.common.util.concurrent.ListenableFuture;
import com.hp.hpl.jena.rdf.model.Model;
import org.apache.camel.CamelContext;
import org.apache.camel.CamelContextAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import won.protocol.jms.*;
import won.protocol.model.Connection;
import won.protocol.need.NeedProtocolNeedClientSide;
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

    private String connectStartingEndpoint;
    private String openStartingEndpoint;
    private String closeStartingEndpoint;
    private String textMessageStartingEndpoint;

    @Autowired
    private NeedProtocolCommunicationService protocolCommunicationService;

    //TODO: debugging needed. when a established connection is closed then reconnected, both connections are in state "request sent"
  @Override
  public ListenableFuture<URI> connect(final URI needUri, final URI otherNeedUri, final URI otherConnectionUri, final Model content) throws Exception {



      //TODO: when shall be the remote won node unregistered?
      //TODO; shall be checked if the endpoint for the remote won node already exists. configuring remote endpoints for each message is inefficient

      CamelConfiguration camelConfiguration = protocolCommunicationService.configureCamelEndpoint(otherNeedUri,needUri,connectStartingEndpoint);

      Map<String, String> headerMap = new HashMap<>();
      headerMap.put("protocol","NeedProtocol");
      headerMap.put("needURI", needUri.toString()) ;
      headerMap.put("otherNeedURI", otherNeedUri.toString());
      headerMap.put("otherConnectionURI", otherConnectionUri.toString()) ;
      headerMap.put("content",RdfUtils.toString(content));
      headerMap.put("methodName","connect");
      headerMap.put("remoteBrokerEndpoint", camelConfiguration.getEndpoint());
      logger.info("sending connect message to remoteBrokerEndpoint {}",camelConfiguration.getEndpoint());

      return messagingService.sendInOutMessageGeneric(null,headerMap,null,connectStartingEndpoint);
  }

    public void open(final Connection connection, final Model content) throws Exception {
        logger.info(MessageFormat.format("need-facing: OPEN called for connection {0}", connection));


        CamelConfiguration camelConfiguration = protocolCommunicationService.configureCamelEndpoint(connection.getNeedURI(),connection.getRemoteNeedURI(),openStartingEndpoint);

        Map headerMap = new HashMap<String, String>();
        headerMap.put("protocol","NeedProtocol");
        headerMap.put("connectionURI", connection.getRemoteConnectionURI().toString()) ;
        headerMap.put("content", RdfUtils.toString(content));
        headerMap.put("methodName","open");
        headerMap.put("remoteBrokerEndpoint", camelConfiguration.getEndpoint());

        messagingService.sendInOnlyMessage(null,headerMap,null, openStartingEndpoint );
    }


  public void close(final Connection connection, final Model content) throws Exception {
      logger.info("need-facing: CLOSE called for connection {}", connection);

      CamelConfiguration camelConfiguration = protocolCommunicationService.configureCamelEndpoint(connection.getNeedURI(),connection.getRemoteNeedURI(),closeStartingEndpoint);

      Map headerMap = new HashMap<String, String>();
      headerMap.put("protocol","NeedProtocol");
      headerMap.put("connectionURI", connection.getRemoteConnectionURI().toString()) ;
      headerMap.put("content", RdfUtils.toString(content));
      headerMap.put("methodName","close");
      headerMap.put("remoteBrokerEndpoint", camelConfiguration.getEndpoint());

      messagingService.sendInOnlyMessage(null,headerMap,null, closeStartingEndpoint ) ;

  }


  public void textMessage(final Connection connection, final Model message) throws Exception {
      logger.info("need-facing: SEND_TEXT_MESSAGE called for connection {} with message {}", connection, message);
      String messageConvert = RdfUtils.toString(message);

      CamelConfiguration camelConfiguration = protocolCommunicationService.
          configureCamelEndpoint(
              connection.getNeedURI(),
              connection.getRemoteNeedURI(),
              textMessageStartingEndpoint);
      logger.info("retrieved endpoint for connection. Endpoint: {}", camelConfiguration.getEndpoint());

      Map headerMap = new HashMap<String, String>();
      headerMap.put("protocol","NeedProtocol");
      headerMap.put("connectionURI", connection.getRemoteConnectionURI().toString()) ;
      headerMap.put("content", messageConvert);
      headerMap.put("methodName","textMessage");
      headerMap.put("remoteBrokerEndpoint", camelConfiguration.getEndpoint());
      logger.info("NeedProtocolNeedClientImpl: sending text message to remoteBrokerEndpoint: {}",camelConfiguration.getEndpoint());
      messagingService.sendInOnlyMessage(null,headerMap,null, textMessageStartingEndpoint );

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

    public void setConnectStartingEndpoint(String connectStartingEndpoint) {
        this.connectStartingEndpoint = connectStartingEndpoint;
    }


    public void setOpenStartingEndpoint(String openStartingEndpoint) {
        this.openStartingEndpoint = openStartingEndpoint;
    }

    public void setCloseStartingEndpoint(String closeStartingEndpoint) {
        this.closeStartingEndpoint = closeStartingEndpoint;
    }

    public void setTextMessageStartingEndpoint(String textMessageStartingEndpoint) {
        this.textMessageStartingEndpoint = textMessageStartingEndpoint;
    }

}
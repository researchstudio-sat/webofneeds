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

package won.owner.protocol.impl;
import javax.jms.Destination;

import com.hp.hpl.jena.rdf.model.Model;
import org.apache.camel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import won.owner.routes.OwnerApplicationListenerRouteBuilder;
import won.owner.ws.OwnerProtocolNeedClientFactory;
import won.protocol.exception.*;
import won.protocol.jms.MessagingService;
import won.protocol.model.WonNode;
import won.protocol.repository.WonNodeRepository;
import won.protocol.util.PropertiesUtil;
import won.protocol.util.RdfUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * User: LEIH-NB
 * Date: 17.10.13
 */
public class OwnerProtocolNeedServiceClientJMSBased implements InitializingBean,OwnerProtocolNeedServiceClientSide {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private MessageProducer messageProducer;
    private OwnerProtocolNeedClientFactory clientFactory;
    private ProducerTemplate producerTemplate;
    private Destination destination;
    private org.springframework.jms.connection.CachingConnectionFactory con;
    private CamelContext camelContext;
    private MessagingService messagingService;
    private PropertiesUtil propertiesUtil;
    private URI defaultNodeURI;
    @Autowired
    private WonNodeRepository wonNodeRepository;
    private OwnerApplicationListenerRouteBuilder ownerApplicationListenerRouteBuilder;

    @Override
    public void afterPropertiesSet() throws Exception {
        //todo: consider the case where wonNodeURI is specified explicitly

        List<WonNode> wonNodeList = wonNodeRepository.findByWonNodeURI(defaultNodeURI);
        if (wonNodeList.size()>0){
            String ownerApplicationID = wonNodeList.get(0).getOwnerApplicationID();
            Map headerMap = new HashMap<String, Object>();
            headerMap.put("ownerApplicationID", ownerApplicationID) ;
            Map properties = new HashMap();
            properties.put("methodName","connect");
            Future<List<String>> futureResults =messagingService.sendInOutMessageGeneric(properties, headerMap, null, "outgoingMessages");
            List<String> endpoints = futureResults.get();
            ownerApplicationListenerRouteBuilder.setEndpoints(endpoints);
        }


    }

    @Override
    public Future<URI> connect(URI needURI, URI otherNeedURI, Model content) throws NoSuchNeedException, IllegalMessageForNeedStateException, ConnectionAlreadyExistsException {

        Map headerMap = new HashMap<String, Object>();
        headerMap.put("needURI", needURI.toString()) ;
        headerMap.put("otherNeedURI", otherNeedURI.toString());
        headerMap.put("content",RdfUtils.toString(content));
        Map properties = new HashMap();
        properties.put("methodName","connect");
        return messagingService.sendInOutMessage(properties,headerMap,null, "outgoingMessages");
    }

    @Override
    public void deactivate(URI needURI) throws NoSuchNeedException {

        Map headerMap = new HashMap();
        headerMap.put("needURI",needURI.toString());
        Map properties = new HashMap();
        properties.put("methodName","deactivate");
        messagingService.sendInOnlyMessage(properties,headerMap,null,"outgoingMessages" );
        logger.info("sending activate message: "+ needURI.toString());
    }

    @Override
    public void activate(URI needURI) throws NoSuchNeedException {

        Map headerMap = new HashMap();
        headerMap.put("needURI",needURI.toString());
        Map properties = new HashMap();
        properties.put("methodName","activate");
        messagingService.sendInOnlyMessage(properties,headerMap,null,"outgoingMessages" );
        logger.info("sending activate message: "+ needURI.toString());

    }

    @Override
    public Future<String> register() {

        logger.info("sending register message");

        return messagingService.sendInOutMessageForString("register", null, null, "outgoingMessages");


    }


    @Override
    public Future<URI> createNeed(URI ownerURI, Model content, boolean activate) throws IllegalNeedContentException, IOException, ExecutionException, InterruptedException, URISyntaxException {

        return createNeed(ownerURI, content, activate,defaultNodeURI);
    }

    @Override
    public void textMessage(URI connectionURI, String message) throws NoSuchConnectionException, IllegalMessageForConnectionStateException {

        Map headerMap = new HashMap();
        headerMap.put("connectionURI",connectionURI.toString());
        headerMap.put("message",message);
        Map properties = new HashMap();
        properties.put("methodName","textMessage");
        messagingService.sendInOnlyMessage(properties,headerMap,null,"outgoingMessages" );
        logger.info("sending text message: ");

    }

    @Override
    public void close(URI connectionURI, Model content) throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
        Map headerMap = new HashMap();
        headerMap.put("connectionURI",connectionURI.toString());
        headerMap.put("content",RdfUtils.toString(content));
        Map properties = new HashMap();
        properties.put("methodName","close");
        messagingService.sendInOnlyMessage(properties,headerMap,null,"outgoingMessages");
        logger.info("sending close message: ");
    }

    @Override
    public void open(URI connectionURI, Model content) throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
        Map headerMap = new HashMap();
        headerMap.put("connectionURI",connectionURI.toString());
        headerMap.put("content",RdfUtils.toString(content));
        Map properties = new HashMap();
        properties.put("methodName","open");
        messagingService.sendInOnlyMessage(properties,headerMap,null, "outgoingMessages");
        logger.info("sending open message: ");

    }

    @Override
    public Future<URI> createNeed(URI ownerURI, Model content, boolean activate, URI wonNodeURI) throws IllegalNeedContentException, IOException, ExecutionException, InterruptedException, URISyntaxException {
        Map headerMap = new HashMap();

        if(wonNodeURI == null)
            wonNodeURI=defaultNodeURI;
        List<WonNode> wonNodeList = wonNodeRepository.findByWonNodeURI(wonNodeURI);
        String ownerApplicationId;
        if(wonNodeList.size()==0)  {
            Future<String> futureResults = register();
            ownerApplicationId = futureResults.get();

            logger.info("registered ownerappID: "+ownerApplicationId);
            WonNode wonNode = new WonNode();
            wonNode.setOwnerApplicationID(ownerApplicationId);
            wonNode.setWonNodeURI(wonNodeURI);
            wonNode = wonNodeRepository.saveAndFlush(wonNode);

        }
        else{
            ownerApplicationId = wonNodeList.get(0).getOwnerApplicationID();

            logger.info("existing ownerApplicationId: "+ownerApplicationId);
        }

        headerMap.put("ownerApplicationID",ownerApplicationId);
        headerMap.put("ownerUri", ownerURI.toString());
        headerMap.put("model", RdfUtils.toString(content));
        headerMap.put("activate",activate);
        Map properties = new HashMap();
        properties.put("methodName","createNeed");
        return messagingService.sendInOutMessage(properties,headerMap,null,"outgoingMessages" );

    }


    public void setClientFactory(OwnerProtocolNeedClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    public void setMessageProducer(MessageProducer messageProducer) {
        this.messageProducer = messageProducer;
    }

    public void setProducerTemplate(ProducerTemplate producerTemplate) {

        this.producerTemplate = producerTemplate;
    }



    public void setMessagingService(MessagingService messagingService) {
        this.messagingService = messagingService;
    }

    public void setPropertiesUtil(PropertiesUtil propertiesUtil) {
        this.propertiesUtil = propertiesUtil;
    }

    public void setDefaultNodeURI(URI defaultNodeURI) {
        this.defaultNodeURI = defaultNodeURI;
    }

    public void setOwnerApplicationListenerRouteBuilder(OwnerApplicationListenerRouteBuilder ownerApplicationListenerRouteBuilder) {
        this.ownerApplicationListenerRouteBuilder = ownerApplicationListenerRouteBuilder;
    }
}

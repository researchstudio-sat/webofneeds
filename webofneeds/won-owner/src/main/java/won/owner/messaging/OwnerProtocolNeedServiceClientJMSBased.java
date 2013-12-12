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

package won.owner.messaging;
import javax.jms.Destination;

import com.hp.hpl.jena.rdf.model.Model;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.Connection;
import org.apache.activemq.camel.component.ActiveMQComponent;
import org.apache.camel.*;
import org.apache.xbean.spring.context.ClassPathXmlApplicationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.jms.connection.CachingConnectionFactory;
import won.owner.protocol.impl.MessageProducer;
import won.owner.camel.routes.OwnerApplicationListenerRouteBuilder;
import won.owner.ws.OwnerProtocolNeedClientFactory;
import won.protocol.exception.*;
import won.protocol.jms.MessagingService;
import won.protocol.jms.OwnerProtocolActiveMQService;
import won.protocol.model.WonNode;
import won.protocol.owner.OwnerProtocolNeedServiceClientSide;
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

public class OwnerProtocolNeedServiceClientJMSBased implements ApplicationContextAware,ApplicationListener<ContextRefreshedEvent>,OwnerProtocolNeedServiceClientSide,CamelContextAware {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private boolean onApplicationRun = false;
    private MessageProducer messageProducer;
    private OwnerProtocolNeedClientFactory clientFactory;
    private ProducerTemplate producerTemplate;
    private Destination destination;
    private org.springframework.jms.connection.CachingConnectionFactory con;
    private CamelContext camelContext;
    private MessagingService messagingService;
    private PropertiesUtil propertiesUtil;
    private URI defaultNodeURI;
    private ApplicationContext ownerApplicationContext;
    //todo: make this configurable
    private String startingEndpoint ="seda:outgoingMessages";

    private OwnerProtocolActiveMQServiceImpl ownerProtocolActiveMQService;

    @Autowired
    private WonNodeRepository wonNodeRepository;

    /**
     * The owner application connects to the default won node upon initalization using ownerProtocolActiveMQService
     * and gets the endoint of the won node broker.
     *
     * @param contextRefreshedEvent
     */
    //TODO: this is called multiple times during startup, which is not ideal
    //we tried implementing applicationContextAware, but that didn't work. Hoping for an epiphany
    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        //todo: consider the case where wonNodeURI is specified explicitly
        if (!onApplicationRun){
            logger.info("DEFAULTNODE: "+defaultNodeURI);
            List<WonNode> wonNodeList = wonNodeRepository.findByWonNodeURI(defaultNodeURI);
            String ownerApplicationID="";
            URI brokerURI = null;
            String remoteEndpoint = null;
            // String startingEndpoint = null;

            if (wonNodeList.size()>0){
                ownerApplicationID = wonNodeList.get(0).getOwnerApplicationID();
                //TODO: it may be that we have an id, but the node has forgotten it... in that case, our code will fail
            } else{
                try {
                    brokerURI = ownerProtocolActiveMQService.configureCamelEndpointForNodeURI(defaultNodeURI,"seda:outgoingMessages");
                    //todo: revisit this part of code for case completeness
                    //todo: this code is activemq specific.. shall be avoided.

                    ActiveMQConnectionFactory activemqConnectionFactory = (ActiveMQConnectionFactory) ownerApplicationContext.getBean("activemqConnectionFactory");
                    logger.info("before setting BrokerURI: "+activemqConnectionFactory.getBrokerURL());
                    activemqConnectionFactory.setBrokerURL(brokerURI.toString()+"?useLocalHost=false");
                    ActiveMQComponent activeMQComponent = (ActiveMQComponent) ownerApplicationContext.getBean("activemq");
                    activeMQComponent.setBrokerURL(brokerURI.toString()+"?useLocalHost=false");


               //     logger.info(activeMQComponent.getConfiguration().getListenerConnectionFactory().);
                   //  javax.jms.Connection connection = activemqConnectionFactory.createConnection();
                   //  connection.start();
                    logger.info("after setting BrokerURI: " + activemqConnectionFactory.getBrokerURL());
                } catch (Exception e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
                remoteEndpoint = ownerProtocolActiveMQService.getEndpoint();
                logger.info("getting remoteEndpoint: "+remoteEndpoint);
                //   startingEndpoint = ownerProtocolActiveMQService.getStartingEndpoint();
                Future<String> futureResults = register(ownerProtocolActiveMQService.getEndpoint());
                ownerApplicationID = null;
                try {
                    ownerApplicationID = futureResults.get();
                } catch (InterruptedException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                } catch (ExecutionException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
                logger.info("registered ownerappID: "+ownerApplicationID);
                WonNode wonNode = new WonNode();
                wonNode.setOwnerApplicationID(ownerApplicationID);
                wonNode.setWonNodeURI(defaultNodeURI);
                try {
                    wonNode.setBrokerURI(brokerURI);
                } catch (Exception e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
                wonNode = wonNodeRepository.saveAndFlush(wonNode);
            }
            Map headerMap = new HashMap<String, Object>();
            headerMap.put("ownerApplicationID", ownerApplicationID) ;

            //todo: refactor to an own method getEndpoints()
            headerMap.put("methodName","getEndpoints");
            headerMap.put("remoteBrokerEndpoint",remoteEndpoint);
            Future<List<String>> futureResults =messagingService.sendInOutMessageGeneric(headerMap, headerMap, null, "seda:outgoingMessages");
            List<String> endpoints = null;
            try {
                endpoints = futureResults.get();
                OwnerApplicationListenerRouteBuilder ownerApplicationListenerRouteBuilder = new OwnerApplicationListenerRouteBuilder(camelContext, endpoints);
                camelContext.addRoutes(ownerApplicationListenerRouteBuilder);
            } catch (Exception e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
            //TODO: some checks needed to assure that the application is configured correctly.
            onApplicationRun = true;
        }

    }

    @Override
    public Future<URI> connect(URI needURI, URI otherNeedURI, Model content) throws NoSuchNeedException, IllegalMessageForNeedStateException, ConnectionAlreadyExistsException {

        Map headerMap = new HashMap<String, Object>();
        headerMap.put("needURI", needURI.toString()) ;
        headerMap.put("otherNeedURI", otherNeedURI.toString());
        headerMap.put("content",RdfUtils.toString(content));

        headerMap.put("methodName","connect");

        try {
            ownerProtocolActiveMQService.configureCamelEndpointForNeed(needURI,startingEndpoint);
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        headerMap.put("remoteBrokerEndpoint",ownerProtocolActiveMQService.getEndpoint());
        return messagingService.sendInOutMessageGeneric(null,headerMap,null,startingEndpoint);
       // return messagingService.sendInOutMessage(null,headerMap,null, "outgoingMessages");
    }

    @Override
    public void deactivate(URI needURI) throws NoSuchNeedException {

        Map headerMap = new HashMap();
        headerMap.put("needURI",needURI.toString());

        headerMap.put("methodName","deactivate");

        try {
            ownerProtocolActiveMQService.configureCamelEndpointForNeed(needURI,startingEndpoint);
        } catch (Exception e) {    //todo: error handling needed
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        headerMap.put("remoteBrokerEndpoint",ownerProtocolActiveMQService.getEndpoint());

        messagingService.sendInOnlyMessage(null,headerMap,null,startingEndpoint );
        logger.info("sending activate message: "+ needURI.toString());
    }

    @Override
    public void activate(URI needURI) throws NoSuchNeedException {

        Map headerMap = new HashMap();
        headerMap.put("needURI",needURI.toString());

        headerMap.put("methodName","activate");

        try {
            ownerProtocolActiveMQService.configureCamelEndpointForNeed(needURI,startingEndpoint);
        } catch (Exception e) { //todo: error handling needed
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        headerMap.put("remoteBrokerEndpoint",ownerProtocolActiveMQService.getEndpoint());

        messagingService.sendInOnlyMessage(null,headerMap,null,startingEndpoint );
        logger.info("sending activate message: "+ needURI.toString());

    }

    @Override
    public Future<String> register(String endpointURI) {

        logger.info("sending register message to remoteBrokerEndpoint {}",endpointURI);
        Map headerMap = new HashMap();
        headerMap.put("remoteBrokerEndpoint",endpointURI);
        headerMap.put("methodName","register");
        return messagingService.sendInOutMessageGeneric(null, headerMap,null,startingEndpoint);
       // return messagingService.sendInOutMessageForString("register", null, null, "outgoingMessages");


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

        headerMap.put("methodName","textMessage");

        try {
            ownerProtocolActiveMQService.configureCamelEndpointForConnection(connectionURI,startingEndpoint);
        } catch (Exception e) { //todo: error handling needed
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        headerMap.put("remoteBrokerEndpoint",ownerProtocolActiveMQService.getEndpoint());

        messagingService.sendInOnlyMessage(null,headerMap,null,startingEndpoint );
        logger.info("sending text message: ");

    }

    @Override
    public void close(URI connectionURI, Model content) throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
        Map headerMap = new HashMap();
        headerMap.put("connectionURI",connectionURI.toString());
        headerMap.put("content",RdfUtils.toString(content));
        headerMap.put("methodName","close");

        try {
            ownerProtocolActiveMQService.configureCamelEndpointForConnection(connectionURI,startingEndpoint);
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        headerMap.put("remoteBrokerEndpoint",ownerProtocolActiveMQService.getEndpoint());

        messagingService.sendInOnlyMessage(null,headerMap,null,startingEndpoint);
        logger.info("sending close message: ");
    }

    @Override
    public void open(URI connectionURI, Model content) throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
        Map headerMap = new HashMap();
        headerMap.put("connectionURI",connectionURI.toString());
        headerMap.put("content",RdfUtils.toString(content));
        headerMap.put("methodName","open");

        try {
            ownerProtocolActiveMQService.configureCamelEndpointForConnection(connectionURI,startingEndpoint);
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        headerMap.put("remoteBrokerEndpoint",ownerProtocolActiveMQService.getEndpoint());
        messagingService.sendInOnlyMessage(null,headerMap,null, startingEndpoint);
        logger.info("sending open message: ");

    }

    @Override
    public Future<URI> createNeed(URI ownerURI, Model content, boolean activate, URI wonNodeURI) throws IllegalNeedContentException, IOException, ExecutionException, InterruptedException, URISyntaxException {
        Map headerMap = new HashMap();

        headerMap.put("ownerUri", ownerURI.toString());
        headerMap.put("model", RdfUtils.toString(content));
        headerMap.put("activate",activate);

        headerMap.put("methodName","createNeed");

        if (content != null) {
            content.setNsPrefix("",ownerURI.toString());
        }
        /**
         * if wonNodeURI is not the default wonNodeURI, following steps shall be followed.
         *  1) new activeMQ connection to the remote broker shall be established.
         *  2) owner protocol queue name shall be retrieved from the node.
         *  3) owner application shall be registered on the node to get the ownerapplication id.
         *  4) wonNode shall be saved on the owner application.
         */

        if(wonNodeURI == null)
            wonNodeURI=defaultNodeURI;

        try {
            //TODO: make this thread-safe!
            ownerProtocolActiveMQService.configureCamelEndpointForNodeURI(wonNodeURI,startingEndpoint);
        } catch (Exception e) {  //TODO: error handling needed
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        List<WonNode> wonNodeList = wonNodeRepository.findByWonNodeURI(wonNodeURI);
        String ownerApplicationId;
        URI brokerURI = null;
        if(wonNodeList.size()==0)  {
            try {
                brokerURI = ownerProtocolActiveMQService.configureCamelEndpointForNodeURI(defaultNodeURI,"seda:outgoingMessages");
            } catch (Exception e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
            //todo: methods of ownerProtocolActiveMQService might have some concurrency issues. this problem will be resolved in the future, and this code here shall be revisited then.
            Future<String> futureResults = register(ownerProtocolActiveMQService.getEndpoint());
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
        headerMap.put("remoteBrokerEndpoint",ownerProtocolActiveMQService.getEndpoint());
        headerMap.put("ownerApplicationID",ownerApplicationId);
        return messagingService.sendInOutMessageGeneric(null, headerMap,null,startingEndpoint);
     //   return messagingService.sendInOutMessage(headerMap,headerMap,null,"outgoingMessages" );

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



    @Override
    public void setCamelContext(CamelContext camelContext) {
        this.camelContext = camelContext;
    }

    @Override
    public CamelContext getCamelContext() {
        return this.camelContext; //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setOwnerProtocolActiveMQService(OwnerProtocolActiveMQServiceImpl ownerProtocolActiveMQService) {
        this.ownerProtocolActiveMQService = ownerProtocolActiveMQService;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.ownerApplicationContext = applicationContext;
    }
}

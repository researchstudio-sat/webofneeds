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
import com.google.common.util.concurrent.ListenableFuture;
import com.hp.hpl.jena.rdf.model.Model;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.camel.CamelContext;
import org.apache.camel.CamelContextAware;
import org.apache.camel.ProducerTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import won.owner.camel.routes.OwnerApplicationListenerRouteBuilder;
import won.owner.protocol.impl.MessageProducer;
import won.owner.ws.OwnerProtocolNeedClientFactory;
import won.protocol.exception.*;
import won.protocol.jms.MessagingService;
import won.protocol.model.WonNode;
import won.protocol.owner.OwnerProtocolNeedServiceClientSide;
import won.protocol.repository.WonNodeRepository;
import won.protocol.util.PropertiesUtil;
import won.protocol.util.RdfUtils;

import javax.jms.Destination;
import java.net.URI;
import java.util.ArrayList;
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
    public static boolean brokerConnected = false;
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
    private List<OwnerApplicationListener> ownerApplicationListeners = new ArrayList<>();
    //todo: make this configurable
    private String startingEndpoint ="seda:outgoingMessages";

    private OwnerProtocolActiveMQServiceImpl ownerProtocolActiveMQService;

    @Autowired
    private WonNodeRepository wonNodeRepository;

    /**
     * The owner application calls the register() method node upon initalization to connect to the default won node
     *
     * @param contextRefreshedEvent
     */
    //TODO: this is called multiple times during startup, which is not ideal
    //we tried implementing applicationContextAware, but that didn't work. Hoping for an epiphany
    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        //todo: consider the case where wonNodeURI is specified explicitly
        if (!onApplicationRun){
            logger.info("registering owner application on application event");
            try {
                new Thread(){
                    @Override
                    public void run() {
                        try {
                            register(defaultNodeURI);
                        } catch (Exception e) {
                            logger.warn("Could not register with default won node {}", defaultNodeURI,e);
                        }
                    }
                }.start();
            } catch (Exception e) {
                logger.info("registering ownerapplication on the node {} failed",defaultNodeURI);

            }
            onApplicationRun = true;
        }

    }

    @Override
    public ListenableFuture<URI> connect(URI needURI, URI otherNeedURI, Model content) throws NoSuchNeedException, IllegalMessageForNeedStateException, ConnectionAlreadyExistsException {

        Map headerMap = new HashMap<String, Object>();
        headerMap.put("needURI", needURI.toString()) ;
        headerMap.put("otherNeedURI", otherNeedURI.toString());
        headerMap.put("content",RdfUtils.toString(content));

        headerMap.put("methodName","connect");
        String endpoint = null;
        try {
            endpoint = ownerProtocolActiveMQService.configureCamelEndpointForNeed(needURI,startingEndpoint);
        } catch (Exception e) {
            logger.debug("configuring camel endpoint for need failed");
        }
        headerMap.put("remoteBrokerEndpoint",endpoint);
        return messagingService.sendInOutMessageGeneric(null,headerMap,null,startingEndpoint);
    }

    @Override
    public void deactivate(URI needURI) throws NoSuchNeedException {

        Map headerMap = new HashMap();
        headerMap.put("needURI",needURI.toString());

        headerMap.put("methodName","deactivate");
        String endpoint = null;
        try {
            endpoint = ownerProtocolActiveMQService.configureCamelEndpointForNeed(needURI,startingEndpoint);
        } catch (Exception e) {    //todo: error handling needed
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        headerMap.put("remoteBrokerEndpoint",endpoint);

        messagingService.sendInOnlyMessage(null,headerMap,null,startingEndpoint );
        logger.info("sending activate message: " + needURI.toString());
    }

    @Override
    public void activate(URI needURI) throws NoSuchNeedException {

        Map headerMap = new HashMap();
        headerMap.put("needURI",needURI.toString());
        headerMap.put("methodName","activate");
        String endpoint = null;
        try {
            endpoint = ownerProtocolActiveMQService.configureCamelEndpointForNeed(needURI,startingEndpoint);
        } catch (Exception e) { //todo: error handling needed
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        headerMap.put("remoteBrokerEndpoint",endpoint);

        messagingService.sendInOnlyMessage(null,headerMap,null,startingEndpoint );
        logger.info("sending activate message: "+ needURI.toString());

    }

    /**
     * registers the owner application at a won node.
     *
     * @param wonNodeURI
     * @return
     * @throws Exception
     */
    @Override
    public synchronized String register(URI wonNodeURI) throws Exception {
        logger.info("WON NODE: "+wonNodeURI);
        List<WonNode> wonNodeList = wonNodeRepository.findByWonNodeURI(wonNodeURI);
        String ownerApplicationID=null;
        URI brokerURI = null;
        String remoteEndpoint = null;
        String brokerComponentName = null;
        // String startingEndpoint = null;

        /**
         * if won node list is bigger than 0, it means that there is already at least one established connection to a won node.-> use owner application id already stored.
         */
        logger.info("size of wonNodeList {}",wonNodeList.size());
        if (wonNodeList.size()>0){
            WonNode wonNode = wonNodeList.get(0);
            String startingComponent = wonNode.getStartingComponent();
            ownerApplicationID = wonNode.getOwnerApplicationID();
            remoteEndpoint = wonNode.getOwnerProtocolEndpoint();
            List<String> endpointList = new ArrayList<>();
            endpointList.add(remoteEndpoint);
            addActiveMQComponentForWonNode(wonNode);
            ActiveMQConnectionFactory activemqConnectionFactory = (ActiveMQConnectionFactory) ownerApplicationContext.getBean("activemqConnectionFactory");
            logger.info("before setting BrokerURI: "+activemqConnectionFactory.getBrokerURL());
            activemqConnectionFactory.setBrokerURL(wonNode.getBrokerURI().toString()+"?useLocalHost=false");
            logger.info("after setting BrokerURI: " + activemqConnectionFactory.getBrokerURL());
            //logger.info("adding route for endpoint {} and starting component {}",endpointList.size(),startingComponent);
            ownerProtocolActiveMQService.addRouteForEndpoint(camelContext,endpointList,startingComponent);
            //TODO: it may be that we have an id, but the node has forgotten it... in that case, our code will fail
        } else{
            //todo: revisit this part of code for case completeness
            //todo: this code is activemq specific.. shall be avoided.
            brokerURI = ownerProtocolActiveMQService.configureCamelEndpointForNodeURI(wonNodeURI,"seda:outgoingMessages");

            if (brokerURI==null)
                throw new BrokerConfigurationFailedException(wonNodeURI);

                //TODO: either use pooled connection or create new factory for every call.
                ActiveMQConnectionFactory activemqConnectionFactory = (ActiveMQConnectionFactory) ownerApplicationContext.getBean("activemqConnectionFactory");
                logger.info("before setting BrokerURI: "+activemqConnectionFactory.getBrokerURL());
                activemqConnectionFactory.setBrokerURL(brokerURI.toString()+"?useLocalHost=false");
                logger.info("after setting BrokerURI: " + activemqConnectionFactory.getBrokerURL());

            remoteEndpoint = ownerProtocolActiveMQService.getEndpoint(wonNodeURI);
            brokerComponentName = ownerProtocolActiveMQService.getBrokerComponentNameForWonNode(wonNodeURI);
            logger.info("getting remoteEndpoint: "+remoteEndpoint);
            logger.info("sending register message to remoteBrokerEndpoint {}",ownerProtocolActiveMQService.getEndpoint(wonNodeURI));
            Map headerMap = new HashMap();
            headerMap.put("remoteBrokerEndpoint",remoteEndpoint);
            headerMap.put("methodName","register");
            Future<String> futureResults = messagingService.sendInOutMessageGeneric(null, headerMap, null, startingEndpoint);

            ownerApplicationID = null;
            ownerApplicationID = futureResults.get();

            if (ownerApplicationID!=null)
                brokerConnected = true;
            logger.info("registered ownerappID: "+ownerApplicationID);
            WonNode wonNode = new WonNode();
            wonNode.setOwnerApplicationID(ownerApplicationID);
            wonNode.setOwnerProtocolEndpoint(remoteEndpoint);
            wonNode.setWonNodeURI(wonNodeURI);
            wonNode.setBrokerURI(brokerURI);
            wonNode.setBrokerComponent(brokerComponentName);
            wonNode.setStartingComponent(ownerProtocolActiveMQService.getStartingComponent(wonNodeURI));
            logger.info("setting starting component {}", wonNode.getStartingComponent());
            wonNode = wonNodeRepository.saveAndFlush(wonNode);
        }
        logger.info("configuring remoteEndpoint with ownerapplication id {} and remote endpoint {} ", ownerApplicationID, remoteEndpoint);
        configureRemoteEndpointsForOwnerApplication(ownerApplicationID, remoteEndpoint);
        return ownerApplicationID.toString();

    }
    private void addActiveMQComponentForWonNode(WonNode wonNode){
        //WonNode wonNode = wonNodeList.get(0);

        String remoteEndpoint = wonNode.getOwnerProtocolEndpoint();
        URI brokerURI = wonNode.getBrokerURI();
        String brokerComponentName = wonNode.getBrokerComponent();

        ownerProtocolActiveMQService.addCamelComponentForWonNodeBroker(brokerComponentName, wonNode.getWonNodeURI(), brokerURI);
    }
    private String configureRemoteEndpointsForOwnerApplication(String ownerApplicationID, String remoteEndpoint) throws CamelConfigurationFailedException, ExecutionException, InterruptedException {
        Map headerMap = new HashMap<String, Object>();
        headerMap.put("ownerApplicationID", ownerApplicationID) ;
        //todo: refactor to an own method getEndpoints()
        headerMap.put("methodName","getEndpoints");
        headerMap.put("remoteBrokerEndpoint",remoteEndpoint);
        Future<List<String>> futureResults =messagingService.sendInOutMessageGeneric(headerMap, headerMap, null, "seda:outgoingMessages");
        List<String> endpoints = null;
        endpoints = futureResults.get();
        logger.info("length of endpoints {}", endpoints.size());
        OwnerApplicationListenerRouteBuilder ownerApplicationListenerRouteBuilder = new OwnerApplicationListenerRouteBuilder(camelContext, endpoints);
        try {
            camelContext.addRoutes(ownerApplicationListenerRouteBuilder);
        } catch (Exception e) {
            logger.debug("adding route to camel context failed", e);
            throw new CamelConfigurationFailedException("adding route to camel context failed",e);
        }


        //TODO: some checks needed to assure that the application is configured correctly.

       return ownerApplicationID.toString();
       // return messagingService.sendInOutMessageForString("register", null, null, "outgoingMessages");


    }


    @Override
    public ListenableFuture<URI> createNeed(URI ownerURI, Model content, boolean activate) throws Exception {

        return createNeed(ownerURI, content, activate,defaultNodeURI);
    }

    @Override
    public void textMessage(URI connectionURI, Model message) throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
        String messageConvert = RdfUtils.toString(message);
        Map headerMap = new HashMap();
        headerMap.put("connectionURI",connectionURI.toString());
        headerMap.put("message",messageConvert);
        headerMap.put("methodName","textMessage");
        String endpoint = null;
        try {
            endpoint = ownerProtocolActiveMQService.configureCamelEndpointForConnection(connectionURI,startingEndpoint);
        } catch (Exception e) { //todo: error handling needed
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        headerMap.put("remoteBrokerEndpoint", endpoint);

        messagingService.sendInOnlyMessage(null, headerMap, null, startingEndpoint);

        logger.debug("sending text message: ");

    }

    @Override
    public void close(URI connectionURI, Model content) throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
        Map headerMap = new HashMap();
        headerMap.put("connectionURI",connectionURI.toString());
        headerMap.put("content",RdfUtils.toString(content));
        headerMap.put("methodName","close");
        String endpoint = null;
        try {
            endpoint = ownerProtocolActiveMQService.configureCamelEndpointForConnection(connectionURI,startingEndpoint);
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        headerMap.put("remoteBrokerEndpoint",endpoint);

        messagingService.sendInOnlyMessage(null,headerMap,null,startingEndpoint);
        logger.debug("sending close message: ");
    }

    @Override
    public void open(URI connectionURI, Model content) throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
        Map headerMap = new HashMap();
        headerMap.put("connectionURI",connectionURI.toString());
        headerMap.put("content",RdfUtils.toString(content));
        headerMap.put("methodName","open");
        String endpoint = null;
        try {
            endpoint = ownerProtocolActiveMQService.configureCamelEndpointForConnection(connectionURI,startingEndpoint);
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        headerMap.put("remoteBrokerEndpoint",endpoint);
        messagingService.sendInOnlyMessage(null,headerMap,null, startingEndpoint);
        logger.debug("sending open message: ");

    }

    @Override
    public ListenableFuture<URI> createNeed(URI ownerURI, Model content, boolean activate, URI wonNodeURI) throws Exception {
        Map headerMap = new HashMap();

        headerMap.put("ownerUri", ownerURI.toString());
        headerMap.put("model", RdfUtils.toString(content));
        headerMap.put("activate",activate);
        headerMap.put("methodName","createNeed");


        /**
         * if wonNodeURI is not the default wonNodeURI, following steps shall be followed.
         *  1) new activeMQ connection to the remote broker shall be established.
         *  2) owner protocol queue name shall be retrieved from the node.
         *  3) owner application shall be registered on the node to get the ownerapplication id.
         *  4) wonNode shall be saved on the owner application.
         */

        if(wonNodeURI == null)
            wonNodeURI=defaultNodeURI;

        List<WonNode> wonNodeList = wonNodeRepository.findByWonNodeURI(wonNodeURI);
        String ownerApplicationId;
        URI brokerURI = null;
        /**
         * if owner application is not connected to any won node, register owner application to the node with wonNodeURI.
         */

        if(wonNodeList.size()==0)  {

            //todo: methods of ownerProtocolActiveMQService might have some concurrency issues. this problem will be resolved in the future, and this code here shall be revisited then.
            String results = register(wonNodeURI);
            ownerApplicationId = results;

            logger.info("registered ownerappID: "+ownerApplicationId);
            WonNode wonNode = new WonNode();
            wonNode.setOwnerApplicationID(ownerApplicationId);
            wonNode.setWonNodeURI(wonNodeURI);
            wonNode.setOwnerProtocolEndpoint(ownerProtocolActiveMQService.getEndpoint(wonNodeURI));
            wonNode = wonNodeRepository.saveAndFlush(wonNode);
            wonNodeList = wonNodeRepository.findByWonNodeURI(wonNodeURI);

        }
        else{
            //todo refactor with register()
            //camelContext.getComponent()
            WonNode wonNode = wonNodeList.get(0);
            ownerApplicationId = wonNodeList.get(0).getOwnerApplicationID();
            addActiveMQComponentForWonNode(wonNodeList.get(0));
            String startingComponent = wonNode.getStartingComponent();
            String ownerApplicationID = wonNode.getOwnerApplicationID();
            String remoteEndpoint = wonNode.getOwnerProtocolEndpoint();
            List<String> endpointList = new ArrayList<>();
            endpointList.add(remoteEndpoint);
            ownerProtocolActiveMQService.addRouteForEndpoint(camelContext,endpointList,startingComponent);
            //ownerProtocolActiveMQService.configureCamelEndpointForNodeURI(wonNodeURI,"seda:outgoingMessages");
            logger.debug("existing ownerApplicationId: " + ownerApplicationId);
        }
        headerMap.put("remoteBrokerEndpoint",wonNodeList.get(0).getOwnerProtocolEndpoint());
        headerMap.put("ownerApplicationID",ownerApplicationId);
        return messagingService.sendInOutMessageGeneric(null, headerMap,null,startingEndpoint);

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

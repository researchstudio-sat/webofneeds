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
import org.apache.camel.CamelContext;
import org.apache.camel.CamelContextAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import won.owner.camel.routes.OwnerApplicationListenerRouteBuilder;
import won.protocol.exception.*;
import won.protocol.jms.MessagingService;
import won.protocol.model.WonNode;
import won.protocol.owner.OwnerProtocolNeedServiceClientSide;
import won.protocol.repository.WonNodeRepository;
import won.protocol.util.RdfUtils;

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
    private CamelContext camelContext;
    private MessagingService messagingService;
    private URI defaultNodeURI;
    private ApplicationContext ownerApplicationContext;
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

    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {

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

        Map<String, Object> headerMap = new HashMap<>();
        headerMap.put("needURI", needURI.toString()) ;
        headerMap.put("otherNeedURI", otherNeedURI.toString());
        headerMap.put("content",RdfUtils.toString(content));
        headerMap.put("methodName","connect");
        String endpoint = null;
        URI wonNodeURI = ownerProtocolActiveMQService.getOwnWonNodeUriWithNeed(needURI);
        List<WonNode> wonNodeList = wonNodeRepository.findByWonNodeURI(wonNodeURI);
        if (wonNodeList.size()>0){
            WonNode wonNode = wonNodeList.get(0);
            endpoint = wonNode.getOwnerProtocolEndpoint();
        }else{
            try {
                endpoint = ownerProtocolActiveMQService.configureCamelEndpointForNeed(needURI,startingEndpoint);
            } catch (Exception e) { //todo: error handling needed
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }

        headerMap.put("remoteBrokerEndpoint",endpoint);
        return messagingService.sendInOutMessageGeneric(null,headerMap,null,startingEndpoint);
    }

    @Override
    public void deactivate(URI needURI) throws NoSuchNeedException {

        Map<String,Object> headerMap = new HashMap<>();
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

        Map<String, Object> headerMap = new HashMap<>();
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
     * @param wonNodeURI the URI of the won node the owner application should be registered to
     * @return ownerApplicationId
     * @throws Exception
     */
    @Override
    public synchronized String register(URI wonNodeURI) throws Exception {

        logger.info("WON NODE: "+wonNodeURI);
        List<WonNode> wonNodeList = wonNodeRepository.findByWonNodeURI(wonNodeURI);
        String ownerApplicationID;
        URI brokerURI;
        String remoteEndpoint;
        String brokerComponentName;

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

            brokerComponentName = addActiveMQComponentForWonNode(wonNode);
            camelContext.getComponent(brokerComponentName).createEndpoint(remoteEndpoint);
            ownerProtocolActiveMQService.addRouteForEndpoint(camelContext,endpointList,startingComponent);
            //TODO: it may be that we have an id, but the node has forgotten it... in that case, our code will fail
        } else{
            //todo: revisit this part of code for case completeness
            //todo: this code is activemq specific.. shall be avoided.
            brokerURI = ownerProtocolActiveMQService.configureCamelEndpointForNodeURI(wonNodeURI,"seda:outgoingMessages");

            if (brokerURI==null)
                throw new BrokerConfigurationFailedException(wonNodeURI);

            remoteEndpoint = ownerProtocolActiveMQService.getEndpoint(wonNodeURI);
            brokerComponentName = ownerProtocolActiveMQService.getBrokerComponentNameForWonNode(wonNodeURI);
            logger.info("getting remoteEndpoint: "+remoteEndpoint);
            logger.info("sending register message to remoteBrokerEndpoint {}",ownerProtocolActiveMQService.getEndpoint(wonNodeURI));

            Map<String, Object> headerMap = new HashMap<>();
            headerMap.put("remoteBrokerEndpoint",remoteEndpoint);
            headerMap.put("methodName","register");
            Future<String> futureResults = messagingService.sendInOutMessageGeneric(null, headerMap, null, startingEndpoint);


            ownerApplicationID = futureResults.get();
            remoteEndpoint = ownerProtocolActiveMQService.replaceEndpointNameWithOwnerApplicationId(remoteEndpoint,ownerApplicationID);
            brokerComponentName = ownerProtocolActiveMQService.replaceComponentNameWithOwnerApplicationId(brokerComponentName,ownerApplicationID);


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
            wonNodeRepository.saveAndFlush(wonNode);
        }
        logger.info("configuring remoteEndpoint with ownerapplication id {} and remote endpoint {} ", ownerApplicationID, remoteEndpoint);
        configureRemoteEndpointsForOwnerApplication(ownerApplicationID, remoteEndpoint);
        return ownerApplicationID;

    }
    private String addActiveMQComponentForWonNode(WonNode wonNode){

        URI brokerURI = wonNode.getBrokerURI();
        String brokerComponentName = wonNode.getBrokerComponent();

        return ownerProtocolActiveMQService.addCamelComponentForWonNodeBroker(brokerComponentName, wonNode.getWonNodeURI(), brokerURI,wonNode.getOwnerApplicationID());
    }

    private String configureRemoteEndpointsForOwnerApplication(String ownerApplicationID, String remoteEndpoint) throws CamelConfigurationFailedException, ExecutionException, InterruptedException {
        Map<String, Object> headerMap = new HashMap<>();
        headerMap.put("ownerApplicationID", ownerApplicationID) ;
        headerMap.put("methodName","getEndpoints");
        headerMap.put("remoteBrokerEndpoint",remoteEndpoint);

        Future<List<String>> futureResults =messagingService.sendInOutMessageGeneric(headerMap, headerMap, null, "seda:outgoingMessages");
        List<String> endpoints = futureResults.get();

        logger.info("length of endpoints {}", endpoints.size());
        OwnerApplicationListenerRouteBuilder ownerApplicationListenerRouteBuilder = new OwnerApplicationListenerRouteBuilder(camelContext, endpoints);
        try {
            camelContext.addRoutes(ownerApplicationListenerRouteBuilder);
        } catch (Exception e) {
            logger.debug("adding route to camel context failed", e);
            throw new CamelConfigurationFailedException("adding route to camel context failed",e);
        }


        //TODO: some checks needed to assure that the application is configured correctly.
       //todo this method should return routes
       return ownerApplicationID;

    }


    @Override
    public ListenableFuture<URI> createNeed(URI ownerURI, Model content, boolean activate) throws Exception {

        return createNeed(ownerURI, content, activate,defaultNodeURI);
    }

    @Override
    public void textMessage(URI connectionURI, Model message) throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
        String messageConvert = RdfUtils.toString(message);

        Map<String, Object> headerMap = new HashMap<>();
        headerMap.put("connectionURI",connectionURI.toString());
        headerMap.put("message",messageConvert);
        headerMap.put("methodName","textMessage");

        String endpoint = null;
        URI wonNodeURI = ownerProtocolActiveMQService.getOwnWonNodeUriWithConnection(connectionURI);
        List<WonNode> wonNodeList = wonNodeRepository.findByWonNodeURI(wonNodeURI);
        if (wonNodeList.size()>0){
            WonNode wonNode = wonNodeList.get(0);
            endpoint = wonNode.getOwnerProtocolEndpoint();
        }else{
            try {

                endpoint = ownerProtocolActiveMQService.configureCamelEndpointForConnection(connectionURI,startingEndpoint);
            } catch (Exception e) { //todo: error handling needed
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }

        headerMap.put("remoteBrokerEndpoint", endpoint);

        messagingService.sendInOnlyMessage(null, headerMap, null, startingEndpoint);

        logger.debug("sending text message: ");

    }

    @Override
    public void close(URI connectionURI, Model content) throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
        Map<String, Object> headerMap = new HashMap<>();
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
        Map<String, Object> headerMap = new HashMap<>();
        headerMap.put("connectionURI",connectionURI.toString());
        headerMap.put("content",RdfUtils.toString(content));
        headerMap.put("methodName","open");
        String endpoint = null;
        URI wonNodeURI = ownerProtocolActiveMQService.getOwnWonNodeUriWithConnection(connectionURI);
        List<WonNode> wonNodeList = wonNodeRepository.findByWonNodeURI(wonNodeURI);
        if (wonNodeList.size()>0){
            WonNode wonNode = wonNodeList.get(0);
            endpoint = wonNode.getOwnerProtocolEndpoint();
        }else{
            try {

                endpoint = ownerProtocolActiveMQService.configureCamelEndpointForConnection(connectionURI,startingEndpoint);
            } catch (Exception e) { //todo: error handling needed
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }

        headerMap.put("remoteBrokerEndpoint",endpoint);
        messagingService.sendInOnlyMessage(null,headerMap,null, startingEndpoint);
        logger.debug("sending open message: ");

    }

    @Override
    public ListenableFuture<URI> createNeed(URI ownerURI, Model content, boolean activate, URI wonNodeURI) throws Exception {
        Map<String, Object> headerMap = new HashMap<>();
        camelContext.getShutdownStrategy().setSuppressLoggingOnTimeout(true);

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
            String startingComponent = wonNode.getStartingComponent();
            ownerApplicationId = wonNodeList.get(0).getOwnerApplicationID();
            if (camelContext.getComponent(wonNodeList.get(0).getBrokerComponent())==null){
                String brokerComponentName = addActiveMQComponentForWonNode(wonNodeList.get(0));
                camelContext.getComponent(brokerComponentName).createEndpoint(wonNode.getOwnerProtocolEndpoint());
            }

            String remoteEndpoint = wonNode.getOwnerProtocolEndpoint();
            List<String> endpointList = new ArrayList<>();
            endpointList.add(remoteEndpoint);
            if(camelContext.getRoute(startingComponent)==null)
                ownerProtocolActiveMQService.addRouteForEndpoint(camelContext,endpointList,startingComponent);
            logger.debug("existing ownerApplicationId: " + ownerApplicationId);
        }
        headerMap.put("remoteBrokerEndpoint",wonNodeList.get(0).getOwnerProtocolEndpoint());
        headerMap.put("ownerApplicationID",ownerApplicationId);
        return messagingService.sendInOutMessageGeneric(null, headerMap,null,startingEndpoint);

    }


    public void setMessagingService(MessagingService messagingService) {
        this.messagingService = messagingService;
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

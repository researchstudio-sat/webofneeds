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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * User: LEIH-NB
 * Date: 17.10.13
 */

public class OwnerProtocolNeedServiceClientJMSBased implements ApplicationContextAware,ApplicationListener<ContextRefreshedEvent>,OwnerProtocolNeedServiceClientSide {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private boolean onApplicationRun = false;
    //private CamelContext camelContext;
    private MessagingService messagingService;
    private URI defaultNodeURI;
    private ApplicationContext ownerApplicationContext;
    //todo: make this configurable
    private String startingEndpoint ="seda:outgoingMessages";



    //private OwnerProtocolActiveMQServiceImpl ownerProtocolActiveMQService;
    @Autowired
    private OwnerProtocolCommunicationService ownerProtocolCommunicationService;


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
                            String ownerApplicationId = register(defaultNodeURI);
                            configureRemoteEndpointsForOwnerApplication(ownerApplicationId,ownerProtocolCommunicationService.getCamelConfigurator().getEndpoint(defaultNodeURI));

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
    public ListenableFuture<URI> connect(URI needURI, URI otherNeedURI, Model content) throws Exception {

        String endpoint = null;
        URI wonNodeUri = ownerProtocolCommunicationService.getWonNodeUri("connect",needURI);
        List<WonNode> wonNodeList = wonNodeRepository.findByWonNodeURI(wonNodeUri);

        CamelConfiguration camelConfiguration = ownerProtocolCommunicationService.configureCamelEndpoint(wonNodeUri,wonNodeList);
        endpoint = camelConfiguration.getEndpoint();

        Map<String, Object> headerMap = new HashMap<>();
        headerMap.put("needURI", needURI.toString()) ;
        headerMap.put("otherNeedURI", otherNeedURI.toString());
        headerMap.put("content",RdfUtils.toString(content));
        headerMap.put("methodName","connect");
        headerMap.put("remoteBrokerEndpoint", endpoint);

        return messagingService.sendInOutMessageGeneric(null,headerMap,null,startingEndpoint);
    }

    @Override
    public void deactivate(URI needURI) throws Exception {


        String endpoint = null;
        URI wonNodeUri = ownerProtocolCommunicationService.getWonNodeUri("deactivate", needURI);
        List<WonNode> wonNodeList = wonNodeRepository.findByWonNodeURI(wonNodeUri);

        CamelConfiguration camelConfiguration = ownerProtocolCommunicationService.configureCamelEndpoint(wonNodeUri,wonNodeList);
        endpoint = camelConfiguration.getEndpoint();

        Map<String,Object> headerMap = new HashMap<>();
        headerMap.put("needURI",needURI.toString());
        headerMap.put("methodName","deactivate");
        headerMap.put("remoteBrokerEndpoint",endpoint);

        messagingService.sendInOnlyMessage(null, headerMap, null, startingEndpoint);
        logger.info("sending deactivate message: " + needURI.toString());
    }

    @Override
    public void activate(URI needURI) throws Exception {


        String endpoint = null;
        URI wonNodeUri = ownerProtocolCommunicationService.getWonNodeUri("activate", needURI);
        List<WonNode> wonNodeList = wonNodeRepository.findByWonNodeURI(wonNodeUri);

        CamelConfiguration camelConfiguration = ownerProtocolCommunicationService.configureCamelEndpoint(wonNodeUri,wonNodeList);
        endpoint = camelConfiguration.getEndpoint();

        Map<String, Object> headerMap = new HashMap<>();
        headerMap.put("needURI",needURI.toString());
        headerMap.put("methodName","activate");
        headerMap.put("remoteBrokerEndpoint", endpoint);

        messagingService.sendInOnlyMessage(null, headerMap, null, startingEndpoint);
        logger.info("sending activate message: "+ needURI.toString());

    }
    /**
     * registers the owner application at a won node.
     *
     * @return ownerApplicationId
     * @throws Exception
     */
    public synchronized  String register(URI wonNodeURI) throws Exception {
        logger.info("WON NODE: "+wonNodeURI);
        List<WonNode> wonNodeList = wonNodeRepository.findByWonNodeURI(wonNodeURI);
        String ownerApplicationId;

        CamelConfiguration camelConfiguration = ownerProtocolCommunicationService.configureCamelEndpoint(wonNodeURI,wonNodeList);
        Map<String, Object> headerMap = new HashMap<>();
        headerMap.put("remoteBrokerEndpoint", camelConfiguration.getEndpoint());
        headerMap.put("methodName", "register");
        Future<String> futureResults = messagingService.sendInOutMessageGeneric(null, headerMap, null, startingEndpoint);

        ownerApplicationId = futureResults.get();

        camelConfiguration.setBrokerComponentName(ownerProtocolCommunicationService.replaceComponentNameWithOwnerApplicationId(camelConfiguration,ownerApplicationId));
        camelConfiguration.setEndpoint(ownerProtocolCommunicationService.replaceEndpointNameWithOwnerApplicationId(camelConfiguration,ownerApplicationId));

        logger.info("registered ownerappID: "+ownerApplicationId);
        WonNode wonNode = storeWonNode(ownerApplicationId,camelConfiguration,wonNodeURI);
        wonNodeRepository.saveAndFlush(wonNode);

        return ownerApplicationId;
    }
    public WonNode storeWonNode(String ownerApplicationId, CamelConfiguration camelConfiguration,URI wonNodeURI) throws NoSuchConnectionException {
        WonNode wonNode = new WonNode();
        wonNode.setOwnerApplicationID(ownerApplicationId);
        wonNode.setOwnerProtocolEndpoint(camelConfiguration.getEndpoint());
        wonNode.setWonNodeURI(wonNodeURI);
        wonNode.setBrokerURI(ownerProtocolCommunicationService.getBrokerUri(wonNodeURI));
        wonNode.setBrokerComponent(camelConfiguration.getBrokerComponentName());
        wonNode.setStartingComponent(ownerProtocolCommunicationService.getCamelConfigurator().getStartingEndpoint(wonNodeURI));
        logger.info("setting starting component {}", wonNode.getStartingComponent());
        return wonNode;

    }

    private void configureRemoteEndpointsForOwnerApplication(String ownerApplicationID, String remoteEndpoint) throws CamelConfigurationFailedException, ExecutionException, InterruptedException {
        Map<String, Object> headerMap = new HashMap<>();
        headerMap.put("ownerApplicationID", ownerApplicationID) ;
        headerMap.put("methodName","getEndpoints");
        headerMap.put("remoteBrokerEndpoint",remoteEndpoint);

        Future<List<String>> futureResults =messagingService.sendInOutMessageGeneric(headerMap, headerMap, null, "seda:outgoingMessages");
        List<String> endpoints = futureResults.get();

        ownerProtocolCommunicationService.getCamelConfigurator().addRemoteQueueListeners(endpoints);
        //TODO: some checks needed to assure that the application is configured correctly.
       //todo this method should return routes
    }

    @Override
    public ListenableFuture<URI> createNeed(URI ownerURI, Model content, boolean activate) throws Exception {

        return createNeed(ownerURI, content, activate,defaultNodeURI);
    }

    @Override
    public void textMessage(URI connectionURI, Model message) throws Exception {
        String messageConvert = RdfUtils.toString(message);

        String endpoint = null;
        URI wonNodeUri = ownerProtocolCommunicationService.getWonNodeUri("textMessage", connectionURI);
        List<WonNode> wonNodeList = wonNodeRepository.findByWonNodeURI(wonNodeUri);

        CamelConfiguration camelConfiguration = ownerProtocolCommunicationService.configureCamelEndpoint(wonNodeUri,wonNodeList);
        endpoint = camelConfiguration.getEndpoint();

        Map<String, Object> headerMap = new HashMap<>();
        headerMap.put("connectionURI",connectionURI.toString());
        headerMap.put("message",messageConvert);
        headerMap.put("methodName","textMessage");
        headerMap.put("remoteBrokerEndpoint", endpoint);
        messagingService.sendInOnlyMessage(null, headerMap, null, startingEndpoint);
        logger.debug("sending text message: ");
    }

    @Override
    public void close(URI connectionURI, Model content) throws Exception {

        String endpoint = null;
        URI wonNodeUri = ownerProtocolCommunicationService.getWonNodeUri("close", connectionURI);
        List<WonNode> wonNodeList = wonNodeRepository.findByWonNodeURI(wonNodeUri);

        CamelConfiguration camelConfiguration = ownerProtocolCommunicationService.configureCamelEndpoint(wonNodeUri,wonNodeList);
        endpoint = camelConfiguration.getEndpoint();

        Map<String, Object> headerMap = new HashMap<>();
        headerMap.put("connectionURI",connectionURI.toString());
        headerMap.put("content",RdfUtils.toString(content));
        headerMap.put("methodName","close");
        headerMap.put("remoteBrokerEndpoint", endpoint);

        messagingService.sendInOnlyMessage(null,headerMap,null,startingEndpoint);
        logger.debug("sending close message: ");
    }

    @Override
    public void open(URI connectionURI, Model content) throws Exception {

        String endpoint = null;
        URI wonNodeUri = ownerProtocolCommunicationService.getWonNodeUri("open", connectionURI);
        List<WonNode> wonNodeList = wonNodeRepository.findByWonNodeURI(wonNodeUri);

        CamelConfiguration camelConfiguration = ownerProtocolCommunicationService.configureCamelEndpoint(wonNodeUri,wonNodeList);
        endpoint = camelConfiguration.getEndpoint();

        Map<String, Object> headerMap = new HashMap<>();
        headerMap.put("connectionURI",connectionURI.toString());
        headerMap.put("content",RdfUtils.toString(content));
        headerMap.put("methodName","open");
        headerMap.put("remoteBrokerEndpoint", endpoint);
        messagingService.sendInOnlyMessage(null, headerMap, null, startingEndpoint);
        logger.debug("sending open message: ");

    }

    @Override
    public synchronized ListenableFuture<URI> createNeed(URI ownerURI, Model content, boolean activate, URI wonNodeUri) throws Exception {

        //camelContext.getShutdownStrategy().setSuppressLoggingOnTimeout(true);
        /**
         * if wonNodeURI is not the default wonNodeURI, following steps shall be followed.
         *  1) new activeMQ connection to the remote broker shall be established.
         *  2) owner protocol queue name shall be retrieved from the node.
         *  3) owner application shall be registered on the node to get the ownerapplication id.
         *  4) wonNode shall be saved on the owner application.
         */
        if(wonNodeUri == null)
            wonNodeUri =defaultNodeURI;

        List<WonNode> wonNodeList = wonNodeRepository.findByWonNodeURI(wonNodeUri);
        String ownerApplicationId;
        /**
         * if owner application is not connected to any won node, register owner application to the node with wonNodeURI.
         */
       // CamelConfiguration camelConfiguration = ownerProtocolCommunicationService.configureCamelEndpoint(wonNodeUri,wonNodeList);
        if(wonNodeList.size()==0)  {
            //todo: methods of ownerProtocolActiveMQService might have some concurrency issues. this problem will be resolved in the future, and this code here shall be revisited then.
            ownerApplicationId = register(wonNodeUri);
            configureRemoteEndpointsForOwnerApplication(ownerApplicationId,ownerProtocolCommunicationService.getCamelConfigurator().getEndpoint(wonNodeUri));
            logger.info("registered ownerappID: "+ownerApplicationId);
            wonNodeList = wonNodeRepository.findByWonNodeURI(wonNodeUri);
        }
        else{
            //todo refactor with register()
            //camelContext.getComponent()
            ownerApplicationId = wonNodeList.get(0).getOwnerApplicationID();
        }
        Map<String, Object> headerMap = new HashMap<>();
        headerMap.put("ownerUri", ownerURI.toString());
        headerMap.put("model", RdfUtils.toString(content));
        headerMap.put("activate",activate);
        headerMap.put("methodName","createNeed");
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
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.ownerApplicationContext = applicationContext;
    }


}

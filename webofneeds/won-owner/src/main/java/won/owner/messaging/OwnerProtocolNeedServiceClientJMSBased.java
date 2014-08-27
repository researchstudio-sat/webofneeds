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
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import won.protocol.exception.*;
import won.protocol.jms.CamelConfiguration;
import won.protocol.jms.MessagingService;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageEncoder;
import won.protocol.model.WonNode;
import won.protocol.owner.OwnerProtocolNeedServiceClientSide;
import won.protocol.repository.WonNodeRepository;
import won.protocol.util.DataAccessUtils;
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

public class OwnerProtocolNeedServiceClientJMSBased
        implements ApplicationContextAware,
        ApplicationListener<ContextRefreshedEvent>,
        OwnerProtocolNeedServiceClientSide {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private boolean onApplicationRun = false;
    //private CamelContext camelContext;

    private MessagingService messagingService;
    private URI defaultNodeURI;
    private ApplicationContext ownerApplicationContext;




    //todo: make this configurable
    private String startingEndpoint ;



    //private OwnerProtocolActiveMQServiceImpl ownerProtocolActiveMQService;
    @Autowired
    private OwnerProtocolCommunicationServiceImpl ownerProtocolCommunicationServiceImpl;


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
            logger.debug("registering owner application on application event");
            try {
                new Thread(){
                    @Override
                    public void run() {
                        try {
                            String ownerApplicationId = register(defaultNodeURI);
                            configureRemoteEndpointsForOwnerApplication(ownerApplicationId, ownerProtocolCommunicationServiceImpl.getProtocolCamelConfigurator().getEndpoint(defaultNodeURI));

                        } catch (Exception e) {
                            logger.warn("Could not register with default won node {}", defaultNodeURI,e);
                        }
                    }
                }.start();
            } catch (Exception e) {
                logger.warn("registering ownerapplication on the node {} failed",defaultNodeURI);
            }
            onApplicationRun = true;
        }
    }

    @Override
    public ListenableFuture<URI> connect(URI needURI, URI otherNeedURI, Model content, Dataset messageEvent)
            throws Exception {

        URI wonNodeUri = ownerProtocolCommunicationServiceImpl.getWonNodeUriWithNeedUri(needURI);
        logger.debug("OwnerProtocol: sending connect for need {} and other need {} call to node", needURI, otherNeedURI);
        CamelConfiguration camelConfiguration = ownerProtocolCommunicationServiceImpl.configureCamelEndpoint(wonNodeUri);

        Map<String, Object> headerMap = new HashMap<>();
        headerMap.put("needURI", needURI.toString()) ;
        headerMap.put("otherNeedURI", otherNeedURI.toString());
        headerMap.put("content",RdfUtils.toString(content));
        headerMap.put("methodName","connect");
        headerMap.put("remoteBrokerEndpoint",camelConfiguration.getEndpoint());
        headerMap.put("messageEvent", RdfUtils.toString(messageEvent));

        return messagingService.sendInOutMessageGeneric(null,headerMap,null,startingEndpoint);
    }

    @Override
    public void deactivate(URI needURI, Dataset messageEvent) throws Exception {

        URI wonNodeUri = ownerProtocolCommunicationServiceImpl.getWonNodeUriWithNeedUri(needURI);

        CamelConfiguration camelConfiguration = ownerProtocolCommunicationServiceImpl.configureCamelEndpoint(wonNodeUri);

        Map<String,Object> headerMap = new HashMap<>();
        headerMap.put("needURI",needURI.toString());
        headerMap.put("methodName","deactivate");
        headerMap.put("remoteBrokerEndpoint",camelConfiguration.getEndpoint());
        headerMap.put("messageEvent", RdfUtils.toString(messageEvent));

        messagingService.sendInOnlyMessage(null, headerMap, null, startingEndpoint);
        logger.debug("sending deactivate message: " + needURI.toString());
    }

    @Override
    public void activate(URI needURI, Dataset messageEvent) throws Exception {

        URI wonNodeUri = ownerProtocolCommunicationServiceImpl.getWonNodeUriWithNeedUri(needURI);
        List<WonNode> wonNodeList = wonNodeRepository.findByWonNodeURI(wonNodeUri);

        CamelConfiguration camelConfiguration = ownerProtocolCommunicationServiceImpl.configureCamelEndpoint(wonNodeUri);

        Map<String, Object> headerMap = new HashMap<>();
        headerMap.put("needURI",needURI.toString());
        headerMap.put("methodName","activate");
        headerMap.put("remoteBrokerEndpoint", camelConfiguration.getEndpoint());
        headerMap.put("messageEvent", RdfUtils.toString(messageEvent));

        messagingService.sendInOnlyMessage(null, headerMap, null, startingEndpoint);
        logger.debug("sending activate message: " + needURI.toString());

    }

  /**
     * registers the owner application at a won node.
     *
     * @return ownerApplicationId
     * @throws Exception
     */
    public synchronized  String register(URI wonNodeURI) throws Exception {
        logger.debug("WON NODE: "+wonNodeURI);

        CamelConfiguration camelConfiguration = ownerProtocolCommunicationServiceImpl.configureCamelEndpoint(wonNodeURI);

        Map<String, Object> headerMap = new HashMap<>();
        headerMap.put("remoteBrokerEndpoint", camelConfiguration.getEndpoint());
        headerMap.put("methodName", "register");
        Future<String> futureResults = messagingService.sendInOutMessageGeneric(null, headerMap, null, startingEndpoint);

        String ownerApplicationId = futureResults.get();

        camelConfiguration.setBrokerComponentName(ownerProtocolCommunicationServiceImpl.replaceComponentNameWithOwnerApplicationId(camelConfiguration, ownerApplicationId));
        camelConfiguration.setEndpoint(ownerProtocolCommunicationServiceImpl.replaceEndpointNameWithOwnerApplicationId(camelConfiguration,ownerApplicationId));
        //TODO: check if won node is already in the db
        logger.debug("registered ownerappID: "+ownerApplicationId);
        storeWonNode(ownerApplicationId,camelConfiguration,wonNodeURI);


        return ownerApplicationId;
    }

  /**
   * Stores the won node information, possibly overwriting existing data.
   * @param ownerApplicationId
   * @param camelConfiguration
   * @param wonNodeURI
   * @return
   * @throws NoSuchConnectionException
   */
    public WonNode storeWonNode(String ownerApplicationId, CamelConfiguration camelConfiguration,URI wonNodeURI) throws NoSuchConnectionException {
        WonNode wonNode = DataAccessUtils.loadWonNode(wonNodeRepository, wonNodeURI);
        if (wonNode == null) {
          wonNode = new WonNode();
        }
        wonNode.setOwnerApplicationID(ownerApplicationId);
        wonNode.setOwnerProtocolEndpoint(camelConfiguration.getEndpoint());
        wonNode.setWonNodeURI(wonNodeURI);
        wonNode.setBrokerURI(ownerProtocolCommunicationServiceImpl.getBrokerUri(wonNodeURI));
        wonNode.setBrokerComponent(camelConfiguration.getBrokerComponentName());
        wonNode.setStartingComponent(ownerProtocolCommunicationServiceImpl.getProtocolCamelConfigurator().getStartingEndpoint(wonNodeURI));
        wonNodeRepository.save(wonNode);
        logger.debug("setting starting component {}", wonNode.getStartingComponent());
        return wonNode;
    }

    private void configureRemoteEndpointsForOwnerApplication(String ownerApplicationID, String remoteEndpoint) throws CamelConfigurationFailedException, ExecutionException, InterruptedException {
        Map<String, Object> headerMap = new HashMap<>();
        headerMap.put("ownerApplicationID", ownerApplicationID) ;
        headerMap.put("methodName","getEndpoints");
        headerMap.put("remoteBrokerEndpoint",remoteEndpoint);

        Future<List<String>> futureResults =messagingService.sendInOutMessageGeneric(headerMap, headerMap, null, "seda:outgoingMessages");
        List<String> endpoints = futureResults.get();

        ownerProtocolCommunicationServiceImpl.getProtocolCamelConfigurator().addRemoteQueueListeners(endpoints,URI.create(remoteEndpoint));
        //TODO: some checks needed to assure that the application is configured correctly.
       //todo this method should return routes
    }

    @Override
    public ListenableFuture<URI> createNeed(Model content, boolean activate, Dataset messageEvent)
            throws Exception {

        return createNeed(content, activate, defaultNodeURI, messageEvent);
    }

    @Override
    public void sendMessage(URI connectionURI, Model message, Dataset messageEvent) throws Exception {
        String messageConvert = RdfUtils.toString(message);

        URI wonNodeUri = ownerProtocolCommunicationServiceImpl.getWonNodeUriWithConnectionUri(connectionURI);

        CamelConfiguration camelConfiguration = ownerProtocolCommunicationServiceImpl.configureCamelEndpoint(wonNodeUri);
        String endpoint = camelConfiguration.getEndpoint();

        Map<String, Object> headerMap = new HashMap<>();
        headerMap.put("connectionURI",connectionURI.toString());
        headerMap.put("message",messageConvert);
        headerMap.put("methodName","sendMessage");
        headerMap.put("remoteBrokerEndpoint", endpoint);
        headerMap.put("messageEvent", RdfUtils.toString(messageEvent));

        messagingService.sendInOnlyMessage(null, headerMap, null, startingEndpoint);
        logger.debug("sending text message: ");
    }

    @Override
    public void close(URI connectionURI, Model content, Dataset messageEvent) throws Exception {

        URI wonNodeUri = ownerProtocolCommunicationServiceImpl.getWonNodeUriWithConnectionUri(connectionURI);

        CamelConfiguration camelConfiguration = ownerProtocolCommunicationServiceImpl.configureCamelEndpoint(wonNodeUri);
        String endpoint = camelConfiguration.getEndpoint();

        Map<String, Object> headerMap = new HashMap<>();
        headerMap.put("connectionURI",connectionURI.toString());
        headerMap.put("content",RdfUtils.toString(content));
        headerMap.put("methodName","close");
        headerMap.put("remoteBrokerEndpoint", endpoint);
        headerMap.put("messageEvent", RdfUtils.toString(messageEvent));

        messagingService.sendInOnlyMessage(null,headerMap,null,startingEndpoint);
        logger.debug("sending close message: ");
    }

    @Override
    public void open(URI connectionURI, Model content, Dataset messageEvent) throws Exception {

        URI wonNodeUri = ownerProtocolCommunicationServiceImpl.getWonNodeUriWithConnectionUri(connectionURI);

        CamelConfiguration camelConfiguration = ownerProtocolCommunicationServiceImpl.configureCamelEndpoint(wonNodeUri);
        String endpoint = camelConfiguration.getEndpoint();

        Map<String, Object> headerMap = new HashMap<>();
        headerMap.put("connectionURI",connectionURI.toString());
        headerMap.put("content",RdfUtils.toString(content));
        headerMap.put("methodName","open");
        headerMap.put("remoteBrokerEndpoint", endpoint);
        headerMap.put("messageEvent", RdfUtils.toString(messageEvent));

        messagingService.sendInOnlyMessage(null, headerMap, null, startingEndpoint);
        logger.debug("sending open message: ");

    }

  public void sendWonMessage(WonMessage wonMessage) throws Exception
  {
    // ToDo (FS): change it to won node URI and create method in the MessageEvent class
    URI wonNodeUri = wonMessage.getMessageEvent().getSenderURI();

    CamelConfiguration camelConfiguration = ownerProtocolCommunicationServiceImpl.configureCamelEndpoint(wonNodeUri);
    String endpoint = camelConfiguration.getEndpoint();

    Map<String, Object> headerMap = new HashMap<>();
    // ToDo (FS): make Lang.x configurable
    headerMap.put("wonMessage", WonMessageEncoder.encode(wonMessage, Lang.TRIG));
    headerMap.put("methodName", "wonMessage");
    headerMap.put("remoteBrokerEndpoint", endpoint);

    messagingService.sendInOnlyMessage(null, headerMap, null, startingEndpoint);
    logger.debug("sending WonMessage: ");
  }

    @Override
    public synchronized ListenableFuture<URI> createNeed(
            Model content,
            boolean activate,
            URI wonNodeUri,
            Dataset messageEvent)
            throws Exception {

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
       // CamelConfiguration camelConfiguration = ownerProtocolCommunicationServiceImpl.configureCamelEndpoint(wonNodeUri,wonNodeList);
        if(wonNodeList.size()==0)  {
            //todo: methods of ownerProtocolActiveMQService might have some concurrency issues. this problem will be resolved in the future, and this code here shall be revisited then.
            ownerApplicationId = register(wonNodeUri);
            configureRemoteEndpointsForOwnerApplication(ownerApplicationId, ownerProtocolCommunicationServiceImpl.getProtocolCamelConfigurator().getEndpoint(wonNodeUri));
            logger.debug("registered ownerappID: "+ownerApplicationId);
            wonNodeList = wonNodeRepository.findByWonNodeURI(wonNodeUri);
        }
        else{
            //todo refactor with register()
          //TODO what happens with persistent WonNodeRepository? shouldn't camel configured again?
            //camelContext.getComponent()
            ownerApplicationId = wonNodeList.get(0).getOwnerApplicationID();
        }
        Map<String, Object> headerMap = new HashMap<>();
        headerMap.put("model", RdfUtils.toString(content));
        headerMap.put("activate",activate);
        headerMap.put("methodName","createNeed");
        headerMap.put("remoteBrokerEndpoint",wonNodeList.get(0).getOwnerProtocolEndpoint());
        headerMap.put("ownerApplicationID",ownerApplicationId);
        headerMap.put("messageEvent", RdfUtils.toString(messageEvent));

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

    public void setStartingEndpoint(String startingEndpoint) {
        this.startingEndpoint = startingEndpoint;
    }

}

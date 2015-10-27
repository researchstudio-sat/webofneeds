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

import org.apache.jena.riot.Lang;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import won.protocol.jms.MessagingService;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageEncoder;
import won.protocol.message.processor.impl.KeyForNewNeedAddingProcessor;
import won.protocol.message.processor.impl.SignatureAddingWonMessageProcessor;
import won.protocol.message.sender.WonMessageSender;
import won.protocol.model.WonNode;
import won.protocol.repository.WonNodeRepository;
import won.protocol.util.RdfUtils;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: LEIH-NB
 * Date: 17.10.13
 */

public class OwnerWonMessageSenderJMSBased
  implements ApplicationContextAware,
  ApplicationListener<ContextRefreshedEvent>,
        WonMessageSender
{

  private final Logger logger = LoggerFactory.getLogger(getClass());
  private boolean onApplicationRun = false;
  private MessagingService messagingService;
  private URI defaultNodeURI;
  private ApplicationContext ownerApplicationContext;

  //todo: make this configurable
  private String startingEndpoint;


  @Autowired
  private OwnerProtocolCommunicationServiceImpl ownerProtocolCommunicationServiceImpl;


  @Autowired
  private WonNodeRepository wonNodeRepository;

  @Autowired
  private SignatureAddingWonMessageProcessor signatureAddingProcessor ;

  @Autowired
  private KeyForNewNeedAddingProcessor needKeyGeneratorAndAdder;

  public void sendWonMessage(WonMessage wonMessage) {
    try {

      // TODO check if there is a better place for applying signing logic
      wonMessage = doSigningOnOwner(wonMessage);

      if (logger.isDebugEnabled()){
        logger.debug("sending this message: {}", RdfUtils.writeDatasetToString(wonMessage.getCompleteDataset(), Lang.TRIG));
      }

      // ToDo (FS): change it to won node URI and create method in the MessageEvent class
      URI wonNodeUri = wonMessage.getSenderNodeURI();

      if (wonNodeUri == null){
        //obtain the sender won node from the sender need
          throw new IllegalStateException("a message needs a SenderNodeUri otherwise we can't determine the won node " +
                                            "via which to send it");
      }

      ownerProtocolCommunicationServiceImpl.register(wonNodeUri, messagingService);

      List<WonNode> wonNodeList = wonNodeRepository.findByWonNodeURI(wonNodeUri);
      String ownerApplicationId = wonNodeList.get(0).getOwnerApplicationID();


      //String ep = camelConfiguration.getEndpoint()
      String ep = ownerProtocolCommunicationServiceImpl.getProtocolCamelConfigurator()
                                           .getEndpoint(wonNodeUri);
      Map<String, Object> headerMap = new HashMap<>();
      headerMap.put("ownerApplicationID", ownerApplicationId);
      headerMap.put("remoteBrokerEndpoint",ep);
      messagingService
              .sendInOnlyMessage(null, headerMap, WonMessageEncoder.encode(wonMessage, Lang.TRIG), startingEndpoint);

      //camelContext.getShutdownStrategy().setSuppressLoggingOnTimeout(true);
    } catch (Exception e){
      throw new RuntimeException("could not send message", e);
    }
  }

  //TODO: adding public keys and signing can be removed when it happens in the browser
  //in that case owner will have to sign only system messages, or in case it adds information to the message
  //TODO exceptions
  private WonMessage doSigningOnOwner(final WonMessage wonMessage)
    throws Exception {
    // add public key of the newly created need
    WonMessage outMessage = needKeyGeneratorAndAdder.process(wonMessage);
    // add signature:
    return signatureAddingProcessor.processOnBehalfOfNeed(outMessage);
  }


  /**
   * The owner application calls the register() method node upon initalization to connect to the default won node
   *
   * @param contextRefreshedEvent
   */

  @Override
  public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {

    if (!onApplicationRun) {
      logger.debug("registering owner application on application event");
      try {
        new Thread()
        {
          @Override
          public void run() {
            try {
              ownerProtocolCommunicationServiceImpl.register(defaultNodeURI, messagingService);

            } catch (Exception e) {
              logger.warn("Could not register with default won node {}", defaultNodeURI, e);
            }
          }
        }.start();
      } catch (Exception e) {
        logger.warn("registering ownerapplication on the node {} failed", defaultNodeURI);
      }
      onApplicationRun = true;
    }
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

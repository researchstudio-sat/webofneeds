/*
 * Copyright 2012 Research Studios Austria Forschungsges.m.b.H. Licensed under
 * the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable
 * law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package won.owner.messaging;

import org.apache.jena.riot.Lang;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import won.protocol.jms.MessagingService;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageEncoder;
import won.protocol.message.WonMessageUtils;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.message.processor.impl.KeyForNewAtomAddingProcessor;
import won.protocol.message.processor.impl.SignatureAddingWonMessageProcessor;
import won.protocol.message.sender.WonMessageSender;
import won.protocol.message.sender.exception.WonMessageSenderException;
import won.protocol.model.WonNode;
import won.protocol.repository.WonNodeRepository;
import won.protocol.util.LoggingUtils;
import won.protocol.util.RdfUtils;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: LEIH-NB Date: 17.10.13 Instance of this class receives events upon
 * which it tries to register at the default won node using JMS.
 */
public class OwnerWonMessageSenderJMSBased implements ApplicationListener<WonNodeRegistrationEvent>, WonMessageSender {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private boolean isDefaultWonNodeRegistered = false;
    private MessagingService messagingService;
    private URI defaultNodeURI;
    // todo: make this configurable
    private String startingEndpoint;
    @Autowired
    private OwnerProtocolCommunicationServiceImpl ownerProtocolCommunicationServiceImpl;
    @Autowired
    private WonNodeRepository wonNodeRepository;
    @Autowired
    private SignatureAddingWonMessageProcessor signatureAddingProcessor;
    @Autowired
    private KeyForNewAtomAddingProcessor atomKeyGeneratorAndAdder;

    @Override
    public void prepareAndSendMessage(WonMessage message) throws WonMessageSenderException {
        sendMessage(prepareMessage(message));
    }

    @Override
    public void prepareAndSendMessageOnBehalf(WonMessage message, URI webId) throws WonMessageSenderException {
        sendMessage(prepareMessageOnBehalf(message, webId));
    }

    @Override
    public WonMessage prepareMessage(WonMessage message) throws WonMessageSenderException {
        try {
            return signMessage(message);
        } catch (Exception e) {
            throw new WonMessageSenderException("Could not sign message or calculate its URI", e);
        }
    }

    @Override
    public WonMessage prepareMessageOnBehalf(WonMessage message, URI webId) throws WonMessageSenderException {
        try {
            return signMessage(message, webId);
        } catch (Exception e) {
            throw new WonMessageSenderException("Could not sign message or calculate its URI", e);
        }
    }

    /**
     * Signs the message, calculates its messageURI based on content and sends it.
     *
     * @param message
     * @return the updated, final message.
     * @throws WonMessageSenderException
     */
    public void sendMessage(WonMessage wonMessage) {
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("sending this message: {}",
                                RdfUtils.writeDatasetToString(wonMessage.getCompleteDataset(), Lang.TRIG));
            }
            URI msgUri = wonMessage.getMessageURIRequired();
            if (!WonMessageUtils.isValidMessageUri(msgUri)) {
                throw new WonMessageSenderException(
                                "Not a valid message uri: " + msgUri + ". Did you call prepareMessage(message) first?");
            }
            // ToDo (FS): change it to won node URI and create method in the MessageEvent
            // class
            URI wonNodeUri = wonMessage.getSenderNodeURI();
            if (wonNodeUri == null) {
                // obtain the sender won node from the sender atom
                throw new IllegalStateException(
                                "a message needs a SenderNodeUri otherwise we can't determine the won node "
                                                + "via which to send it");
            }
            // get the camel endpoint for talking to the WoN node
            String ep = ownerProtocolCommunicationServiceImpl.getProtocolCamelConfigurator().getEndpoint(wonNodeUri);
            if (ep == null) {
                // looks like we aren't registered - check if that's the case and register if
                // necessary
                if (!ownerProtocolCommunicationServiceImpl.isRegisteredWithWonNode(wonNodeUri)) {
                    ownerProtocolCommunicationServiceImpl.register(wonNodeUri, messagingService);
                }
                // try again to get the endpoint
                ep = ownerProtocolCommunicationServiceImpl.getProtocolCamelConfigurator().getEndpoint(wonNodeUri);
                if (ep == null) {
                    throw new Exception("could not obtain camel endpoint for WoN node " + wonNodeUri
                                    + " even after trying to re-register");
                }
            }
            List<WonNode> wonNodeList = wonNodeRepository.findByWonNodeURI(wonNodeUri);
            String ownerApplicationId = wonNodeList.get(0).getOwnerApplicationID();
            Map<String, Object> headerMap = new HashMap<>();
            headerMap.put(WonCamelConstants.OWNER_APPLICATION_ID_HEADER, ownerApplicationId);
            headerMap.put(WonCamelConstants.REMOTE_BROKER_ENDPOINT_HEADER, ep);
            messagingService.sendInOnlyMessage(null, headerMap, WonMessageEncoder.encode(wonMessage, Lang.TRIG),
                            startingEndpoint);
            // camelContext.getShutdownStrategy().setSuppressLoggingOnTimeout(true);
        } catch (Exception e) {
            throw new RuntimeException("could not send message", e);
        }
    }

    // TODO: adding public keys and signing can be removed when it happens in the
    // browser
    // in that case owner will have to sign only system messages, or in case it adds
    // information to the message
    // TODO exceptions
    private WonMessage signMessage(final WonMessage wonMessage) throws Exception {
        // add public key of the newly created atom
        WonMessage outMessage = atomKeyGeneratorAndAdder.process(wonMessage);
        // add signature:
        return signatureAddingProcessor.signWithAtomKey(outMessage);
    }

    private WonMessage signMessage(final WonMessage wonMessage, URI webId) throws Exception {
        // add signature:
        return signatureAddingProcessor.signWithOtherKey(wonMessage, webId);
    }

    /**
     * The owner application calls the register() method node upon initalization
     * (and during fixed time intervals) to connect to the default won node.
     *
     * @param wonNodeRegistrationEvent
     */
    @Override
    public void onApplicationEvent(final WonNodeRegistrationEvent wonNodeRegistrationEvent) {
        if (!isDefaultWonNodeRegistered) {
            try {
                new Thread() {
                    @Override
                    public void run() {
                        try {
                            logger.info("register at default won node {}", defaultNodeURI);
                            ownerProtocolCommunicationServiceImpl.register(defaultNodeURI, messagingService);
                            // try the registration as long as no exception occurs
                            logger.info("successfully registered at default won node {}", defaultNodeURI);
                            isDefaultWonNodeRegistered = true;
                        } catch (Exception e) {
                            LoggingUtils.logMessageAsInfoAndStacktraceAsDebug(logger, e,
                                            "Could not register with default won node {}. Try again later.",
                                            defaultNodeURI);
                        }
                    }
                }.start();
            } catch (Exception e) {
                LoggingUtils.logMessageAsInfoAndStacktraceAsDebug(logger, e,
                                "Could not register with default won node {}. Try again later.", defaultNodeURI);
            }
        }
    }

    public void setMessagingService(MessagingService messagingService) {
        this.messagingService = messagingService;
    }

    public void setDefaultNodeURI(URI defaultNodeURI) {
        this.defaultNodeURI = defaultNodeURI;
    }

    public void setStartingEndpoint(String startingEndpoint) {
        this.startingEndpoint = startingEndpoint;
    }
}

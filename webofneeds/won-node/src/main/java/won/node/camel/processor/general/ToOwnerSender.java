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
package won.node.camel.processor.general;

import static won.node.camel.service.WonCamelHelper.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import won.node.camel.processor.AbstractCamelProcessor;
import won.node.service.persistence.OwnerManagementService;
import won.protocol.jms.MessagingService;
import won.protocol.message.WonMessage;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.model.Atom;
import won.protocol.model.OwnerApplication;
import won.protocol.repository.OwnerApplicationRepository;

/**
 * Combines the MESSAGE_HEADER and RESPONSE_HEADER messages and sends them to
 * the owner - unless we are processing a remote response, in that case, we must
 * have no response and we just forward the MESSAGE_HEADER message to the owner.
 */
public class ToOwnerSender extends AbstractCamelProcessor {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    @Autowired
    private OwnerManagementService ownerManagementService;
    @Autowired
    private OwnerApplicationRepository ownerApplicationRepository;
    @Autowired
    private CamelContext camelContext;
    @Autowired
    private MessagingService messagingService;

    @Override
    public void process(Exchange exchange) throws Exception {
        WonMessage msg = getMessageToSendRequired(exchange);
        Objects.requireNonNull(msg);
        URI recipientAtom = getRecipientAtomURIRequired(exchange);
        Optional<Atom> atom = atomService.getAtom(recipientAtom);
        List<OwnerApplication> ownerApps = getOwnerApplications(msg, atom,
                        getOwnerApplicationId(exchange));
        // String methodName =headers.get("methodName").toString();
        logger.debug("number of registered owner applications: {}",
                        ownerApps == null ? 0 : ownerApps.size());
        List<String> queueNames = ownerApps.stream().map(app -> getQueueName(app)).collect(Collectors.toList());
        if (logger.isDebugEnabled()) {
            logger.debug("sending message to owner(s) {}: {}", Arrays.toString(queueNames.toArray()),
                            msg.toStringForDebug(true));
        }
        Exchange exchangeToOwners = new DefaultExchange(camelContext);
        putMessageIntoBody(exchangeToOwners, msg);
        exchangeToOwners.getIn().setHeader(WonCamelConstants.OWNER_APPLICATION_IDS_HEADER, queueNames);
        messagingService.send(exchangeToOwners, "direct:sendToOwnerApplications");
        removeMessageToSend(exchange);
    }

    private String getQueueName(OwnerApplication ownerapp) {
        logger.debug("ownerApplicationID: {}", ownerapp.getOwnerApplicationId());
        return ownerManagementService.getEndpointForMessage("wonMessage", ownerapp.getOwnerApplicationId());
    }

    /**
     * Sends the message to the ownerApplications registered for the specified
     * <code>atomURI</code>. If none are found, use the specified
     * <code>fallbackOwnerApplicationId</code> as fallback.
     * 
     * @param msg
     * @param ownerApplicationId
     */
    private List<OwnerApplication> getOwnerApplications(WonMessage msg, Optional<Atom> atom,
                    Optional<String> fallbackOwnerApplicationId) {
        Objects.requireNonNull(msg);
        Objects.requireNonNull(atom);
        if (logger.isDebugEnabled()) {
            logger.debug("about to send this message to registered owner apps:" + msg.toStringForDebug(true));
        }
        List<OwnerApplication> ownerApplications = new ArrayList<>();
        if (atom.isPresent()) {
            ownerApplications.addAll(atom.get().getAuthorizedApplications());
            if (logger.isDebugEnabled()) {
                logger.debug("Atom to send the message {} to: {}", msg.getMessageURI(), atom.get().getAtomURI());
                logger.debug("Found these ownerapplicationids for message {} : {}", msg.getMessageURI(),
                                ownerApplications);
            }
        }
        // if no owner application ids are authorized, we use the fallback specified (if
        // any)
        if (ownerApplications.isEmpty() && fallbackOwnerApplicationId.isPresent()) {
            if (logger.isDebugEnabled()) {
                logger.debug("Using this ownerapplicationid as fallback for message {} : {}", msg.getMessageURI(),
                                fallbackOwnerApplicationId.get());
            }
            ownerApplicationRepository.findOneByOwnerApplicationId(fallbackOwnerApplicationId.get())
                            .ifPresent(x -> ownerApplications.add(x));
        }
        return ownerApplications;
    }
}

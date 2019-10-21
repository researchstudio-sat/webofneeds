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
package won.node.service.nodebehaviour;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import won.node.service.persistence.AtomService;
import won.protocol.jms.MessagingService;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.model.Atom;
import won.protocol.service.WonNodeInformationService;

/**
 * Manipulates atoms from the system side by generating msg:FromSystem messages.
 */
@Component
public class AtomManagementService {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    @Autowired
    private MessagingService messagingService;
    @Autowired
    private WonNodeInformationService wonNodeInformationService;
    @Autowired
    private AtomService atomService;

    public void sendTextMessageToOwner(URI atomURI, String message) {
        if (atomURI == null) {
            logger.warn("sendTextMessageToOwner called but atomUri is null - doing nothing");
            return;
        }
        if (message == null || message.trim().length() == 0) {
            logger.warn("sendTextMessageToOwner called for atom {}, but message is null or empty - doing nothing",
                            atomURI);
            return;
        }
        logger.debug("Sending FromSystem text message to atom {}", atomURI);
        // check if we have that atom (e.g. it's not an atom living on another node, or
        // does not exist at all)
        Atom atom = atomService.getAtomRequired(atomURI);
        if (atom == null) {
            logger.debug("deactivateAtom called for atom {} but that atom was not found in the repository - doing nothing",
                            atomURI);
            return;
        }
        URI wonNodeURI = wonNodeInformationService.getWonNodeUri(atomURI);
        if (wonNodeURI == null) {
            logger.debug("deactivateAtom called for atom {} but we could not find a WonNodeURI for that atom - doing nothing",
                            atomURI);
            return;
        }
        URI messageURI = wonNodeInformationService.generateEventURI(wonNodeURI);
        WonMessageBuilder builder = WonMessageBuilder.setMessagePropertiesForAtomMessageFromSystem(messageURI, atomURI,
                        wonNodeURI);
        builder.setTextMessage(message);
        sendSystemMessage(builder.build());
    }

    public void deactivateAtom(URI atomURI, String optionalMessage) {
        if (atomURI == null) {
            logger.warn("deactivateAtom called but atomUri is null - doing nothing");
            return;
        }
        logger.debug("Deactivating atom {}", atomURI);
        // check if we have that atom (e.g. it's not an atom living on another node, or
        // does not exist at all)
        Atom atom = atomService.getAtomRequired(atomURI);
        if (atom == null) {
            logger.debug("deactivateAtom called for atom {} but that atom was not found in the repository - doing nothing",
                            atomURI);
            return;
        }
        URI wonNodeURI = wonNodeInformationService.getWonNodeUri(atomURI);
        if (wonNodeURI == null) {
            logger.debug("deactivateAtom called for atom {} but we could not find a WonNodeURI for that atom - doing nothing",
                            atomURI);
            return;
        }
        URI messageURI = wonNodeInformationService.generateEventURI(wonNodeURI);
        WonMessageBuilder builder = WonMessageBuilder.setMessagePropertiesForDeactivateFromSystem(messageURI, atomURI,
                        wonNodeURI);
        if (optionalMessage != null && optionalMessage.trim().length() > 0) {
            builder.setTextMessage(optionalMessage);
        }
        sendSystemMessage(builder.build());
    }

    /**
     * Processes the system message (allowing socket implementations) and delivers
     * it, depending on its receiver settings.
     * 
     * @param message
     */
    protected void sendSystemMessage(WonMessage message) {
        Map headerMap = new HashMap<String, Object>();
        headerMap.put(WonCamelConstants.MESSAGE_HEADER, message);
        messagingService.sendInOnlyMessage(null, headerMap, null, "seda:SystemMessageIn");
    }
}

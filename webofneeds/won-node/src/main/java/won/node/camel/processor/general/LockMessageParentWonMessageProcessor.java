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

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.Optional;

import javax.persistence.EntityManager;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import won.node.service.persistence.MessageService;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageDirection;
import won.protocol.message.WonMessageType;
import won.protocol.model.Atom;
import won.protocol.model.Connection;
import won.protocol.repository.AtomMessageContainerRepository;
import won.protocol.repository.AtomRepository;
import won.protocol.repository.ConnectionMessageContainerRepository;
import won.protocol.repository.ConnectionRepository;

/**
 * Acquires a pessimistic read lock on the message's parent.
 */
public class LockMessageParentWonMessageProcessor implements Processor {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    @Autowired
    ConnectionRepository connectionRepository;
    @Autowired
    AtomRepository atomRepository;
    @Autowired
    ConnectionMessageContainerRepository connectionMessageContainerRepository;
    @Autowired
    AtomMessageContainerRepository atomMessageContainerRepository;
    @Autowired
    MessageService messageService;
    @Autowired
    EntityManager entityManager;

    @Override
    public void process(Exchange exchange) throws Exception {
        WonMessage message = getMessageRequired(exchange);
        WonMessageDirection direction = getDirectionRequired(exchange);
        logger.debug("trying to lock parent of {} message {} {}",
                        new Object[] { message.getMessageType(), message.getMessageURI(),
                                        direction });
        try {
            lockParent(message, getDirectionRequired(exchange));
        } catch (Exception e) {
            URI messageUri;
            try {
                messageUri = message.getMessageURI();
            } catch (Exception e1) {
                logger.error("Error getting messageURI from WonMessage", e);
                messageUri = null;
            }
            logger.error("Error locking parent of WonMessage with uri {}", messageUri, e);
        }
    }

    private void lockParent(WonMessage message, WonMessageDirection direction) {
        // try to lock the message's connection first
        // * if the message is connection-specific
        // * and one exists already
        WonMessageType type = message.getMessageTypeRequired().isResponseMessage()
                        ? message.getRespondingToMessageTypeRequired()
                        : message.getMessageTypeRequired();
        if (type.isConnectionSpecificMessage()) {
            logger.debug("Attempting to lock connection for message {} {}", message.getMessageURI(), direction);
            Optional<URI> conURI = messageService.getConnectionofMessage(message, direction);
            if (conURI.isPresent()) {
                logger.debug("Locking connection {} for message {} {}",
                                new Object[] { conURI.get(), message.getMessageURI(),
                                                direction });
                Optional<Connection> con = connectionRepository.findOneByConnectionURIForUpdate(conURI.get());
                if (con.isPresent()) {
                    entityManager.refresh(con.get());
                    logger.debug("Locked connection {} for message {} {}",
                                    new Object[] { conURI.get(), message.getMessageURI(),
                                                    direction });
                    logger.debug("connection: {}", con.get());
                    return;
                } else {
                    logger.debug("Did not lock connection {} for message {} {}",
                                    new Object[] { conURI.get(), message.getMessageURI(),
                                                    direction });
                }
            } else {
                logger.debug("Did not find connection to lock for message {} {}", message.getMessageURI(), direction);
            }
        } else {
            logger.debug("Not attempting to lock connection for message {} {}", message.getMessageURI(), direction);
        }
        logger.debug("Attempting to lock atom for message {}", message.getMessageURI());
        // no connection found to lock or wanting to lock the atom too
        Optional<URI> atomURI = messageService.getAtomOfMessage(message, direction);
        if (atomURI.isPresent()) {
            Optional<Atom> atom = atomRepository.findOneByAtomURIForUpdate(atomURI.get());
            if (atom.isPresent()) {
                entityManager.refresh(atom.get());
                logger.debug("Locked atom {} for message {} {}",
                                new Object[] { atom.get().getAtomURI(), message.getMessageURI(), direction });
            } else {
                logger.debug("Did not find atom {} to lock for message {} {}",
                                new Object[] { atomURI.get(), message.getMessageURI(), direction });
            }
        } else {
            logger.debug("Did not find atom to lock for message {} {}", message.getMessageURI(), direction);
        }
        // it's possible that we did not lock anything because
        // * the message is neither atom- nor conneciton-specific
        // * the message is a CREATE message - the atom doesn't exist yet
    }
}

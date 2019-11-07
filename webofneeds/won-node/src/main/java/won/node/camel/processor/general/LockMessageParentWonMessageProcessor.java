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

import static won.node.camel.processor.WonCamelHelper.*;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.Optional;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import won.node.service.persistence.MessageService;
import won.protocol.message.WonMessage;
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

    @Override
    public void process(Exchange exchange) throws Exception {
        WonMessage message = getMessageRequired(exchange);
        try {
            Optional<URI> parent = messageService.getParentofMessage(message, getDirectionRequired(exchange));
            if (parent.isPresent()) {
                lockParent(message, parent.get());
            }
        } catch (Exception e) {
            URI messageUri;
            try {
                messageUri = message.getMessageURI();
            } catch (Exception e1) {
                logger.error("Error getting messageURI from WonMessage");
                messageUri = null;
            }
            logger.error("Error locking parent of WonMessage with uri {}", messageUri);
        }
    }

    private void lockParent(WonMessage message, URI parentURI) {
        // try a connection:
        Optional<Connection> con = connectionRepository.findOneByConnectionURIForUpdate(parentURI);
        if (con.isPresent()) {
            connectionMessageContainerRepository.findOneByParentUriForUpdate(parentURI);
        } else {
            atomRepository.findOneByAtomURIForUpdate(parentURI);
            atomMessageContainerRepository.findOneByParentUriForUpdate(parentURI);
        }
    }
}

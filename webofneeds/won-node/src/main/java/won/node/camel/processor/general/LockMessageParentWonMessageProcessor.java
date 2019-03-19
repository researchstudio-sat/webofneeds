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

package won.node.camel.processor.general;

import java.net.URI;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageUtils;
import won.protocol.message.processor.WonMessageProcessor;
import won.protocol.message.processor.exception.WonMessageProcessingException;
import won.protocol.model.Connection;
import won.protocol.repository.ConnectionEventContainerRepository;
import won.protocol.repository.ConnectionRepository;
import won.protocol.repository.NeedEventContainerRepository;
import won.protocol.repository.NeedRepository;

/**
 * Acquires a pessimistic read lock on the message's parent.
 */
public class LockMessageParentWonMessageProcessor implements WonMessageProcessor {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    ConnectionRepository connectionRepository;
    @Autowired
    NeedRepository needRepository;
    @Autowired
    ConnectionEventContainerRepository connectionEventContainerRepository;
    @Autowired
    NeedEventContainerRepository needEventContainerRepository;

    @Override
    public WonMessage process(WonMessage message) throws WonMessageProcessingException {
        try {
            lockParent(message);
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
        return message;
    }

    private void lockParent(WonMessage message) {
        // get the parent's URI (either a connection or a need
        URI parentURI = WonMessageUtils.getParentEntityUri(message);
        // try a connection:
        Optional<Connection> con = connectionRepository.findOneByConnectionURIForUpdate(parentURI);
        if (con.isPresent()) {
            connectionEventContainerRepository.findOneByParentUriForUpdate(parentURI);
        } else {
            needRepository.findOneByNeedURIForUpdate(parentURI);
            needEventContainerRepository.findOneByParentUriForUpdate(parentURI);
        }
    }
}

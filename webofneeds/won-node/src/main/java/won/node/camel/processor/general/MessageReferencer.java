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

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StopWatch;

import won.protocol.exception.WonMessageProcessingException;
import won.protocol.message.WonMessage;
import won.protocol.model.MessageContainer;
import won.protocol.repository.MessageContainerRepository;
import won.protocol.util.LogMarkers;
import won.protocol.vocabulary.WONMSG;

/**
 * Utility class containing code needed at multiple points for adding references
 * to previous messages to a message.
 */
public class MessageReferencer {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    @Autowired
    private MessageContainerRepository messageContainerRepository;

    /**
     * Adds message references to <code>message</code>. All message URIs that are in
     * the unconfirmed list of the message container have to be referenced if the
     * message is a success response from the container.
     */
    public WonMessage addMessageReferences(final WonMessage message, URI parentURI)
                    throws WonMessageProcessingException {
        StopWatch sw = new StopWatch();
        sw.start();
        if (message.getMessageTypeRequired().isSuccessResponse()
                        && (Objects.equals(message.getConnectionURI(), parentURI)
                                        || Objects.equals(message.getAtomURI(), parentURI))) {
            if (logger.isDebugEnabled()) {
                logger.debug("Referencing unconfirmed messages in {} from message {}", parentURI,
                                message.toShortStringForDebug());
            }
            Optional<MessageContainer> container = messageContainerRepository.findOneByParentUri(parentURI);
            if (container.isPresent()) {
                message.addMessagePropertiesURI(WONMSG.previousMessage,
                                container.get().getUnconfirmedAndIncrementAndCleanup());
                // is
                // visible in the MessageService
                if (logger.isDebugEnabled()) {
                    logger.debug("Added {} references to message {}: {}",
                                    new Object[] { container.get().getUnconfirmedCount(),
                                                    message.toShortStringForDebug(),
                                                    container.get().peekAtUnconfirmed() });
                }
            } else {
                logger.debug("No unconfirmed messages found");
            }
        }
        sw.stop();
        logger.debug(LogMarkers.TIMING, "adding unconfirmed took {} millis", sw.getLastTaskTimeMillis());
        return message;
    }
}

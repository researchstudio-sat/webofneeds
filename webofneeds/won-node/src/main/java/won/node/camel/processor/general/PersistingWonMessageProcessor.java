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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import won.node.service.persistence.MessageService;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageUtils;
import won.protocol.message.processor.WonMessageProcessor;
import won.protocol.message.processor.exception.WonMessageProcessingException;

/**
 * Persists the specified WonMessage.
 */
public class PersistingWonMessageProcessor implements WonMessageProcessor {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    @Autowired
    MessageService messageService;

    @Override
    // we use READ_COMMITTED because we want to wait for an exclusive lock will
    // accept data written by a concurrent transaction that commits before we read
    public WonMessage process(WonMessage message) throws WonMessageProcessingException {
        URI parentURI = WonMessageUtils.getParentEntityUri(message);
        messageService.updateResponseInfo(message);
        messageService.saveMessage(message, parentURI);
        return message;
    }
}

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

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import won.node.camel.service.WonCamelHelper;
import won.node.service.persistence.MessageService;
import won.protocol.message.WonMessage;

/**
 * Persists the message and if present, also the response.
 */
public class ResponsePersistingProcessor implements Processor {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    @Autowired
    MessageService messageService;

    @Override
    public void process(Exchange exchange) throws Exception {
        WonMessage message = WonCamelHelper.getResponseRequired(exchange);
        URI parentURI = WonCamelHelper.getParentURIRequired(exchange);
        if (logger.isDebugEnabled()) {
            logger.debug("storing response message {}", message.toStringForDebug(false));
        }
        messageService.saveMessage(message, parentURI);
    }
}

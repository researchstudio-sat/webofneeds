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

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import won.protocol.jms.AtomProtocolCommunicationService;
import won.protocol.jms.MessagingService;
import won.protocol.message.WonMessage;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.util.RdfUtils;

/**
 * Processor responsible for routing messages to
 */
public class AtomProtocolOutgoingMessagesProcessor implements Processor {
    private final org.slf4j.Logger logger = LoggerFactory.getLogger(getClass());
    @Autowired
    private AtomProtocolCommunicationService atomProtocolCommunicationService;
    @Autowired
    private MessagingService messageService;

    @Override
    public void process(Exchange exchange) throws Exception {
        logger.debug("processing message for sending to remote node");
        WonMessage wonMessage = (WonMessage) exchange.getIn().getHeader(WonCamelConstants.MESSAGE_HEADER);
        if (wonMessage.getSenderAtomURI() != null
                        && wonMessage.getSenderNodeURI().equals(wonMessage.getRecipientNodeURI())) {
            // sending locally, directly put message into the incoming atom protocol
            messageService.sendInOnlyMessage(null, null,
                            RdfUtils.writeDatasetToString(wonMessage.getCompleteDataset(),
                                            WonCamelConstants.RDF_LANGUAGE_FOR_MESSAGE),
                            "activemq:queue:AtomProtocol.in");
            return;
        }
        // add a camel endpoint for the remote won node
        atomProtocolCommunicationService.configureCamelEndpoint(wonMessage.getRecipientNodeURI());
        // send the message to that endpoint
        String ep = atomProtocolCommunicationService.getProtocolCamelConfigurator()
                        .getEndpoint(wonMessage.getRecipientNodeURI());
        // messageService.sendInOnlyMessage(null, null, wonMessage,
        // wonMessage.getRecipientNodeURI().toString());
        String msgBody = RdfUtils.writeDatasetToString(wonMessage.getCompleteDataset(),
                        WonCamelConstants.RDF_LANGUAGE_FOR_MESSAGE);
        if (logger.isDebugEnabled()) {
            logger.debug("sending message to node {}: {}", wonMessage.getRecipientNodeURI(),
                            wonMessage.toStringForDebug(true));
        }
        messageService.sendInOnlyMessage(null, null, msgBody, ep);
    }
}

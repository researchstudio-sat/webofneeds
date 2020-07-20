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
import java.util.Optional;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.impl.DefaultExchange;
import org.apache.jena.riot.Lang;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import won.protocol.jms.AtomProtocolCommunicationService;
import won.protocol.jms.MessagingService;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageDirection;
import won.protocol.message.WonMessageEncoder;
import won.protocol.service.MessageRoutingInfoService;

/**
 * Processor responsible for routing messages to another node, or to route it
 * internally if the recipient atom is local. Sends the message in the message
 * header, ignoring the response header.
 */
public class ToNodeSender implements Processor {
    private final org.slf4j.Logger logger = LoggerFactory.getLogger(getClass());
    @Autowired
    MessagingService messagingService;
    @Autowired
    MessageRoutingInfoService messageRoutingInfoService;
    @Autowired
    private AtomProtocolCommunicationService atomProtocolCommunicationService;
    @Autowired
    private CamelContext camelContext;

    public void process(Exchange exchange) throws Exception {
        logger.debug("processing message for sending to remote node");
        WonMessage msg = getMessageToSendRequired(exchange);
        // senderNode may be null, in this case we have to go via activemq
        Optional<URI> senderNode = messageRoutingInfoService.senderNode(msg);
        Optional<URI> recipientNode = messageRoutingInfoService.recipientNode(msg);
        if (!(senderNode.isPresent() && recipientNode.isPresent())) {
            logger.warn("Cannot send message {} to remote node: could not determine sender/recipient node",
                            msg.getMessageURI());
        }
        if (senderNode.get().equals(recipientNode.get())) {
            // sending locally, directly put message into the incoming atom protocol
            Exchange newExchangeFromExternal = new DefaultExchange(camelContext);
            putMessage(newExchangeFromExternal, msg);
            putDirection(newExchangeFromExternal, WonMessageDirection.FROM_EXTERNAL);
            putMessageType(newExchangeFromExternal, msg.getMessageType());
            messagingService.send(newExchangeFromExternal, "seda:msgFromExternal");
            removeMessageToSend(exchange);
            return;
        }
        // add a camel endpoint for the remote won node
        atomProtocolCommunicationService.configureCamelEndpoint(recipientNode.get());
        // send the message to that endpoint
        String ep = atomProtocolCommunicationService.getProtocolCamelConfigurator()
                        .getEndpoint(recipientNode.get());
        // messageService.sendInOnlyMessage(null, null, wonMessage,
        // wonMessage.getRecipientNodeURI().toString());
        String msgBody = WonMessageEncoder.encode(msg, Lang.TRIG);
        if (logger.isDebugEnabled()) {
            logger.debug("sending message to node {}: {}", recipientNode,
                            msg.toStringForDebug(true));
        }
        messagingService.sendInOnlyMessage(null, null, msgBody, ep);
        removeMessageToSend(exchange);
    }
}

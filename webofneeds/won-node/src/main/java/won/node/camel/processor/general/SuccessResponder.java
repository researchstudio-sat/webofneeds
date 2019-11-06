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
import java.util.Date;
import java.util.Optional;

import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import won.node.camel.processor.AbstractCamelProcessor;
import won.node.camel.processor.WonCamelHelper;
import won.node.service.persistence.ConnectionService;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.message.processor.impl.SignatureAddingWonMessageProcessor;
import won.protocol.model.Connection;
import won.protocol.vocabulary.WONMSG;

/**
 * Creates a success response message if appropriate. If a sucess response is
 * created, it is put in the MESSAGE_HEADER, and the original message is put in
 * the ORIGINAL_MESSAGE_HEADER.
 */
public class SuccessResponder extends AbstractCamelProcessor {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    @Autowired
    private MessageReferencer messageReferencer;
    @Autowired
    private SignatureAddingWonMessageProcessor signatureAddingWonMessageProcessor;
    @Autowired
    private ConnectionService connectionService;

    @Override
    public void process(final Exchange exchange) throws Exception {
        WonMessage originalMessage = WonCamelHelper.getMessageRequired(exchange);
        if (originalMessage.getMessageTypeRequired().isResponseMessage()) {
            // we don't respond to responses
            return;
        }
        // in the case of connect, the owners don't know connection uris yet. Tell them
        // about them by using them as the senderURI property in the response.
        Optional<URI> parent = Optional.empty();
        if (originalMessage.getMessageTypeRequired().isConnectionSpecificMessage()) {
            Optional<Connection> con = connectionService.getConnectionForMessage(originalMessage,
                            getDirectionRequired(exchange));
            if (con.isPresent()) {
                parent = Optional.of(con.get().getConnectionURI());
            }
        }
        URI newMessageURI = this.wonNodeInformationService.generateEventURI();
        WonMessage responseMessage = WonMessageBuilder
                        .setPropertiesForNodeResponse(originalMessage, true, newMessageURI,
                                        parent, getDirectionRequired(exchange))
                        .build();
        messageReferencer.addMessageReferences(responseMessage, getParentURIRequired(exchange));
        responseMessage.addMessageProperty(WONMSG.timestamp, new Date().getTime());
        responseMessage = signatureAddingWonMessageProcessor.process(responseMessage);
        exchange.getIn().setHeader(WonCamelConstants.RESPONSE_HEADER, responseMessage);
    }
}

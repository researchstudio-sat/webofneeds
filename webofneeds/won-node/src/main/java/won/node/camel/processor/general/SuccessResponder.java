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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.node.camel.processor.AbstractCamelProcessor;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.message.WonMessageDirection;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.message.processor.exception.WonMessageProcessingException;

import java.lang.invoke.MethodHandles;
import java.net.URI;

/**
 * Sends a success response message back to the sender of the original message,
 * if that message was sent on behalf of a specified atom (i.e. its
 * senderAtomURI is set).
 */
public class SuccessResponder extends AbstractCamelProcessor {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Override
    public void process(final Exchange exchange) throws Exception {
        WonMessage originalMessage = (WonMessage) exchange.getIn().getHeader(WonCamelConstants.MESSAGE_HEADER);
        if (originalMessage == null)
            throw new WonMessageProcessingException("did not find the original message in the " + "exchange header '"
                            + WonCamelConstants.MESSAGE_HEADER + "'");
        // only send success message if the original message was sent on behalf of a
        // atom (otherwise we have to find out
        // with other means which ownerapplications to send the response to.
        if (originalMessage.getSenderAtomURI() == null)
            return;
        if (originalMessage.getMessageType().isHintMessage()) {
            // we don't want to send a SuccessResponse for a hint message as matchers
            // are not fully compatible messaging agents (atoms), so sending this message
            // will fail.
            logger.debug("suppressing success response for HINT message");
            return;
        }
        URI newMessageURI = this.wonNodeInformationService.generateEventURI();
        WonMessage responseMessage = WonMessageBuilder
                        .setPropertiesForNodeResponse(originalMessage, true, newMessageURI).build();
        if (WonMessageDirection.FROM_OWNER == originalMessage.getEnvelopeType()) {
            sendSystemMessageToOwner(responseMessage);
        } else if (WonMessageDirection.FROM_EXTERNAL == originalMessage.getEnvelopeType()) {
            sendSystemMessage(responseMessage);
        }
    }
}

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

import java.io.StringWriter;
import java.net.URI;

import org.apache.camel.Exchange;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;

import won.node.camel.processor.AbstractCamelProcessor;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.message.WonMessageDirection;
import won.protocol.message.WonMessageType;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.util.RdfUtils;
import won.protocol.util.WonRdfUtils;

/**
 * Sends a error response message back to the sender of the original message, if that message was sent on behalf of a
 * specified need (i.e. its senderNeedURI is set).
 */
public class FailResponder extends AbstractCamelProcessor {
    @Override
    public void process(final Exchange exchange) throws Exception {
        Exception exception = null;
        WonMessage originalMessage = null;
        try {
            originalMessage = (WonMessage) exchange.getIn().getHeader(WonCamelConstants.ORIGINAL_MESSAGE_HEADER);

            if (originalMessage == null) {
                logger.debug(
                        "Processing an exception. camel header {} was null, assuming original message in header {}",
                        WonCamelConstants.ORIGINAL_MESSAGE_HEADER, WonCamelConstants.MESSAGE_HEADER);
                originalMessage = (WonMessage) exchange.getIn().getHeader(WonCamelConstants.MESSAGE_HEADER);
            }
            if (originalMessage == null) {
                // we didn't find the original message, so we can't send a response.
                // Log all we can so that we can start debugging the problem
                logger.warn("Could not obtain original message from camel headers {} or {} for error {}",
                        new Object[] { WonCamelConstants.ORIGINAL_MESSAGE_HEADER, WonCamelConstants.MESSAGE_HEADER,
                                exchange.getProperty(Exchange.EXCEPTION_CAUGHT) });
                logger.warn("original exception:", exchange.getProperty(Exchange.EXCEPTION_CAUGHT));
                return;
            }
            exception = (Exception) exchange.getProperty(Exchange.EXCEPTION_CAUGHT);
            String errormessage = null;
            if (exception != null) {
                errormessage = exception.getClass().getSimpleName() + ": " + exception.getMessage();
            } else {
                errormessage = String.format("An error occurred while processing message %s",
                        originalMessage.getMessageURI());
            }
            if (originalMessage.getMessageType() == WonMessageType.HINT_MESSAGE) {
                // we don't want to send a FailureResponse for a hint message as matchers
                // are not fully compatible messaging agents (needs), so sending this message will fail.
                logger.debug("suppressing failure response for HINT message", exception);
                return;
            }
            logger.info(
                    "Caught error while processing WON message {} (type:{}) : {} - sending FailureResponse (more info on log level DEBUG)",
                    new Object[] { originalMessage.getMessageURI(), originalMessage.getMessageType(), errormessage });
            if (exception != null) {
                logger.debug("stacktrace of caught exception:", exception);
            }
            logger.debug("original message: {}", RdfUtils.toString(originalMessage.getCompleteDataset()));

            if (WonMessageType.FAILURE_RESPONSE == originalMessage.getMessageType()
                    && WonMessageType.FAILURE_RESPONSE == originalMessage.getIsResponseToMessageType()) {
                // do not throw failures back and forth. If the original message is already a failure message
                // that indicates a problem processing a failure message, log this and stop.
                logger.info(
                        "Encountered an error processing a FailureResponse for a FailureResponse. The FailureResponse is "
                                + "logged at log level DEBUG. Its message URI is {}",
                        originalMessage.getMessageURI(), exception);

                StringWriter sw = new StringWriter();
                RDFDataMgr.write(sw, originalMessage.getCompleteDataset(), Lang.TRIG);
                logger.warn("FailureResponse to FailureResponse that raised the error:\n{}", sw.toString());

                return;
            }
            URI newMessageURI = this.wonNodeInformationService.generateEventURI();
            logger.debug("Sending FailureResponse {}", newMessageURI);
            Model errorMessageContent = WonRdfUtils.MessageUtils.textMessage(errormessage);
            RdfUtils.replaceBaseURI(errorMessageContent, newMessageURI.toString());
            WonMessage responseMessage = WonMessageBuilder
                    .setPropertiesForNodeResponse(originalMessage, false, newMessageURI).addContent(errorMessageContent)
                    .build();

            if (WonMessageDirection.FROM_OWNER == originalMessage.getEnvelopeType()) {
                String ownerApplicationId = (String) exchange.getIn().getHeader(WonCamelConstants.OWNER_APPLICATION_ID);
                sendSystemMessageToOwner(responseMessage, ownerApplicationId);
            } else if (WonMessageDirection.FROM_EXTERNAL == originalMessage.getEnvelopeType()) {
                sendSystemMessage(responseMessage);
            } else {
                logger.info(String.format("cannot route failure message for direction of original message, "
                        + "expected FROM_OWNER or FROM_EXTERNAL, but found %s. Original cause is logged on log level DEBUG.",
                        originalMessage.getEnvelopeType()));
                logger.debug("original cause", exception);
            }
        } catch (Throwable t) {
            // something went wrong - we can't inform the sender of the message.
            // now:
            // 1. log the error we had here
            // 2. log the original error, otherwise it is swallowed completely
            logger.warn("Error in failure response handling!");
            URI originalMessageURI = null;
            try {
                originalMessageURI = originalMessage == null ? null : originalMessage.getMessageURI();
            } catch (Exception e) {
                logger.error("Error getting message URI from WonMessage");
            }

            if (exception != null && exception.getClass() != null) {
                logger.warn(String.format(
                        "Could not send FailureResponse for original Exception %s (message: %s) that occurred while processing message %s.",
                        exception.getClass().getSimpleName(), exception.getMessage(), originalMessageURI), t);
                logger.warn("original error: ", exception);
            } else {
                logger.warn(String.format("Could not send FailureResponse to original message %s.", originalMessageURI),
                        t);
                logger.warn("original error: ", exception);
            }
        }
    }

}

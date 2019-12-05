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

import java.io.StringWriter;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.Optional;

import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import won.node.camel.processor.AbstractCamelProcessor;
import won.protocol.exception.WonProtocolException;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageDirection;
import won.protocol.message.WonMessageType;
import won.protocol.message.builder.ResponseBuilder;
import won.protocol.message.builder.WonMessageBuilder;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.message.processor.impl.SignatureAddingWonMessageProcessor;
import won.protocol.model.Connection;
import won.protocol.service.MessageRoutingInfoService;
import won.protocol.util.Prefixer;
import won.protocol.util.RdfUtils;

/**
 * Sends a error response message back to the sender of the original message, if
 * that message was sent on behalf of a specified atom (i.e. its senderAtomURI
 * is set).
 */
public class FailResponder extends AbstractCamelProcessor {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    @Autowired
    MessageRoutingInfoService messageRoutingInfoService;
    @Autowired
    ProducerTemplate producerTemplate;
    @Autowired
    private SignatureAddingWonMessageProcessor signatureAddingWonMessageProcessor;

    @Override
    public void process(final Exchange exchange) throws Exception {
        Exception exception = null;
        WonMessage originalMessage = null;
        try {
            originalMessage = (WonMessage) exchange.getIn().getHeader(WonCamelConstants.MESSAGE_HEADER);
            if (originalMessage == null) {
                // we didn't find the original message, so we can't send a response.
                // Log all we can so that we can start debugging the problem
                logger.warn("Could not obtain original message from camel header {} for error {}",
                                new Object[] { WonCamelConstants.MESSAGE_HEADER,
                                                exchange.getProperty(Exchange.EXCEPTION_CAUGHT) });
                logger.warn("original exception:", exchange.getProperty(Exchange.EXCEPTION_CAUGHT));
                return;
            }
            exception = (Exception) exchange.getProperty(Exchange.EXCEPTION_CAUGHT);
            String errormessage = null;
            if (exception != null) {
                errormessage = exception.getClass().getSimpleName() + ": " + exception.getMessage();
            }
            if (errormessage != null) {
                errormessage = String.format("An error occurred while processing message %s (type: %s): %s",
                                originalMessage.getMessageURI(), originalMessage.getMessageType(), errormessage);
            }
            if (exception instanceof WonProtocolException) {
                if (logger.isDebugEnabled()) {
                    if (exception != null) {
                        logger.debug("Caught protocol exception. Sending FailureResponse ", exception);
                    }
                }
            } else {
                logger.warn("Caught unexpected exception while processing WON message {} (type:{}) : {} - sending FailureResponse",
                                new Object[] { originalMessage.getMessageURI(), originalMessage.getMessageType(),
                                                errormessage });
                logger.warn("Full stacktrace: ", exception);
            }
            if (logger.isDebugEnabled()) {
                logger.debug("original message: {}",
                                RdfUtils.toString(Prefixer.setPrefixes(originalMessage.getCompleteDataset())));
            }
            if (WonMessageType.FAILURE_RESPONSE == originalMessage.getMessageType()) {
                // do not throw failures back and forth. If the original message is already a
                // failure message
                // that indicates a problem processing a failure message, log this and stop.
                logger.info("Encountered an error processing a FailureResponse. The FailureResponse is "
                                + "logged at log level DEBUG. Its message URI is {}", originalMessage.getMessageURI(),
                                exception);
                StringWriter sw = new StringWriter();
                RDFDataMgr.write(sw, Prefixer.setPrefixes(originalMessage.getCompleteDataset()), Lang.TRIG);
                logger.warn("FailureResponse to FailureResponse that raised the error:\n{}", sw.toString());
                return;
            }
            if (originalMessage.getMessageTypeRequired().isResponseMessage()) {
                // not sending a failure response for a response
                return;
            }
            WonMessageDirection direction = getDirectionRequired(exchange);
            ResponseBuilder responseBuilder = WonMessageBuilder.response();
            // in the case of connect, the owners don't know connection uris yet. Tell them
            // about them by using them as the senderURI property in the response.
            if (originalMessage.getMessageTypeRequired().isConnectionSpecificMessage()) {
                Optional<Connection> con = connectionService.getConnectionForMessage(originalMessage,
                                getDirectionRequired(exchange));
                if (con.isPresent()) {
                    responseBuilder.fromConnection(con.get().getConnectionURI());
                }
            } else if (originalMessage.getMessageTypeRequired().isAtomSpecificMessage()) {
                responseBuilder.fromAtom(originalMessage.getAtomURIRequired());
            }
            WonMessage responseMessage = responseBuilder
                            .respondingToMessage(originalMessage, getDirectionRequired(exchange))
                            .failure()
                            .content().text(errormessage)
                            .build();
            responseMessage = signatureAddingWonMessageProcessor.signWithDefaultKey(responseMessage);
            putResponse(exchange, responseMessage);
            putMessageToSend(exchange, responseMessage);
            // extract the routing information
            URI atom = messageRoutingInfoService.recipientAtom(responseMessage)
                            .orElseThrow(() -> new IllegalArgumentException(
                                            "Cannot dertermine recipient atom for response"));
            // the sender node is not there in the case of hint messages.
            Optional<URI> senderNode = messageRoutingInfoService.senderNode(responseMessage);
            URI recipientNode = messageRoutingInfoService.recipientNode(responseMessage)
                            .orElseThrow(() -> new IllegalArgumentException(
                                            "Cannot dertermine node for response"));
            if (senderNode.isPresent()) {
                putSenderNodeURI(exchange, senderNode.get());
            }
            putRecipientNodeURI(exchange, recipientNode);
            putRecipientAtomURI(exchange, atom);
            // send the message
            if (direction.isFromExternal()) {
                messagingService.send(exchange, "direct:sendToNode");
            } else if (direction.isFromOwner()) {
                messagingService.send(exchange, "direct:sendToOwner");
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

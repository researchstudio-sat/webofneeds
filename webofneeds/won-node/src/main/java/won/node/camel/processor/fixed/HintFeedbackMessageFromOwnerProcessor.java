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
package won.node.camel.processor.fixed;

import java.net.URI;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.springframework.stereotype.Component;

import won.node.camel.processor.AbstractFromOwnerCamelProcessor;
import won.node.camel.processor.annotation.FixedMessageProcessor;
import won.protocol.message.WonMessage;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.model.Connection;
import won.protocol.util.RdfUtils;
import won.protocol.vocabulary.WON;
import won.protocol.vocabulary.WONMSG;

/**
 * User: syim Date: 02.03.2015
 */
@Component
@FixedMessageProcessor(direction = WONMSG.TYPE_FROM_OWNER_STRING, messageType = WONMSG.TYPE_HINT_FEEDBACK_STRING)
public class HintFeedbackMessageFromOwnerProcessor extends AbstractFromOwnerCamelProcessor {
    public void process(final Exchange exchange) throws Exception {
        Message message = exchange.getIn();
        WonMessage wonMessage = (WonMessage) message.getHeader(WonCamelConstants.MESSAGE_HEADER);
        Connection con = connectionRepository.findOneByConnectionURIForUpdate(wonMessage.getSenderURI()).get();
        logger.debug("HINT_FEEDBACK received from the owner side for connection {}", wonMessage.getSenderURI());
        processFeedbackMessage(con, wonMessage);
    }

    /////// TODO: move code below to the implementation of a FEEDBACK message
    /**
     * Finds feedback in the message, processes it and removes it from the message.
     *
     * @param con
     * @param message
     * @return true if feedback was present, false otherwise
     */
    private void processFeedbackMessage(final Connection con, final WonMessage message) {
        assert con != null : "connection must not be null";
        assert message != null : "message must not be null";
        final URI messageURI = message.getMessageURI();
        RdfUtils.visit(message.getMessageContent(), new RdfUtils.ModelVisitor<Object>() {
            @Override
            public Model visit(final Model model) {
                Resource baseResource = model.getResource(messageURI.toString());
                if (baseResource.hasProperty(WON.HAS_FEEDBACK)) {
                    // add the base resource as a feedback event to the connection
                    processFeedback(con, baseResource);
                }
                return null;
            }
        });
    }

    private void processFeedback(Connection connection, final RDFNode feedbackNode) {
        if (!feedbackNode.isResource()) {
            logger.warn("feedback node is not a resource, cannot process feedback for {}",
                            connection.getConnectionURI());
            return;
        }
        final Resource feedbackRes = (Resource) feedbackNode;
        if (!dataService.addFeedback(connection, feedbackRes)) {
            logger.warn("failed to add feedback to resource {}", connection.getConnectionURI());
        }
    }
}

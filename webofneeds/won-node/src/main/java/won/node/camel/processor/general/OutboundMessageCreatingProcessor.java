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

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import won.protocol.message.WonMessage;
import won.protocol.message.processor.camel.WonCamelConstants;

/**
 * Processor that expects an outgoingMessageFactory in the respecitve in header,
 * uses it to generate the outgoing message and places the new message in the
 * WonOutgoingMessage header
 */
public class OutboundMessageCreatingProcessor implements Processor {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Override
    public void process(Exchange exchange) throws Exception {
        WonMessage wonMessage = (WonMessage) exchange.getIn().getHeader(WonCamelConstants.MESSAGE_HEADER);
        if (wonMessage == null) {
            logger.debug("did not find a WonMessage in header {}, this is unexpected ",
                            WonCamelConstants.MESSAGE_HEADER);
            return;
        }
        // just send the original message
        exchange.getIn().setHeader(WonCamelConstants.OUTBOUND_MESSAGE_HEADER, wonMessage);
    }
}

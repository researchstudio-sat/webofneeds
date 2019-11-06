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

import static won.node.camel.processor.WonCamelHelper.*;

import java.lang.invoke.MethodHandles;

import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import won.node.camel.processor.AbstractCamelProcessor;
import won.node.camel.processor.annotation.FixedMessageProcessor;
import won.protocol.message.WonMessage;
import won.protocol.model.Connection;
import won.protocol.vocabulary.WONMSG;

/**
 * Processes a CLOSE message coming from the FROM_SYSTEM direction. The effects
 * are:
 * <ul>
 * <li>the connection is closed</li>
 * <li>a CLOSE message is sent to the remote end the connection</li>
 * <li>the message is forwarded to the owner (so the owner notices the
 * CLOSE)</li>
 * </ul>
 */
@Component
@FixedMessageProcessor(direction = WONMSG.FromSystemString, messageType = WONMSG.CloseMessageString)
public class CloseMessageFromSystemProcessor extends AbstractCamelProcessor {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public void process(final Exchange exchange) throws Exception {
        WonMessage wonMessage = getMessageRequired(exchange);
        logger.debug("CLOSE received from the system side for connection {}", wonMessage.getSenderURI());
        Connection con = connectionService.closeFromSystem(wonMessage);
        // if we know the remote connection, send a close message to the remote
        // connection
        if (con.getTargetConnectionURI() == null) {
            suppressMessageToNode(exchange);
        }
    }
}

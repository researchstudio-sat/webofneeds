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
import java.util.Collection;

import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import won.node.camel.processor.AbstractCamelProcessor;
import won.node.camel.processor.annotation.FixedMessageReactionProcessor;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.message.WonMessageDirection;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.message.processor.exception.WonMessageProcessingException;
import won.protocol.model.Connection;
import won.protocol.model.ConnectionState;
import won.protocol.model.Need;
import won.protocol.util.DataAccessUtils;
import won.protocol.vocabulary.WONMSG;

/**
 * After processing a replace message, a notification is sent in all established
 * connections. TODO: notify matchers.
 */
@Component
@FixedMessageReactionProcessor(direction = WONMSG.TYPE_FROM_OWNER_STRING, messageType = WONMSG.TYPE_REPLACE_STRING)
public class ReplaceNeedMessageFromOwnerReactionProcessor extends AbstractCamelProcessor {
    Logger logger = LoggerFactory.getLogger(this.getClass());

    public void process(final Exchange exchange) throws Exception {
        WonMessage wonMessage = (WonMessage) exchange.getIn().getHeader(WonCamelConstants.MESSAGE_HEADER);
        URI receiverNeedURI = wonMessage.getReceiverNeedURI();
        logger.debug("Replaced need. needURI:{}", receiverNeedURI);
        if (receiverNeedURI == null)
            throw new WonMessageProcessingException("receiverNeedURI is not set");
        Need need = DataAccessUtils.loadNeed(needRepository, receiverNeedURI);
        // matcherProtocolMatcherClient.needModified(need.getNeedURI(), wonMessage);
        // notify all connections
        Collection<Connection> conns = connectionRepository.findByNeedURIAndState(need.getNeedURI(),
                        ConnectionState.CONNECTED);
        for (Connection con : conns) {
            sendMessage(need, con, "Note: need content was changed.");
        }
    }

    public void sendMessage(final Need need, final Connection con, String textMessage) {
        // send message from system via connection
        URI messageURI = wonNodeInformationService.generateEventURI();
        URI remoteWonNodeURI = wonNodeInformationService.getWonNodeUri(con.getRemoteNeedURI());
        WonMessage message = WonMessageBuilder.setMessagePropertiesForSystemMessageToRemoteNeed(messageURI,
                        con.getConnectionURI(), con.getNeedURI(), need.getWonNodeURI(), con.getRemoteConnectionURI(),
                        con.getRemoteNeedURI(), remoteWonNodeURI, textMessage).build();
        sendSystemMessage(message);
    }
}

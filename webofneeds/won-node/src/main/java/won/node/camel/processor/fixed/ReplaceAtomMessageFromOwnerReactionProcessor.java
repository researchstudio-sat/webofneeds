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
import won.protocol.message.builder.WonMessageBuilder;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.model.Atom;
import won.protocol.model.Connection;
import won.protocol.model.ConnectionState;
import won.protocol.vocabulary.WONMSG;

/**
 * After processing a replace message, a notification is sent in all established
 * connections. TODO: notify matchers.
 */
@Component
@FixedMessageReactionProcessor(direction = WONMSG.FromOwnerString, messageType = WONMSG.ReplaceMessageString)
public class ReplaceAtomMessageFromOwnerReactionProcessor extends AbstractCamelProcessor {
    Logger logger = LoggerFactory.getLogger(this.getClass());

    public void process(final Exchange exchange) throws Exception {
        WonMessage wonMessage = (WonMessage) exchange.getIn().getHeader(WonCamelConstants.MESSAGE_HEADER);
        URI atomURI = wonMessage.getSenderAtomURI();
        if (atomURI == null)
            throw new IllegalArgumentException("senderAtomURI not found!");
        logger.debug("Reacting to atom replacement. AtomURI:{}", atomURI);
        Atom atom = atomService.getAtomRequired(atomURI);
        matcherProtocolMatcherClient.atomModified(atom.getAtomURI(), wonMessage);
        // notify all connections
        Collection<Connection> conns = connectionRepository.findByAtomURIAndState(atom.getAtomURI(),
                        ConnectionState.CONNECTED);
        for (Connection con : conns) {
            sendChangeNotificationMessage(atom, con);
        }
    }

    private void sendChangeNotificationMessage(final Atom atom, final Connection con) {
        // send message from system via connection
        URI messageURI = wonNodeInformationService.generateEventURI();
        WonMessage message = WonMessageBuilder
                        .changeNotificatin(messageURI)
                        .direction().fromSystem()
                        .sockets()
                        /**/.sender(con.getSocketURI())
                        /**/.recipient(con.getTargetSocketURI())
                        .build();
        camelWonMessageService.sendSystemMessage(message);
    }
}

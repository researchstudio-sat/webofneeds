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

import javax.persistence.EntityManager;

import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import won.node.camel.processor.AbstractCamelProcessor;
import won.node.camel.processor.annotation.FixedMessageReactionProcessor;
import won.protocol.exception.WonMessageProcessingException;
import won.protocol.message.WonMessage;
import won.protocol.message.builder.WonMessageBuilder;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.model.Atom;
import won.protocol.model.Connection;
import won.protocol.model.ConnectionState;
import won.protocol.vocabulary.WONMSG;

/**
 *
 */
@Component
@FixedMessageReactionProcessor(direction = WONMSG.FromOwnerString, messageType = WONMSG.DeactivateMessageString)
public class DeactivateAtomMessageFromOwnerReactionProcessor extends AbstractCamelProcessor {
    Logger logger = LoggerFactory.getLogger(this.getClass());
    @Autowired
    EntityManager entityManager;

    public void process(final Exchange exchange) throws Exception {
        WonMessage wonMessage = (WonMessage) exchange.getIn().getHeader(WonCamelConstants.MESSAGE_HEADER);
        URI recipientAtomURI = wonMessage.getRecipientAtomURI();
        logger.debug("DEACTIVATING atom. atomURI:{}", recipientAtomURI);
        if (recipientAtomURI == null)
            throw new WonMessageProcessingException("recipientAtomURI is not set");
        Atom atom = atomService.getAtomRequired(recipientAtomURI);
        matcherProtocolMatcherClient.atomDeactivated(atom.getAtomURI(), wonMessage);
        // close all connections
        Collection<Connection> conns = connectionRepository.findByAtomURIAndNotStateForUpdate(atom.getAtomURI(),
                        ConnectionState.CLOSED);
        for (Connection con : conns) {
            entityManager.refresh(con);
            closeConnection(atom, con);
        }
    }

    public void closeConnection(final Atom atom, final Connection con) {
        // send close from system to each connection
        // the close message is directed at our local connection. It will
        // be routed to the owner and forwarded to to remote connection
        WonMessage message = WonMessageBuilder
                        .close()
                        .sockets()
                        /**/.sender(con.getSocketURI())
                        /**/.recipient(con.getTargetSocketURI())
                        .content()
                        /**/.text("Closed because Atom was deactivated")
                        .direction()
                        /**/.fromSystem()
                        .build();
        camelWonMessageService.sendSystemMessage(message);
    }
}

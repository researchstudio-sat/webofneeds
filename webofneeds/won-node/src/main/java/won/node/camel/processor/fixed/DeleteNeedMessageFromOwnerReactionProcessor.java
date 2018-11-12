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

package won.node.camel.processor.fixed;

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

import java.net.URI;
import java.util.Collection;

/**
 *
 */
@Component
@FixedMessageReactionProcessor(direction= WONMSG.TYPE_FROM_OWNER_STRING,messageType = WONMSG.TYPE_DELETE_STRING)
public class DeleteNeedMessageFromOwnerReactionProcessor extends AbstractCamelProcessor
{
  Logger logger = LoggerFactory.getLogger(this.getClass());

  public void process(final Exchange exchange) throws Exception {
    WonMessage wonMessage = (WonMessage) exchange.getIn().getHeader(WonCamelConstants.MESSAGE_HEADER);
    URI receiverNeedURI = wonMessage.getReceiverNeedURI();
    URI wonNodeUri = wonMessage.getReceiverNodeURI();
    logger.debug("DEACTIVATING need. needURI:{}", receiverNeedURI);
    if (receiverNeedURI == null) throw new WonMessageProcessingException("receiverNeedURI is not set");
    matcherProtocolMatcherClient.needDeactivated(receiverNeedURI, wonMessage);
    //close all connections
    Collection<Connection> conns = connectionRepository.getConnectionsByNeedURIAndNotInStateForUpdate(receiverNeedURI, ConnectionState.CLOSED);
    for (Connection con: conns) {
      closeConnection(wonNodeUri, con);
      connectionRepository.delete(con);
    }

  }

  public void closeConnection(final URI wonNodeUri, final Connection con) {
    //send close from system to each connection
    //the close message is directed at our local connection. It will
    //be routed to the owner and forwarded to to remote connection
    URI messageURI = wonNodeInformationService.generateEventURI();
    WonMessage message = WonMessageBuilder
      .setMessagePropertiesForClose(messageURI,
                                    WonMessageDirection.FROM_SYSTEM,
                                    con.getConnectionURI(),
                                    con.getNeedURI(),
                                    wonNodeUri,
                                    con.getConnectionURI(),
                                    con.getNeedURI(),
                                    wonNodeUri, "Closed because Need was deleted").build();

    sendSystemMessage(message);
  }

}

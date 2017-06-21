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
import org.apache.camel.Message;
import org.springframework.stereotype.Component;
import won.node.camel.processor.AbstractCamelProcessor;
import won.node.camel.processor.annotation.FixedMessageProcessor;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.model.Connection;
import won.protocol.model.ConnectionEventType;
import won.protocol.model.ConnectionState;
import won.protocol.vocabulary.WONMSG;

import java.net.URI;


/**
 * Processes a CLOSE message coming from the FROM_SYSTEM direction.
 * The effects are:
 * <ul>
 *   <li>the connection is closed</li>
 *   <li>a CLOSE message is sent to the remote end the connection</li>
 *   <li>the message is forwarded to the owner (so the owner notices the CLOSE)</li>
 * </ul>
 *
 */
@Component
@FixedMessageProcessor(
        direction= WONMSG.TYPE_FROM_SYSTEM_STRING,
        messageType = WONMSG.TYPE_CLOSE_STRING)
public class CloseMessageFromSystemProcessor extends AbstractCamelProcessor
{

  public void process(final Exchange exchange) throws Exception {
    Message message = exchange.getIn();
    WonMessage wonMessage = (WonMessage) message.getHeader(WonCamelConstants.MESSAGE_HEADER);

    logger.debug("CLOSE received from the system side for connection {}", wonMessage.getSenderURI());

    Connection con = connectionRepository.findOneByConnectionURIForUpdate(wonMessage.getSenderURI());
    ConnectionState originalState = con.getState();
      //TODO: we could introduce SYSTEM_CLOSE here
    con = dataService.nextConnectionState(con, ConnectionEventType.OWNER_CLOSE);
    //if the connection was in suggested state, don't send a close message to the remote need
    if (originalState != ConnectionState.SUGGESTED) {
      //prepare the message to pass to the remote node
      final WonMessage newWonMessage = createMessageToSendToRemoteNode(wonMessage, con);
      //abort if there is no remote connection
      if (newWonMessage == null) {
        return;
      }
      //put it into the 'outbound message' header (so the persister doesn't pick up the wrong one).
      exchange.getIn().setHeader(WonCamelConstants.OUTBOUND_MESSAGE_HEADER, newWonMessage);
      //set the sender uri in the envelope TODO: TwoMsgs: do not set sender here
      wonMessage.addMessageProperty(WONMSG.SENDER_PROPERTY, con.getConnectionURI());
      //add the information about the corresponding message to the local one
      wonMessage.addMessageProperty(WONMSG.HAS_CORRESPONDING_REMOTE_MESSAGE, newWonMessage.getMessageURI());
      //the persister will pick it up later
    }

    //because the FromSystem message is now in the message header, it will be
    //picked up by the routing system and delivered to the owner.

    //the message for the remote connection is in the outbound message header and will be
    // sent to the remote connection.
  }

  private WonMessage createMessageToSendToRemoteNode(WonMessage wonMessage, Connection con) {
    //there need not be a remote connection. Don't create a message if this is the case.
    if (con.getRemoteConnectionURI() == null) return null;
    URI remoteNodeURI = wonNodeInformationService.getWonNodeUri(con.getRemoteConnectionURI());
    URI localNodeURI = wonNodeInformationService.getWonNodeUri(con.getConnectionURI());
    //create the message to send to the remote node
    return WonMessageBuilder
      .setPropertiesForPassingMessageToRemoteNode(
        wonMessage,
        wonNodeInformationService
          .generateEventURI(remoteNodeURI))
      .setSenderNodeURI(localNodeURI)
      .setSenderURI(con.getConnectionURI())
      .setSenderNeedURI(con.getNeedURI())
      .setReceiverNodeURI(remoteNodeURI)
      .setReceiverURI(con.getRemoteConnectionURI())
      .setReceiverNeedURI(con.getRemoteNeedURI())
      .build();
  }


}

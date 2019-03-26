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

import java.net.URI;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.springframework.stereotype.Component;

import won.node.camel.processor.AbstractCamelProcessor;
import won.node.camel.processor.annotation.FixedMessageProcessor;
import won.node.camel.processor.general.OutboundMessageFactoryProcessor;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.message.processor.exception.WonMessageProcessingException;
import won.protocol.model.Connection;
import won.protocol.model.ConnectionEventType;
import won.protocol.model.ConnectionState;
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
 *
 */
@Component
@FixedMessageProcessor(direction = WONMSG.TYPE_FROM_SYSTEM_STRING, messageType = WONMSG.TYPE_CLOSE_STRING)
public class CloseMessageFromSystemProcessor extends AbstractCamelProcessor {

  public void process(final Exchange exchange) throws Exception {
    Message message = exchange.getIn();
    WonMessage wonMessage = (WonMessage) message.getHeader(WonCamelConstants.MESSAGE_HEADER);

    logger.debug("CLOSE received from the system side for connection {}", wonMessage.getSenderURI());

    Connection con = connectionRepository.findOneByConnectionURIForUpdate(wonMessage.getSenderURI()).get();
    ConnectionState originalState = con.getState();
    // TODO: we could introduce SYSTEM_CLOSE here
    con = dataService.nextConnectionState(con, ConnectionEventType.OWNER_CLOSE);
    // if we know the remote connection, send a close message to the remote
    // connection
    if (con.getRemoteConnectionURI() != null) {
      URI remoteNodeURI = wonNodeInformationService.getWonNodeUri(con.getRemoteConnectionURI());
      URI remoteMessageUri = wonNodeInformationService.generateEventURI(remoteNodeURI);

      // put the factory into the outbound message factory header. It will be used to
      // generate the outbound message
      // after the wonMessage has been processed and saved, to make sure that the
      // outbound message contains
      // all the data that we also store locally
      OutboundMessageFactory outboundMessageFactory = new OutboundMessageFactory(remoteMessageUri, con);
      message.setHeader(WonCamelConstants.OUTBOUND_MESSAGE_FACTORY_HEADER, outboundMessageFactory);

      // set the sender uri in the envelope TODO: TwoMsgs: do not set sender here
      wonMessage.addMessageProperty(WONMSG.SENDER_PROPERTY, con.getConnectionURI());
      // add the information about the corresponding message to the local one
      wonMessage.addMessageProperty(WONMSG.HAS_CORRESPONDING_REMOTE_MESSAGE, remoteMessageUri);
      // the persister will pick it up later
    }

    // because the FromSystem message is now in the message header, it will be
    // picked up by the routing system and delivered to the owner.

    // the message for the remote connection is in the outbound message header and
    // will be
    // sent to the remote connection.
  }

  private class OutboundMessageFactory extends OutboundMessageFactoryProcessor {
    private Connection connection;

    public OutboundMessageFactory(URI messageURI, Connection connection) {
      super(messageURI);
      this.connection = connection;
    }

    @Override
    public WonMessage process(WonMessage message) throws WonMessageProcessingException {
      // there need not be a remote connection. Don't create a message if this is the
      // case.
      if (connection.getRemoteConnectionURI() == null)
        return null;
      URI remoteNodeURI = wonNodeInformationService.getWonNodeUri(connection.getRemoteConnectionURI());
      URI localNodeURI = wonNodeInformationService.getWonNodeUri(connection.getConnectionURI());
      // create the message to send to the remote node
      return WonMessageBuilder.setPropertiesForPassingMessageToRemoteNode(message, getMessageURI())
          .setSenderNodeURI(localNodeURI).setSenderURI(connection.getConnectionURI())
          .setSenderNeedURI(connection.getNeedURI()).setReceiverNodeURI(remoteNodeURI)
          .setReceiverURI(connection.getRemoteConnectionURI()).setReceiverNeedURI(connection.getRemoteNeedURI())
          .build();
    }
  }

}

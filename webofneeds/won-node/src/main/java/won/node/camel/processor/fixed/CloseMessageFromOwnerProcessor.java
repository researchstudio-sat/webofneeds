package won.node.camel.processor.fixed;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.springframework.stereotype.Component;
import won.node.camel.processor.AbstractFromOwnerCamelProcessor;
import won.node.camel.processor.annotation.FixedMessageProcessor;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.model.Connection;
import won.protocol.model.ConnectionEventType;
import won.protocol.model.ConnectionState;
import won.protocol.vocabulary.WONMSG;

/**
 * User: syim
 * Date: 02.03.2015
 */
@Component
@FixedMessageProcessor(
        direction= WONMSG.TYPE_FROM_OWNER_STRING,
        messageType = WONMSG.TYPE_CLOSE_STRING)
public class CloseMessageFromOwnerProcessor extends AbstractFromOwnerCamelProcessor
{

  public void process(final Exchange exchange) throws Exception {
    Message message = exchange.getIn();
    WonMessage wonMessage = (WonMessage) message.getHeader(WonCamelConstants.MESSAGE_HEADER);

    logger.debug("CLOSE received from the owner side for connection {}", wonMessage.getSenderURI());

    Connection con = connectionRepository.findOneByConnectionURIForUpdate(wonMessage.getSenderURI());
    ConnectionState originalState = con.getState();
    con = dataService.nextConnectionState(con, ConnectionEventType.OWNER_CLOSE);
    //if the connection was in suggested state, don't send a close message to the remote need
    if (originalState != ConnectionState.SUGGESTED) {
      //prepare the message to pass to the remote node
      final WonMessage newWonMessage = createMessageToSendToRemoteNode(wonMessage);
      //put it into the 'outbound message' header (so the persister doesn't pick up the wrong one).
      message.setHeader(WonCamelConstants.OUTBOUND_MESSAGE_HEADER, newWonMessage);
      //set the sender uri in the envelope TODO: TwoMsgs: do not set sender here
      wonMessage.addMessageProperty(WONMSG.SENDER_PROPERTY, con.getConnectionURI());
      //add the information about the corresponding message to the local one
      wonMessage.addMessageProperty(WONMSG.HAS_CORRESPONDING_REMOTE_MESSAGE, newWonMessage.getMessageURI());
      //the persister will pick it up later from the header
    }
  }

  private WonMessage createMessageToSendToRemoteNode(WonMessage wonMessage) {
    //create the message to send to the remote node
    return WonMessageBuilder
      .setPropertiesForPassingMessageToRemoteNode(
        wonMessage,
        wonNodeInformationService
          .generateEventURI(wonMessage.getReceiverNodeURI()))
      .build();
  }


}

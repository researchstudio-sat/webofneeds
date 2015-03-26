package won.node.messaging.processors;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.springframework.stereotype.Component;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.model.Connection;
import won.protocol.model.ConnectionEventType;
import won.protocol.vocabulary.WONMSG;

/**
 * User: syim
 * Date: 02.03.2015
 */
@Component
@FixedMessageProcessor(direction= WONMSG.TYPE_FROM_OWNER_STRING,messageType = WONMSG.TYPE_CLOSE_STRING)
public class CloseMessageFromOwnerProcessor extends AbstractFromOwnerCamelProcessor
{


  public void process(final Exchange exchange) throws Exception {
    Message message = exchange.getIn();
    WonMessage wonMessage = (WonMessage) message.getHeader(WonCamelConstants.WON_MESSAGE_HEADER);

    logger.debug("CLOSE received from the owner side for connection {}", wonMessage.getSenderURI());

    Connection con = dataService.nextConnectionState(wonMessage.getSenderURI(), ConnectionEventType.OWNER_CLOSE);
    //prepare the message to pass to the remote node
    final WonMessage newWonMessage = createMessageToSendToRemoteNode(wonMessage);
    //put it into the 'outbound message' header (so the persister doesn't pick up the wrong one).
    message.setHeader(WonCamelConstants.OUTBOUND_MESSAGE_HEADER, newWonMessage);
  }

  private WonMessage createMessageToSendToRemoteNode(WonMessage wonMessage) {
    //create the message to send to the remote node
    return new WonMessageBuilder()
      .setPropertiesForPassingMessageToRemoteNode(
        wonMessage,
        wonNodeInformationService
          .generateEventURI(wonMessage.getReceiverNodeURI()))
      .build();
  }


}

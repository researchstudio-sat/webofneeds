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
import won.protocol.vocabulary.WONMSG;

import java.net.URI;

/**
 * User: syim
 * Date: 02.03.2015
 */
@Component
@FixedMessageProcessor(direction = WONMSG.TYPE_FROM_OWNER_STRING,messageType = WONMSG.TYPE_OPEN_STRING)
public class OpenMessageFromOwnerProcessor extends AbstractFromOwnerCamelProcessor
{

  public void process(final Exchange exchange) throws Exception {
    Message message = exchange.getIn();
    WonMessage wonMessage = (WonMessage) message.getHeader(WonCamelConstants.MESSAGE_HEADER);
    Connection con = connectionRepository.findOneByConnectionURIForUpdate(wonMessage.getSenderURI());

    //prepare the message to pass to the remote node
    final WonMessage newWonMessage = createMessageToSendToRemoteNode(wonMessage, con);

    //put it into the 'modified message' header (so the persister doesn't pick up the wrong one).
    message.setHeader(WonCamelConstants.OUTBOUND_MESSAGE_HEADER, newWonMessage);
    URI connectionURIFromWonMessage = newWonMessage.getSenderURI();

    logger.debug("OPEN received from the owner side for connection {}", connectionURIFromWonMessage);

    con = dataService.nextConnectionState(connectionURIFromWonMessage, ConnectionEventType.OWNER_OPEN);
    assert con != null;
    assert con.getRemoteNeedURI() != null;
    assert con.getRemoteNeedURI().equals(wonMessage.getReceiverNeedURI());
    assert con.getConnectionURI() != null;
    assert con.getConnectionURI().equals(wonMessage.getSenderURI());
    if (wonMessage.getReceiverURI() != null){
      assert con.getRemoteConnectionURI().equals(wonMessage.getReceiverURI());
    } else {
      con.setRemoteConnectionURI(wonMessage.getReceiverURI());
    }
    con.setState(con.getState().transit(ConnectionEventType.OWNER_OPEN));
    connectionRepository.save(con);

    //add the information about the corresponding message to the local one
    wonMessage.addMessageProperty(WONMSG.HAS_CORRESPONDING_REMOTE_MESSAGE, newWonMessage.getMessageURI());

    //the persister will pick it up later
  }

  private WonMessage createMessageToSendToRemoteNode(WonMessage wonMessage, Connection con) {
    //create the message to send to the remote node
    return WonMessageBuilder
      .setPropertiesForPassingMessageToRemoteNode(
        wonMessage,
        wonNodeInformationService
          .generateEventURI(wonMessage.getReceiverNodeURI()))
      .setSenderURI(con.getConnectionURI())
      .build();
  }

}

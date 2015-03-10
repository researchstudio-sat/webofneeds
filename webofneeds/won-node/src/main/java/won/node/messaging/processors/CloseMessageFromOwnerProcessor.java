package won.node.messaging.processors;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import won.protocol.message.WonEnvelopeType;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.message.WonMessageEncoder;
import won.protocol.model.Connection;
import won.protocol.model.ConnectionEventType;
import won.protocol.model.MessageEventPlaceholder;

/**
 * User: syim
 * Date: 02.03.2015
 */
public class CloseMessageFromOwnerProcessor extends AbstractInOnlyMessageProcessor
{



  public void process(final Exchange exchange) throws Exception {
    Message message = exchange.getIn();
    WonMessage wonMessage = message.getBody(WonMessage.class);

    WonMessage newWonMessage = new WonMessageBuilder()
      .wrap(wonMessage)
      .setTimestamp(System.currentTimeMillis())
      .setWonEnvelopeType(WonEnvelopeType.FROM_NODE)
      .build();
    logger.debug("STORING message with id {}", newWonMessage.getMessageURI());
    rdfStorage.storeDataset(newWonMessage.getMessageURI(),
                                   WonMessageEncoder.encodeAsDataset(newWonMessage));

    logger.debug("CLOSE received from the owner side for connection {}", wonMessage.getSenderURI());

    Connection con = dataService.nextConnectionState(wonMessage.getSenderURI(), ConnectionEventType.OWNER_CLOSE);

    // store newWonMessage and messageEventPlaceholder
    rdfStorage.storeDataset(newWonMessage.getMessageURI(),
                                   WonMessageEncoder.encodeAsDataset(newWonMessage));
    messageEventRepository.save(new MessageEventPlaceholder(con.getConnectionURI(),
                                                            newWonMessage));

    //invoke facet implementation
    //  reg.get(con).closeFromOwner(newWonMessage);
  }
}

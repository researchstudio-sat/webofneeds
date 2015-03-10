package won.node.messaging.processors;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.message.WonMessageEncoder;
import won.protocol.model.Connection;
import won.protocol.model.ConnectionEventType;
import won.protocol.model.MessageEventPlaceholder;

import java.net.URI;

/**
 * User: syim
 * Date: 02.03.2015
 */
public class OpenMessageFromNodeProcessor extends AbstractInOnlyMessageProcessor
{

  public void process(final Exchange exchange) throws Exception {
    Message message = exchange.getIn();
    WonMessage wonMessage = message.getBody(WonMessage.class);
    URI newMessageURI = this.wonNodeInformationService.generateEventURI();
    WonMessage newWonMessage = WonMessageBuilder.copyInboundNodeToNodeMessageAsNodeToOwnerMessage(
      newMessageURI, wonMessage.getReceiverURI(), wonMessage);
    logger.debug("STORING message with id {}", newWonMessage.getMessageURI());
    rdfStorage.storeDataset(newWonMessage.getMessageURI(),
                                   WonMessageEncoder.encodeAsDataset(newWonMessage));

    URI connectionURIFromWonMessage = newWonMessage.getReceiverURI();

    Connection con = dataService.nextConnectionState(connectionURIFromWonMessage,
                                                     ConnectionEventType.PARTNER_OPEN);

    messageEventRepository.save(new MessageEventPlaceholder(
      connectionURIFromWonMessage, newWonMessage));
    //invoke facet implementation
    //reg.get(con).openFromNeed(con, wonMessage.getMessageContent(), newWonMessage);
  }
}

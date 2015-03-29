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

import java.net.URI;

/**
 * User: syim
 * Date: 02.03.2015
 */
@Component
@FixedMessageProcessor(direction= WONMSG.TYPE_FROM_EXTERNAL_STRING,messageType = WONMSG.TYPE_OPEN_STRING)
public class OpenMessageFromNodeProcessor extends AbstractCamelProcessor
{

  public void process(final Exchange exchange) throws Exception {
    Message message = exchange.getIn();
    WonMessage wonMessage = (WonMessage) message.getHeader(WonCamelConstants.MESSAGE_HEADER);
    WonMessage newWonMessage = createMessageToSendToOwner(wonMessage);
    URI connectionURIFromWonMessage = newWonMessage.getReceiverURI();
    Connection con = dataService.nextConnectionState(connectionURIFromWonMessage,
                                                     ConnectionEventType.PARTNER_OPEN);
    exchange.getIn().setHeader(WonCamelConstants.MESSAGE_HEADER,newWonMessage);
  }

  private WonMessage createMessageToSendToOwner(WonMessage wonMessage) {
    //create the message to send to the owner
    return new WonMessageBuilder()
      .setPropertiesForPassingMessageToOwner(wonMessage)
      .build();
  }


}

package won.node.messaging.processors;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.springframework.stereotype.Component;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.vocabulary.WONMSG;

/**
 * User: syim
 * Date: 02.03.2015
 */
@Component
@FixedMessageProcessor(direction= WONMSG.TYPE_FROM_EXTERNAL_STRING,messageType = WONMSG.TYPE_CONNECTION_MESSAGE_STRING)
public class SendMessageFromNodeProcessor extends AbstractCamelProcessor
{

  public void process(final Exchange exchange) throws Exception {
    Message message = exchange.getIn();
    WonMessage wonMessage = (WonMessage) message.getHeader(WonCamelConstants.WON_MESSAGE_HEADER);

    WonMessage newWonMessage = createMessageToSendToOwner(wonMessage);

    exchange.getIn().setHeader(WonCamelConstants.WON_MESSAGE_HEADER,newWonMessage);
  }

  private WonMessage createMessageToSendToOwner(WonMessage wonMessage) {
    //create the message to send to the owner
    return new WonMessageBuilder()
      .setPropertiesForPassingMessageToOwner(wonMessage)
      .build();
  }


}

package won.node.camel.processor.fixed;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.springframework.stereotype.Component;
import won.node.camel.processor.AbstractCamelProcessor;
import won.node.camel.processor.annotation.FixedMessageProcessor;
import won.protocol.exception.IllegalMessageForConnectionStateException;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.message.processor.exception.MissingMessagePropertyException;
import won.protocol.model.Connection;
import won.protocol.model.ConnectionState;
import won.protocol.vocabulary.WONMSG;

import java.net.URI;

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
    WonMessage wonMessage = (WonMessage) message.getHeader(WonCamelConstants.MESSAGE_HEADER);
    URI connectionUri = wonMessage.getReceiverURI();
    if (connectionUri == null){
      throw new MissingMessagePropertyException(URI.create(WONMSG.RECEIVER_PROPERTY.toString()));
    }
    Connection con = connectionRepository.findOneByConnectionURI(connectionUri);
    if (con.getState() != ConnectionState.CONNECTED) {
      throw new IllegalMessageForConnectionStateException(connectionUri, "CONNECTION_MESSAGE", con.getState());
    }
    WonMessage newWonMessage = createMessageToSendToOwner(wonMessage);

    exchange.getIn().setHeader(WonCamelConstants.MESSAGE_HEADER,newWonMessage);
  }

  private WonMessage createMessageToSendToOwner(WonMessage wonMessage) {
    //create the message to send to the owner
    return new WonMessageBuilder()
      .setPropertiesForPassingMessageToOwner(wonMessage)
      .build();
  }


}

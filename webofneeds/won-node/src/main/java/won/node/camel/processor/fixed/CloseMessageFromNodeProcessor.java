package won.node.camel.processor.fixed;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import won.node.camel.processor.AbstractCamelProcessor;
import won.node.camel.processor.annotation.FixedMessageProcessor;
import won.protocol.message.WonMessage;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.model.ConnectionEventType;
import won.protocol.vocabulary.WONMSG;

import java.net.URI;

/**
 * User: syim
 * Date: 02.03.2015
 */
@Component
@FixedMessageProcessor(
        direction = WONMSG.TYPE_FROM_EXTERNAL_STRING,
        messageType = WONMSG.TYPE_CLOSE_STRING)
public class CloseMessageFromNodeProcessor extends AbstractCamelProcessor
{

  @Transactional(propagation = Propagation.REQUIRED)
  public void process(final Exchange exchange) throws Exception {
    Message message = exchange.getIn();
    WonMessage wonMessage = (WonMessage) message.getHeader(WonCamelConstants.MESSAGE_HEADER);
    URI connectionURIFromWonMessage = wonMessage.getReceiverURI();
    dataService.nextConnectionState(
      connectionURIFromWonMessage, ConnectionEventType.PARTNER_CLOSE);
  }


}

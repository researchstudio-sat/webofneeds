package won.node.camel.processor.fixed;

import java.net.URI;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.springframework.stereotype.Component;

import won.node.camel.processor.AbstractCamelProcessor;
import won.node.camel.processor.annotation.FixedMessageProcessor;
import won.protocol.message.WonMessage;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.model.Need;
import won.protocol.model.NeedState;
import won.protocol.util.DataAccessUtils;
import won.protocol.vocabulary.WONMSG;

/**
 * User: syim
 * Date: 02.03.2015
 */
@Component
@FixedMessageProcessor(
        direction = WONMSG.TYPE_FROM_OWNER_STRING,
        messageType = WONMSG.TYPE_ACTIVATE_STRING)
public class ActivateNeedMessageProcessor extends AbstractCamelProcessor
{

  public void process(Exchange exchange) throws Exception {
    Message message = exchange.getIn();
    WonMessage wonMessage = (WonMessage) message.getHeader(WonCamelConstants.MESSAGE_HEADER);
    URI receiverNeedURI = wonMessage.getReceiverNeedURI();
    logger.debug("ACTIVATING need. needURI:{}", receiverNeedURI);
    if (receiverNeedURI == null) throw new IllegalArgumentException("receiverNeedURI is not set");
    Need need = DataAccessUtils.loadNeed(needRepository, receiverNeedURI);
    need.getEventContainer().getEvents().add(messageEventRepository.findOneByMessageURIforUpdate(wonMessage.getMessageURI()));
    need.setState(NeedState.ACTIVE);
    logger.debug("Setting Need State: " + need.getState());
    needRepository.save(need);
  }

}

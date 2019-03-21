package won.node.camel.processor.fixed;

import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import won.node.camel.processor.AbstractCamelProcessor;
import won.node.camel.processor.annotation.FixedMessageProcessor;
import won.protocol.message.WonMessage;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.message.processor.exception.WonMessageProcessingException;
import won.protocol.model.Need;
import won.protocol.model.NeedState;
import won.protocol.util.DataAccessUtils;
import won.protocol.vocabulary.WONMSG;

import java.net.URI;

/**
 * User: syim Date: 02.03.2015
 */
@Component
@FixedMessageProcessor(direction = WONMSG.TYPE_FROM_OWNER_STRING, messageType = WONMSG.TYPE_DEACTIVATE_STRING)
public class DeactivateNeedMessageProcessor extends AbstractCamelProcessor {
  Logger logger = LoggerFactory.getLogger(this.getClass());

  public void process(final Exchange exchange) throws Exception {
    WonMessage wonMessage = (WonMessage) exchange.getIn().getHeader(WonCamelConstants.MESSAGE_HEADER);
    URI receiverNeedURI = wonMessage.getReceiverNeedURI();
    logger.debug("DEACTIVATING need. needURI:{}", receiverNeedURI);
    if (receiverNeedURI == null)
      throw new WonMessageProcessingException("receiverNeedURI is not set");
    Need need = DataAccessUtils.loadNeed(needRepository, receiverNeedURI);
    need.getEventContainer().getEvents()
        .add(messageEventRepository.findOneByMessageURIforUpdate(wonMessage.getMessageURI()));
    need.setState(NeedState.INACTIVE);
    need = needRepository.save(need);
  }

}

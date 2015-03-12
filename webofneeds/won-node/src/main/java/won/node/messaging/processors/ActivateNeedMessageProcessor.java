package won.node.messaging.processors;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.springframework.stereotype.Component;
import won.node.annotation.FixedMessageProcessor;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.message.WonMessageEncoder;
import won.protocol.model.MessageEventPlaceholder;
import won.protocol.model.Need;
import won.protocol.model.NeedState;
import won.protocol.util.DataAccessUtils;
import won.protocol.vocabulary.WONMSG;

import java.net.URI;

/**
 * User: syim
 * Date: 02.03.2015
 */
@Component
@FixedMessageProcessor(direction = WONMSG.TYPE_FROM_OWNER_STRING,messageType = WONMSG.TYPE_ACTIVATE_STRING)
public class ActivateNeedMessageProcessor extends AbstractInOnlyMessageProcessor
{


  public void process(Exchange exchange) throws Exception {
    Message message = exchange.getIn();
    WonMessage wonMessage = message.getBody(WonMessage.class);
    WonMessage newWonMessage =  WonMessageBuilder.wrapOutboundOwnerToNodeOrSystemMessageAsNodeToNodeMessage(
      wonMessage);
    logger.debug("STORING message with id {}", newWonMessage.getMessageURI());
    rdfStorage.storeDataset(newWonMessage.getMessageURI(),
                            WonMessageEncoder.encodeAsDataset(newWonMessage));

    URI receiverNeedURI = newWonMessage.getReceiverNeedURI();
    logger.debug("ACTIVATING need. needURI:{}", receiverNeedURI);
    if (receiverNeedURI == null) throw new IllegalArgumentException("receiverNeedURI is not set");
    Need need = DataAccessUtils.loadNeed(needRepository, receiverNeedURI);
    need.setState(NeedState.ACTIVE);
    logger.debug("Setting Need State: " + need.getState());
    needRepository.save(need);
    messageEventRepository.save(new MessageEventPlaceholder(need.getNeedURI(), newWonMessage));

    matcherProtocolMatcherClient.needActivated(need.getNeedURI(), newWonMessage);
  }
}

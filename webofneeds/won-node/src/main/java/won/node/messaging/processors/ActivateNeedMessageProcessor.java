package won.node.messaging.processors;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import won.node.protocol.MatcherProtocolMatcherServiceClientSide;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.message.WonMessageEncoder;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.model.MessageEventPlaceholder;
import won.protocol.model.Need;
import won.protocol.model.NeedState;
import won.protocol.repository.MessageEventRepository;
import won.protocol.repository.NeedRepository;
import won.protocol.repository.rdfstorage.RDFStorageService;
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

  @Autowired
  RDFStorageService rdfStorage;

  @Autowired
  NeedRepository needRepository;

  @Autowired
  MessageEventRepository messageEventRepository;

  @Autowired
  MatcherProtocolMatcherServiceClientSide matcherProtocolMatcherClient;


  public void process(Exchange exchange) throws Exception {
    Message message = exchange.getIn();
    WonMessage wonMessage = (WonMessage) message.getHeader(WonCamelConstants.WON_MESSAGE_EXCHANGE_HEADER);
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
  public void setRdfStorage(final RDFStorageService rdfStorage) {
    this.rdfStorage = rdfStorage;
  }

  public void setNeedRepository(final NeedRepository needRepository) {
    this.needRepository = needRepository;
  }

  public void setMessageEventRepository(final MessageEventRepository messageEventRepository) {
    this.messageEventRepository = messageEventRepository;
  }

  public void setMatcherProtocolMatcherClient(final MatcherProtocolMatcherServiceClientSide matcherProtocolMatcherClient) {
    this.matcherProtocolMatcherClient = matcherProtocolMatcherClient;
  }

}

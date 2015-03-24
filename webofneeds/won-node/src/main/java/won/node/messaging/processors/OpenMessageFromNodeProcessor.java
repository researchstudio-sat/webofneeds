package won.node.messaging.processors;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import won.node.service.DataAccessService;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.message.WonMessageEncoder;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.model.Connection;
import won.protocol.model.ConnectionEventType;
import won.protocol.model.MessageEventPlaceholder;
import won.protocol.repository.MessageEventRepository;
import won.protocol.repository.rdfstorage.RDFStorageService;
import won.protocol.service.WonNodeInformationService;
import won.protocol.vocabulary.WONMSG;

import java.net.URI;

/**
 * User: syim
 * Date: 02.03.2015
 */
@Component
@FixedMessageProcessor(direction= WONMSG.TYPE_FROM_EXTERNAL_STRING,messageType = WONMSG.TYPE_OPEN_STRING)
public class OpenMessageFromNodeProcessor extends AbstractInOnlyMessageProcessor
{

  @Autowired
  WonNodeInformationService wonNodeInformationService;

  @Autowired
  RDFStorageService rdfStorage;

  @Autowired
  DataAccessService dataService;

  @Autowired
  MessageEventRepository messageEventRepository;

  public void process(final Exchange exchange) throws Exception {
    Message message = exchange.getIn();
    WonMessage wonMessage = (WonMessage) message.getHeader(WonCamelConstants.WON_MESSAGE_HEADER);
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

    exchange.getIn().setHeader(WonCamelConstants.WON_MESSAGE_HEADER,newWonMessage);
    //invoke facet implementation
    //reg.get(con).openFromNeed(con, wonMessage.getMessageContent(), newWonMessage);
  }

  public void setWonNodeInformationService(final WonNodeInformationService wonNodeInformationService) {
    this.wonNodeInformationService = wonNodeInformationService;
  }

  public void setRdfStorage(final RDFStorageService rdfStorage) {
    this.rdfStorage = rdfStorage;
  }

  public void setDataService(final DataAccessService dataService) {
    this.dataService = dataService;
  }

  public void setMessageEventRepository(final MessageEventRepository messageEventRepository) {
    this.messageEventRepository = messageEventRepository;
  }
}

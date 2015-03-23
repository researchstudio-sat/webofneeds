package won.node.messaging.processors;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import won.node.service.DataAccessService;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.message.WonMessageEncoder;
import won.protocol.model.Connection;
import won.protocol.model.MessageEventPlaceholder;
import won.protocol.repository.ConnectionRepository;
import won.protocol.repository.MessageEventRepository;
import won.protocol.repository.rdfstorage.RDFStorageService;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.DataAccessUtils;
import won.protocol.vocabulary.WONMSG;

import java.net.URI;

/**
 * User: syim
 * Date: 02.03.2015
 */
@Component
@FixedMessageProcessor(direction= WONMSG.TYPE_FROM_EXTERNAL_STRING,messageType = WONMSG.TYPE_CONNECTION_MESSAGE_STRING)
public class SendMessageFromNodeProcessor extends AbstractInOnlyMessageProcessor
{
  private final Logger logger = LoggerFactory.getLogger(getClass());


  @Autowired
  ConnectionRepository connectionRepository;

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
    WonMessage wonMessage = (WonMessage) message.getHeader("wonMessage");
    URI newMessageURI = this.wonNodeInformationService.generateEventURI();
    WonMessage newWonMessage = WonMessageBuilder.copyInboundNodeToNodeMessageAsNodeToOwnerMessage(
      newMessageURI, wonMessage.getReceiverURI(), wonMessage);
    logger.debug("STORING message with id {}", newWonMessage.getMessageURI());
    rdfStorage.storeDataset(newWonMessage.getMessageURI(),
                                   WonMessageEncoder.encodeAsDataset(newWonMessage));

    URI connectionURIFromWonMessage = newWonMessage.getReceiverURI();

    Connection con = DataAccessUtils.loadConnection(
      connectionRepository, connectionURIFromWonMessage);

    messageEventRepository.save(new MessageEventPlaceholder(
      connectionURIFromWonMessage, newWonMessage));

    exchange.getIn().setHeader("wonMessage",newWonMessage);
    //invoke facet implementation
    // reg.get(con).sendMessageFromNeed(con, message, newWonMessage);
  }

  public void setConnectionRepository(final ConnectionRepository connectionRepository) {
    this.connectionRepository = connectionRepository;
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

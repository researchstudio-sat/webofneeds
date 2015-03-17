package won.node.messaging.processors;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import won.node.annotation.FixedMessageProcessor;
import won.node.service.DataAccessService;
import won.protocol.message.WonMessageDirection;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.message.WonMessageEncoder;
import won.protocol.model.Connection;
import won.protocol.model.ConnectionEventType;
import won.protocol.model.MessageEventPlaceholder;
import won.protocol.repository.MessageEventRepository;
import won.protocol.repository.rdfstorage.RDFStorageService;
import won.protocol.vocabulary.WONMSG;

/**
 * User: syim
 * Date: 02.03.2015
 */
@Component
@FixedMessageProcessor(direction= WONMSG.TYPE_FROM_OWNER_STRING,messageType = WONMSG.TYPE_CLOSE_STRING)
public class CloseMessageFromOwnerProcessor extends AbstractInOnlyMessageProcessor
{

  @Autowired
  RDFStorageService rdfStorage;

  @Autowired
  DataAccessService dataService;

  @Autowired
  MessageEventRepository messageEventRepository;

  public void process(final Exchange exchange) throws Exception {
    Message message = exchange.getIn();
    WonMessage wonMessage = message.getBody(WonMessage.class);

    WonMessage newWonMessage = new WonMessageBuilder()
      .wrap(wonMessage)
      .setTimestamp(System.currentTimeMillis())
      .setWonMessageDirection(WonMessageDirection.FROM_EXTERNAL)
      .build();
    logger.debug("STORING message with id {}", newWonMessage.getMessageURI());
    rdfStorage.storeDataset(newWonMessage.getMessageURI(),
                                   WonMessageEncoder.encodeAsDataset(newWonMessage));

    logger.debug("CLOSE received from the owner side for connection {}", wonMessage.getSenderURI());

    Connection con = dataService.nextConnectionState(wonMessage.getSenderURI(), ConnectionEventType.OWNER_CLOSE);

    // store newWonMessage and messageEventPlaceholder
    rdfStorage.storeDataset(newWonMessage.getMessageURI(),
                                   WonMessageEncoder.encodeAsDataset(newWonMessage));
    messageEventRepository.save(new MessageEventPlaceholder(con.getConnectionURI(),
                                                            newWonMessage));

    exchange.getIn().setHeader("wonMessage",newWonMessage);


    //invoke facet implementation
    //  reg.get(con).closeFromOwner(newWonMessage);
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

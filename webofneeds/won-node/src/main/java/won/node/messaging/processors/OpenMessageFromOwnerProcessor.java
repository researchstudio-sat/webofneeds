package won.node.messaging.processors;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import won.node.annotation.FixedMessageProcessor;
import won.node.service.DataAccessService;
import won.protocol.message.WonEnvelopeType;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.message.WonMessageEncoder;
import won.protocol.model.Connection;
import won.protocol.model.ConnectionEventType;
import won.protocol.model.MessageEventPlaceholder;
import won.protocol.repository.ConnectionRepository;
import won.protocol.repository.MessageEventRepository;
import won.protocol.repository.rdfstorage.RDFStorageService;
import won.protocol.vocabulary.WONMSG;

import java.net.URI;

/**
 * User: syim
 * Date: 02.03.2015
 */
@Component
@FixedMessageProcessor(direction = WONMSG.TYPE_FROM_OWNER_STRING,messageType = WONMSG.TYPE_OPEN_STRING)
public class OpenMessageFromOwnerProcessor extends AbstractInOnlyMessageProcessor
{

  @Autowired
  ConnectionRepository connectionRepository;

  @Autowired
  RDFStorageService rdfStorage;

  @Autowired
  DataAccessService dataService;

  @Autowired
  MessageEventRepository messageEventRepository;
  public void process(final Exchange exchange) throws Exception {
    Message message = exchange.getIn();
    WonMessage wonMessage = message.getBody(WonMessage.class);
    Connection con = connectionRepository.findOneByConnectionURI(wonMessage.getSenderURI());

    logger.debug("STORING message with id {}", wonMessage.getMessageURI());
    //TODO: which properties are really needed to route the messae correctly?
    WonMessage newWonMessage = new WonMessageBuilder()
      .wrap(wonMessage)
      .setTimestamp(System.currentTimeMillis())
      .setSenderURI(con.getConnectionURI())
      .setReceiverURI(con.getRemoteConnectionURI())
      .setReceiverNeedURI(con.getRemoteNeedURI())
      .setWonEnvelopeType(WonEnvelopeType.FROM_NODE)
      .build();

    rdfStorage.storeDataset(newWonMessage.getMessageURI(),
                                   WonMessageEncoder.encodeAsDataset(newWonMessage));

    URI connectionURIFromWonMessage = newWonMessage.getSenderURI();

    logger.debug("OPEN received from the owner side for connection {}", connectionURIFromWonMessage);

    con = dataService.nextConnectionState(connectionURIFromWonMessage, ConnectionEventType.OWNER_OPEN);

    messageEventRepository.save(new MessageEventPlaceholder(connectionURIFromWonMessage,
                                                            newWonMessage));

    //invoke facet implementation
    //reg.get(con).openFromOwner(con, content, newWonMessage);
  }

  public void setConnectionRepository(final ConnectionRepository connectionRepository) {
    this.connectionRepository = connectionRepository;
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

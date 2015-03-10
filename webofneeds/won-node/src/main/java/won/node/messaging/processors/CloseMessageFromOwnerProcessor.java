package won.node.messaging.processors;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import won.node.facet.impl.FacetRegistry;
import won.node.service.DataAccessService;
import won.protocol.message.*;
import won.protocol.model.Connection;
import won.protocol.model.ConnectionEventType;
import won.protocol.model.MessageEventPlaceholder;
import won.protocol.repository.ConnectionRepository;
import won.protocol.repository.MessageEventRepository;
import won.protocol.repository.rdfstorage.RDFStorageService;

/**
 * User: syim
 * Date: 02.03.2015
 */
public class CloseMessageFromOwnerProcessor implements WonMessageProcessor
{
  private final Logger logger = LoggerFactory.getLogger(getClass());
  private FacetRegistry reg;
  private DataAccessService dataService;
  @Autowired
  private ConnectionRepository connectionRepository;
  @Autowired
  private won.node.service.impl.URIService URIService;
  @Autowired
  private RDFStorageService rdfStorageService;
  @Autowired
  private MessageEventRepository messageEventRepository;



  public void process(final Exchange exchange) throws Exception {
    Message message = exchange.getIn();
    WonMessage wonMessage = message.getBody(WonMessage.class);

    WonMessage newWonMessage = new WonMessageBuilder()
      .wrap(wonMessage)
      .setTimestamp(System.currentTimeMillis())
      .setWonEnvelopeType(WonEnvelopeType.FROM_NODE)
      .build();
    logger.debug("STORING message with id {}", newWonMessage.getMessageURI());
    rdfStorageService.storeDataset(newWonMessage.getMessageURI(),
                                   WonMessageEncoder.encodeAsDataset(newWonMessage));

    logger.debug("CLOSE received from the owner side for connection {}", wonMessage.getSenderURI());

    Connection con = dataService.nextConnectionState(wonMessage.getSenderURI(), ConnectionEventType.OWNER_CLOSE);

    // store newWonMessage and messageEventPlaceholder
    rdfStorageService.storeDataset(newWonMessage.getMessageURI(),
                                   WonMessageEncoder.encodeAsDataset(newWonMessage));
    messageEventRepository.save(new MessageEventPlaceholder(con.getConnectionURI(),
                                                            newWonMessage));

    //invoke facet implementation
    //  reg.get(con).closeFromOwner(newWonMessage);
  }
}

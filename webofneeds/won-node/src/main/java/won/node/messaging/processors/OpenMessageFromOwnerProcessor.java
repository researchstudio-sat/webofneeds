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

import java.net.URI;

/**
 * User: syim
 * Date: 02.03.2015
 */
public class OpenMessageFromOwnerProcessor implements WonMessageProcessor
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

    rdfStorageService.storeDataset(newWonMessage.getMessageURI(),
                                   WonMessageEncoder.encodeAsDataset(newWonMessage));

    URI connectionURIFromWonMessage = newWonMessage.getSenderURI();

    logger.debug("OPEN received from the owner side for connection {}", connectionURIFromWonMessage);

    con = dataService.nextConnectionState(connectionURIFromWonMessage, ConnectionEventType.OWNER_OPEN);

    messageEventRepository.save(new MessageEventPlaceholder(connectionURIFromWonMessage,
                                                            newWonMessage));

    //invoke facet implementation
    //reg.get(con).openFromOwner(con, content, newWonMessage);
  }
}

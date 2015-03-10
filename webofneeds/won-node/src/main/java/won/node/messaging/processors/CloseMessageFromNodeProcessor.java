package won.node.messaging.processors;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import won.node.facet.impl.FacetRegistry;
import won.node.service.DataAccessService;
import won.node.service.impl.URIService;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.message.WonMessageEncoder;
import won.protocol.message.WonMessageProcessor;
import won.protocol.model.Connection;
import won.protocol.model.ConnectionEventType;
import won.protocol.model.MessageEventPlaceholder;
import won.protocol.repository.*;
import won.protocol.repository.rdfstorage.RDFStorageService;
import won.protocol.service.WonNodeInformationService;

import java.net.URI;
import java.util.concurrent.ExecutorService;

/**
 * User: syim
 * Date: 02.03.2015
 */
public class CloseMessageFromNodeProcessor implements WonMessageProcessor
{
  private final Logger logger = LoggerFactory.getLogger(getClass());
  private FacetRegistry reg;
  private DataAccessService dataService;

  private ExecutorService executorService;

  @Autowired
  private ConnectionRepository connectionRepository;
  @Autowired
  private EventRepository eventRepository;
  @Autowired
  private RDFStorageService rdfStorageService;
  @Autowired
  private OwnerApplicationRepository ownerApplicationRepository;
  @Autowired
  private NeedRepository needRepository;
  @Autowired
  private URIService uriService;
  @Autowired
  private MessageEventRepository messageEventRepository;
  @Autowired
  private WonNodeInformationService wonNodeInformationService;


  public void process(final Exchange exchange) throws Exception {
    Message message = exchange.getIn();
    WonMessage wonMessage = message.getBody(WonMessage.class);
    URI newMessageURI = this.wonNodeInformationService.generateEventURI();

    WonMessage newWonMessage = WonMessageBuilder.copyInboundNodeToNodeMessageAsNodeToOwnerMessage(
      newMessageURI, wonMessage.getReceiverURI(), wonMessage);
    logger.debug("STORING message with id {}", newWonMessage.getMessageURI());
    rdfStorageService.storeDataset(newWonMessage.getMessageURI(),
                                   WonMessageEncoder.encodeAsDataset(newWonMessage));

    URI connectionURIFromWonMessage = newWonMessage.getReceiverURI();
    Connection con = dataService.nextConnectionState(
      connectionURIFromWonMessage, ConnectionEventType.PARTNER_CLOSE);

    messageEventRepository.save(new MessageEventPlaceholder(
      connectionURIFromWonMessage, newWonMessage));

    //invoke facet implementation


    //reg.get(con).closeFromNeed(con, wonMessage.getMessageContent(), newWonMessage);
  }
}

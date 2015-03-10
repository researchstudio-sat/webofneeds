package won.node.messaging.processors;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import won.node.facet.impl.FacetRegistry;
import won.node.service.DataAccessService;
import won.node.service.impl.NeedCommunicationServiceImpl;
import won.node.service.impl.NeedFacingConnectionCommunicationServiceImpl;
import won.node.service.impl.OwnerFacingConnectionCommunicationServiceImpl;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.message.WonMessageProcessor;
import won.protocol.model.Connection;
import won.protocol.model.ConnectionEventType;
import won.protocol.model.ConnectionState;
import won.protocol.model.MessageEventPlaceholder;
import won.protocol.need.NeedProtocolNeedService;
import won.protocol.owner.OwnerProtocolOwnerService;
import won.protocol.repository.ConnectionRepository;
import won.protocol.repository.EventRepository;
import won.protocol.repository.MessageEventRepository;
import won.protocol.repository.NeedRepository;
import won.protocol.repository.rdfstorage.RDFStorageService;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.WonRdfUtils;

import java.net.URI;
import java.util.concurrent.ExecutorService;

/**
 * User: syim
 * Date: 02.03.2015
 */
public class ConnectMessageFromOwnerProcessor implements WonMessageProcessor
{
  final Logger logger = LoggerFactory.getLogger(NeedCommunicationServiceImpl.class);
  private FacetRegistry reg;
  private DataAccessService dataService;

  /**
   * Client talking to the owner side via the owner protocol
   */
  private OwnerProtocolOwnerService ownerProtocolOwnerService;
  /**
   * Client talking another need via the need protocol
   */
  private NeedProtocolNeedService needProtocolNeedService;

  /**
   * Client talking to this need service from the need side
   */
  private NeedFacingConnectionCommunicationServiceImpl needFacingConnectionCommunicationService;

  /**
   * Client talking to this need service from the owner side
   */
  private OwnerFacingConnectionCommunicationServiceImpl ownerFacingConnectionCommunicationService;

  private won.node.service.impl.URIService URIService;

  private ExecutorService executorService;

  @Autowired
  private NeedRepository needRepository;
  @Autowired
  private ConnectionRepository connectionRepository;
  @Autowired
  private EventRepository eventRepository;
  @Autowired
  private RDFStorageService rdfStorageService;
  @Autowired
  private MessageEventRepository messageEventRepository;
  @Autowired
  private WonNodeInformationService wonNodeInformationService;


  public void process(final Exchange exchange) throws Exception {
    Message message = exchange.getIn();
    WonMessage wonMessage = message.getBody(WonMessage.class);
    URI senderNeedURI = wonMessage.getSenderNeedURI();
    URI receiverNeedURI = wonMessage.getReceiverNeedURI();

    //TODO: when we introduce dedicated URIs for individual facets, this will be how
    URI facetURI = WonRdfUtils.FacetUtils.getFacet(wonMessage.getMessageContent());

    //create Connection in Database
    final Connection con =  dataService.createConnection(senderNeedURI, receiverNeedURI, null, facetURI,
                                                         ConnectionState.REQUEST_SENT,
                                                         ConnectionEventType.OWNER_OPEN);

    // add the connectionID to the wonMessage
    final WonMessage newWonMessage = WonMessageBuilder.wrapOutboundOwnerToNodeOrSystemMessageAsNodeToNodeMessage(
      con.getConnectionURI(),
      wonMessage);

    messageEventRepository.save(
      new MessageEventPlaceholder(con.getConnectionURI(), newWonMessage));
    logger.debug("STORING message with id {}", newWonMessage.getMessageURI());
    rdfStorageService.storeDataset(newWonMessage.getMessageURI(),
                                   newWonMessage.getCompleteDataset());

    //invoke facet implementation
    //Facet facet = reg.get(con);
    //facet.connectFromOwner(con, content, newWonMessage);
    //reg.get(con).connectFromOwner(con, content);

    //return con.getConnectionURI();
  }
}

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
public class ConnectMessageFromNodeProcessor implements WonMessageProcessor
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
    // a need wants to connect.
    // get the required data from the message and create a connection
    URI needURIFromWonMessage = wonMessage.getReceiverNeedURI();
    URI otherNeedURIFromWonMessage = wonMessage.getSenderNeedURI();
    URI otherConnectionURIFromWonMessage = wonMessage.getSenderURI();
    URI facetURI = WonRdfUtils.FacetUtils.getRemoteFacet(wonMessage.getMessageURI(),
                                                         wonMessage.getMessageContent());


    logger.debug("CONNECT received for need {} referring to need {} (connection {})",
                 new Object[]{needURIFromWonMessage,
                              otherNeedURIFromWonMessage,
                              otherConnectionURIFromWonMessage});
    if (otherConnectionURIFromWonMessage == null) throw new IllegalArgumentException("otherConnectionURI is not set");

    //create Connection in Database
    Connection con = dataService.createConnection(needURIFromWonMessage,
                                                  otherNeedURIFromWonMessage,
                                                  otherConnectionURIFromWonMessage,
                                                  facetURI,
                                                  ConnectionState.REQUEST_RECEIVED, ConnectionEventType.PARTNER_OPEN);
    // copy the message envelope
    // information about the newly created connection
    // to the message and pass it on to the owner.
    URI wrappedMessageURI = this.wonNodeInformationService.generateEventURI();
    WonMessage wrappedMessage  =  WonMessageBuilder
      .copyInboundNodeToNodeMessageAsNodeToOwnerMessage(wrappedMessageURI, con.getConnectionURI(), wonMessage);


    rdfStorageService.storeDataset(wrappedMessageURI, wrappedMessage.getCompleteDataset());

    messageEventRepository.save(new MessageEventPlaceholder(
      con.getConnectionURI(), wrappedMessage));

    //invoke facet implementation
    //Facet facet = reg.get(con);
    // send an empty model until we remove this parameter
    //facet.connectFromNeed(con, content, wrappedMessage);

    //return con.getConnectionURI();
  }
}

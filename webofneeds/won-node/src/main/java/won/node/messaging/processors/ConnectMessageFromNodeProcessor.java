package won.node.messaging.processors;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import won.node.service.impl.NeedCommunicationServiceImpl;
import won.node.service.impl.NeedFacingConnectionCommunicationServiceImpl;
import won.node.service.impl.OwnerFacingConnectionCommunicationServiceImpl;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.model.Connection;
import won.protocol.model.ConnectionEventType;
import won.protocol.model.ConnectionState;
import won.protocol.need.NeedProtocolNeedService;
import won.protocol.owner.OwnerProtocolOwnerService;
import won.protocol.repository.ConnectionRepository;
import won.protocol.repository.EventRepository;
import won.protocol.repository.MessageEventRepository;
import won.protocol.repository.NeedRepository;
import won.protocol.repository.rdfstorage.RDFStorageService;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.WonRdfUtils;
import won.protocol.vocabulary.WONMSG;

import java.net.URI;
import java.util.concurrent.ExecutorService;

/**
 * User: syim
 * Date: 02.03.2015
 */
@Component
@FixedMessageProcessor(direction= WONMSG.TYPE_FROM_EXTERNAL_STRING,messageType = WONMSG.TYPE_CONNECT_STRING)
public class ConnectMessageFromNodeProcessor extends AbstractInOnlyMessageProcessor
{
  final Logger logger = LoggerFactory.getLogger(NeedCommunicationServiceImpl.class);

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
    WonMessage wonMessage = (WonMessage) message.getHeader(WonCamelConstants.WON_MESSAGE_EXCHANGE_HEADER);
    // a need wants to connect.
    // get the required data from the message and create a connection
    URI needURIFromWonMessage = wonMessage.getReceiverNeedURI();
    URI otherNeedURIFromWonMessage = wonMessage.getSenderNeedURI();
    URI otherConnectionURIFromWonMessage = wonMessage.getSenderURI();
    URI facetURI = WonRdfUtils.FacetUtils.getRemoteFacet(wonMessage);


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

    exchange.getIn().setHeader(WonCamelConstants.WON_MESSAGE_EXCHANGE_HEADER, wonMessage);
    //send response
    WonMessage successResponseMessage = makeSuccessResponseMessage(wonMessage, con);
    sendMessageToNode(successResponseMessage, wonMessage.getReceiverNeedURI(), wonMessage.getSenderNeedURI());
  }

  private WonMessage makeSuccessResponseMessage(WonMessage originalMessage, Connection con) {
    WonMessageBuilder wonMessageBuilder = new WonMessageBuilder();
    wonMessageBuilder.setPropertiesForNodeResponse(
            originalMessage,
            true,
            this.wonNodeInformationService.generateEventURI());
    //hack(?): set sender to the new connection uri to communicate it to the original sender.
    // alternative: use a dedicated property (msg:newConnectionUri [uri]) in the message
    wonMessageBuilder.setSenderURI(con.getConnectionURI());
    return wonMessageBuilder.build();
  }

}

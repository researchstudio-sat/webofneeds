package won.node.messaging.processors;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.springframework.stereotype.Component;
import won.node.annotation.FixedMessageProcessor;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.model.Connection;
import won.protocol.model.ConnectionEventType;
import won.protocol.model.ConnectionState;
import won.protocol.model.MessageEventPlaceholder;
import won.protocol.util.WonRdfUtils;
import won.protocol.vocabulary.WONMSG;

import java.net.URI;

/**
 * User: syim
 * Date: 02.03.2015
 */
@Component
@FixedMessageProcessor(direction= WONMSG.TYPE_FROM_OWNER_STRING,messageType = WONMSG.TYPE_CONNECT_STRING)
public class ConnectMessageFromOwnerProcessor extends AbstractInOnlyMessageProcessor
{


  public void process(final Exchange exchange) throws Exception {
    Message message = exchange.getIn();
    WonMessage wonMessage = message.getBody(WonMessage.class);
    URI senderNeedURI = wonMessage.getSenderNeedURI();
    URI receiverNeedURI = wonMessage.getReceiverNeedURI();

    //TODO: when we introduce dedicated URIs for individual facets, this will be how
    URI facetURI = WonRdfUtils.FacetUtils.getFacet(wonMessage);

    //create Connection in Database
    final Connection con = dataService.createConnection(senderNeedURI, receiverNeedURI, null, facetURI,
                                                        ConnectionState.REQUEST_SENT,
                                                        ConnectionEventType.OWNER_OPEN);

    // add the connectionID to the wonMessage
    final WonMessage newWonMessage = WonMessageBuilder.wrapOutboundOwnerToNodeOrSystemMessageAsNodeToNodeMessage(
      con.getConnectionURI(),
      wonMessage);

    messageEventRepository.save(
      new MessageEventPlaceholder(con.getConnectionURI(), newWonMessage));
    logger.debug("STORING message with id {}", newWonMessage.getMessageURI());
    rdfStorage.storeDataset(newWonMessage.getMessageURI(),
                            newWonMessage.getCompleteDataset());



  }
}

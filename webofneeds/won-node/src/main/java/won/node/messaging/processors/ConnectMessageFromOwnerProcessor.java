package won.node.messaging.processors;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.model.Connection;
import won.protocol.model.ConnectionEventType;
import won.protocol.model.ConnectionState;
import won.protocol.model.MessageEventPlaceholder;
import won.protocol.util.WonRdfUtils;

import java.net.URI;

/**
 * User: syim
 * Date: 02.03.2015
 */
public class ConnectMessageFromOwnerProcessor extends AbstractInOutMessageProcessor
{


  public Object process(final Exchange exchange) throws Exception {
    Message message = exchange.getIn();
    WonMessage wonMessage = message.getBody(WonMessage.class);
    URI senderNeedURI = wonMessage.getSenderNeedURI();
    URI receiverNeedURI = wonMessage.getReceiverNeedURI();

    //TODO: when we introduce dedicated URIs for individual facets, this will be how
    URI facetURI = WonRdfUtils.FacetUtils.getFacet(wonMessage.getMessageContent());

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

    //invoke facet implementation
    //Facet facet = reg.get(con);
    //facet.connectFromOwner(con, content, newWonMessage);
    //reg.get(con).connectFromOwner(con, content);

    return con.getConnectionURI();
  }
}

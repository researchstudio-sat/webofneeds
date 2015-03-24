package won.node.messaging.processors;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.springframework.stereotype.Component;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.model.Connection;
import won.protocol.model.ConnectionEventType;
import won.protocol.model.ConnectionState;
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
    WonMessage wonMessage = (WonMessage) message.getHeader(WonCamelConstants.WON_MESSAGE_HEADER);
    URI senderNeedURI = wonMessage.getSenderNeedURI();
    URI receiverNeedURI = wonMessage.getReceiverNeedURI();
    URI facetURI = WonRdfUtils.FacetUtils.getFacet(wonMessage);
    //create Connection in Database
    final Connection con = dataService.createConnection(senderNeedURI, receiverNeedURI, null, facetURI,
                                                        ConnectionState.REQUEST_SENT,
                                                        ConnectionEventType.OWNER_OPEN);

    //add the information about the new local connection to the original message
    wonMessage = new WonMessageBuilder().wrap(wonMessage).setSenderURI(con.getConnectionURI()).build();
    //put it into the header so the persister will pick it up later
    message.setHeader(WonCamelConstants.WON_MESSAGE_HEADER,wonMessage);
    //prepare the message to pass to the remote node
    final WonMessage newWonMessage = createMessageToSendToRemoteNode(wonMessage, con);
    //put it into the 'modified message' header (so the persister doesn't pick up the wrong one).
    exchange.getIn().setHeader(WonCamelConstants.OUTBOUND_MESSAGE_HEADER, newWonMessage);
  }

  private WonMessage createMessageToSendToRemoteNode(WonMessage wonMessage, Connection con) {
    //create the message to send to the remote node
    return new WonMessageBuilder()
            .setPropertiesForPassingMessageToRemoteNode(
                    wonMessage,
                    wonNodeInformationService
                            .generateEventURI(wonMessage.getReceiverNodeURI()))
            .setSenderURI(con.getConnectionURI())
            .build();
  }

}

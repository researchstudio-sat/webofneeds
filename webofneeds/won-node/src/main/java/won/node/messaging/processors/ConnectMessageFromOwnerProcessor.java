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
    WonMessage wonMessage = (WonMessage) message.getHeader(WonCamelConstants.WON_MESSAGE_EXCHANGE_HEADER);
    URI senderNeedURI = wonMessage.getSenderNeedURI();
    URI receiverNeedURI = wonMessage.getReceiverNeedURI();
    URI facetURI = WonRdfUtils.FacetUtils.getFacet(wonMessage);
    //create Connection in Database
    final Connection con = dataService.createConnection(senderNeedURI, receiverNeedURI, null, facetURI,
                                                        ConnectionState.REQUEST_SENT,
                                                        ConnectionEventType.OWNER_OPEN);

    final WonMessage newWonMessage = createMessageToSendToRemoteNode(wonMessage, con);

    exchange.getIn().setHeader(WonCamelConstants.WON_MESSAGE_EXCHANGE_HEADER, newWonMessage);

    WonMessage successResponse = makeSuccessResponseMessage(wonMessage, con);
    sendMessageToOwner(successResponse, senderNeedURI);
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

  private WonMessage makeSuccessResponseMessage(WonMessage originalMessage, Connection con) {
    WonMessageBuilder wonMessageBuilder = new WonMessageBuilder();
    wonMessageBuilder.setPropertiesForNodeResponse(
            originalMessage,
            true,
            this.wonNodeInformationService.generateEventURI());
    //hack(?): set sender and receiver to the new connection uri to communicate it to the owner.
    // alternative: use a dedicated property (msg:newConnectionUri [uri]) in the message
    wonMessageBuilder.setSenderURI(con.getConnectionURI());
    wonMessageBuilder.setReceiverURI(con.getConnectionURI());
    return wonMessageBuilder.build();
  }

}

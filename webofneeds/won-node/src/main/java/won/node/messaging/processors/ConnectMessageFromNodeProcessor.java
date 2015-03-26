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


@Component
@FixedMessageProcessor(direction= WONMSG.TYPE_FROM_EXTERNAL_STRING,messageType = WONMSG.TYPE_CONNECT_STRING)
public class ConnectMessageFromNodeProcessor extends AbstractCamelProcessor
{


  public void process(final Exchange exchange) throws Exception {
    Message message = exchange.getIn();
    WonMessage wonMessage = (WonMessage) message.getHeader(WonCamelConstants.WON_MESSAGE_HEADER);
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

    //build message to send to owner, put in header
    final WonMessage newWonMessage = createMessageToSendToOwner(wonMessage, con);
    exchange.getIn().setHeader(WonCamelConstants.WON_MESSAGE_HEADER, newWonMessage);
  }

  private WonMessage createMessageToSendToOwner(WonMessage wonMessage, Connection con) {
    //create the message to send to the owner
    return new WonMessageBuilder()
      .setPropertiesForPassingMessageToOwner(wonMessage)
      //set the uri of the newly created connection as receiver
      .setReceiverURI(con.getConnectionURI())
      .build();
  }


}

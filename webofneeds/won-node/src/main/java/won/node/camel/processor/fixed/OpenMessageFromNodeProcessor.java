package won.node.camel.processor.fixed;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.springframework.stereotype.Component;
import won.node.camel.processor.AbstractCamelProcessor;
import won.node.camel.processor.annotation.FixedMessageProcessor;
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
@FixedMessageProcessor(direction= WONMSG.TYPE_FROM_EXTERNAL_STRING,messageType = WONMSG.TYPE_OPEN_STRING)
public class OpenMessageFromNodeProcessor extends AbstractCamelProcessor
{

  public void process(final Exchange exchange) throws Exception {
    Message message = exchange.getIn();
    WonMessage wonMessage = (WonMessage) message.getHeader(WonCamelConstants.MESSAGE_HEADER);

    URI connectionURIFromWonMessage = wonMessage.getReceiverURI();
    Connection con = null;
    if (connectionURIFromWonMessage == null) {
      //the opener didn't know about the connection - maybe it doesn't exist
      URI facet = WonRdfUtils.FacetUtils.getFacet(wonMessage);
      con = dataService.createConnection(wonMessage.getReceiverNeedURI(), wonMessage.getSenderNeedURI(),
        wonMessage.getSenderURI(), facet,
        ConnectionState.REQUEST_RECEIVED, ConnectionEventType.PARTNER_OPEN);
    } else {
      con = connectionRepository.findOneByConnectionURI(connectionURIFromWonMessage);
    }
    assert con != null;
    assert con.getRemoteNeedURI() != null;
    assert con.getRemoteNeedURI().equals(wonMessage.getSenderNeedURI());
    assert con.getRemoteConnectionURI() != null;
    if (wonMessage.getSenderURI() != null){
      assert con.getRemoteConnectionURI().equals(wonMessage.getSenderURI());
    } else {
      con.setRemoteConnectionURI(wonMessage.getSenderURI());
    }
    con.setState(con.getState().transit(ConnectionEventType.PARTNER_OPEN));
    connectionRepository.save(con);

    WonMessage newWonMessage = createMessageToSendToOwner(wonMessage, con.getConnectionURI());
    exchange.getIn().setHeader(WonCamelConstants.MESSAGE_HEADER,newWonMessage);
  }

  private WonMessage createMessageToSendToOwner(WonMessage wonMessage, URI localConnectionURI) {
    //create the message to send to the owner
    WonMessageBuilder builder = WonMessageBuilder
      .setPropertiesForPassingMessageToOwner(wonMessage);
    if (wonMessage.getReceiverURI() == null){
      //if we just created a new connection, add the connection URI as the receiverURI
      builder.setReceiverURI(localConnectionURI);
    }
    return builder.build();
  }


}

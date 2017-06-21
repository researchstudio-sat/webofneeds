package won.node.camel.processor.fixed;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.springframework.stereotype.Component;
import won.node.camel.processor.AbstractCamelProcessor;
import won.node.camel.processor.annotation.FixedMessageProcessor;
import won.protocol.message.WonMessage;
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
      URI connectionUri = wonNodeInformationService.generateConnectionURI(wonMessage.getReceiverNodeURI());
      con = dataService.createConnection(connectionUri, wonMessage.getReceiverNeedURI(), wonMessage.getSenderNeedURI(),
                                         wonMessage.getSenderURI(), facet,
                                         ConnectionState.REQUEST_RECEIVED, ConnectionEventType.PARTNER_OPEN);
    } else {
      con = connectionRepository.findOneByConnectionURIForUpdate(connectionURIFromWonMessage);
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

    //set the receiver to the local connection uri
    wonMessage.addMessageProperty(WONMSG.RECEIVER_PROPERTY, con.getConnectionURI());
  }

}

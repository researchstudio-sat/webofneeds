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
      //the opener didn't know about the connection
      // this happens, for example, when both parties get a hint. Both create a connection, but they don't know
      // about each other.
      // That's why we first try to find a connection with the same needs and facet:

      //let's extract the facet, we'll need it multiple times here
      URI facet = WonRdfUtils.FacetUtils.getFacet(wonMessage);


      con = connectionRepository.findOneByNeedURIAndRemoteNeedURIAndTypeURIForUpdate(
              wonMessage.getReceiverNeedURI(),
              wonMessage.getSenderNeedURI(),
              facet);
      if (con == null) {
        //ok, we really do not know about the connection. create it.
        URI connectionUri = wonNodeInformationService.generateConnectionURI(wonMessage.getReceiverNodeURI());
        con = dataService.createConnection(connectionUri, wonMessage.getReceiverNeedURI(), wonMessage.getSenderNeedURI(),
                wonMessage.getSenderURI(), facet,
                ConnectionState.REQUEST_RECEIVED, ConnectionEventType.PARTNER_OPEN);
      }
    } else {
      //the opener knew about the connection. just load it.
      con = connectionRepository.findOneByConnectionURIForUpdate(connectionURIFromWonMessage);
    }
    //now perform checks
    if (con == null) throw new IllegalStateException("connection must not be null");
    if (con.getRemoteNeedURI() == null) throw new IllegalStateException("remote need uri must not be null");
    if (!con.getRemoteNeedURI().equals(wonMessage.getSenderNeedURI())) throw new IllegalStateException("the remote need uri of the connection must be equal to the sender need uri of the message");
    if (con.getRemoteConnectionURI() == null) throw  new IllegalStateException("the remote connection uri must not be null");
    if (wonMessage.getSenderURI() == null) throw new IllegalStateException("the sender uri must not be null");
    if (!con.getRemoteConnectionURI().equals(wonMessage.getSenderURI())) throw new IllegalStateException("the sender uri of the message must be equal to the remote connection uri");

    //it is possible that we didn't store the reference to the remote conneciton yet. Now we can do it.
    if (con.getRemoteConnectionURI() == null) {
      // Set it from the message (it's the sender of the message)
      con.setRemoteConnectionURI(wonMessage.getSenderURI());
    }
    con.setState(con.getState().transit(ConnectionEventType.PARTNER_OPEN));
    connectionRepository.save(con);

    //set the receiver to the local connection uri
    wonMessage.addMessageProperty(WONMSG.RECEIVER_PROPERTY, con.getConnectionURI());
  }

}

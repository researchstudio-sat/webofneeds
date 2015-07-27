package won.node.camel.processor.fixed;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.springframework.stereotype.Component;
import won.node.camel.processor.AbstractCamelProcessor;
import won.node.camel.processor.annotation.FixedMessageProcessor;
import won.protocol.exception.NoSuchConnectionException;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.message.processor.exception.MissingMessagePropertyException;
import won.protocol.model.Connection;
import won.protocol.model.ConnectionEventType;
import won.protocol.model.ConnectionState;
import won.protocol.util.WonRdfUtils;
import won.protocol.vocabulary.WONMSG;

import java.net.URI;


@Component
@FixedMessageProcessor(
        direction= WONMSG.TYPE_FROM_EXTERNAL_STRING,
        messageType = WONMSG.TYPE_CONNECT_STRING)
public class ConnectMessageFromNodeProcessor extends AbstractCamelProcessor
{


  public void process(final Exchange exchange) throws Exception {
    Message message = exchange.getIn();
    WonMessage wonMessage = (WonMessage) message.getHeader(WonCamelConstants.MESSAGE_HEADER);
    // a need wants to connect.
    // get the required data from the message and create a connection
    URI needUri = wonMessage.getReceiverNeedURI();
    URI remoteNeedUri = wonMessage.getSenderNeedURI();
    URI remoteConnectionUri = wonMessage.getSenderURI();
    URI facetURI = WonRdfUtils.FacetUtils.getRemoteFacet(wonMessage);
    URI connectionURI = wonMessage.getReceiverURI(); //if the uri is known already, we can load the connection!

    if (remoteConnectionUri == null) throw new MissingMessagePropertyException(URI.create(WONMSG.SENDER_PROPERTY.getURI().toString()));

    Connection con = null;
    if (connectionURI != null) {
      con = connectionRepository.findOneByConnectionURI(connectionURI);
      if (con == null) throw new NoSuchConnectionException(connectionURI);
    } else {
      con = connectionRepository.findOneByNeedURIAndRemoteNeedURIAndTypeURI(needUri, remoteNeedUri, facetURI);
    }
    if (con == null){
      //create Connection in Database
      con = dataService.createConnection(needUri, remoteNeedUri, remoteConnectionUri, facetURI,
              ConnectionState.REQUEST_RECEIVED,
              ConnectionEventType.PARTNER_OPEN);
    }
    con.setRemoteConnectionURI(remoteConnectionUri);
    con.setState(con.getState().transit(ConnectionEventType.PARTNER_OPEN));
    connectionRepository.save(con);


    //build message to send to owner, put in header
    final WonMessage newWonMessage = createMessageToSendToOwner(wonMessage, con);
    exchange.getIn().setHeader(WonCamelConstants.MESSAGE_HEADER, newWonMessage);
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

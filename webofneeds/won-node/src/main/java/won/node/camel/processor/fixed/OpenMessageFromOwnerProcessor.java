package won.node.camel.processor.fixed;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.springframework.stereotype.Component;
import won.node.camel.processor.AbstractFromOwnerCamelProcessor;
import won.node.camel.processor.annotation.FixedMessageProcessor;
import won.node.camel.processor.general.OutboundMessageFactoryProcessor;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.message.processor.exception.WonMessageProcessingException;
import won.protocol.model.Connection;
import won.protocol.model.ConnectionEventType;
import won.protocol.vocabulary.WONMSG;

import java.net.URI;

/**
 * User: syim
 * Date: 02.03.2015
 */
@Component
@FixedMessageProcessor(direction = WONMSG.TYPE_FROM_OWNER_STRING,messageType = WONMSG.TYPE_OPEN_STRING)
public class OpenMessageFromOwnerProcessor extends AbstractFromOwnerCamelProcessor
{

  public void process(final Exchange exchange) throws Exception {
    Message message = exchange.getIn();
    WonMessage wonMessage = (WonMessage) message.getHeader(WonCamelConstants.MESSAGE_HEADER);

    logger.debug("OPEN received from the owner side for connection {}", wonMessage.getSenderURI());

    Connection con = dataService.nextConnectionState(wonMessage.getSenderURI(), ConnectionEventType.OWNER_OPEN);
    assert con != null;
    assert con.getRemoteNeedURI() != null;
    assert con.getRemoteNeedURI().equals(wonMessage.getReceiverNeedURI());
    assert con.getConnectionURI() != null;
    assert con.getConnectionURI().equals(wonMessage.getSenderURI());
    if (wonMessage.getReceiverURI() != null){
      assert con.getRemoteConnectionURI().equals(wonMessage.getReceiverURI());
    } else {
      con.setRemoteConnectionURI(wonMessage.getReceiverURI());
    }
    con.setState(con.getState().transit(ConnectionEventType.OWNER_OPEN));
    connectionRepository.save(con);

    URI remoteMessageUri = wonNodeInformationService
            .generateEventURI(wonMessage.getReceiverNodeURI());

    //add the information about the corresponding message to the local one
    wonMessage.addMessageProperty(WONMSG.HAS_CORRESPONDING_REMOTE_MESSAGE, remoteMessageUri);
    //the persister will pick it up later

    //put the factory into the outbound message factory header. It will be used to generate the outbound message
    //after the wonMessage has been processed and saved, to make sure that the outbound message contains
    //all the data that we also store locally
    OutboundMessageFactory outboundMessageFactory = new OutboundMessageFactory(remoteMessageUri, con);
    exchange.getIn().setHeader(WonCamelConstants.OUTBOUND_MESSAGE_FACTORY_HEADER, outboundMessageFactory);

  }

  private class OutboundMessageFactory extends OutboundMessageFactoryProcessor
  {
    private Connection connection;

    public OutboundMessageFactory(URI messageURI, Connection connection) {
      super(messageURI);
      this.connection = connection;
    }

    @Override
    public WonMessage process(WonMessage message) throws WonMessageProcessingException {
      //create the message to send to the remote node
      return WonMessageBuilder
              .setPropertiesForPassingMessageToRemoteNode(
                      message ,
                      getMessageURI())
              .setSenderURI(connection.getConnectionURI())
              .build();
    }
  }

}

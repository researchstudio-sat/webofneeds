package won.node.camel.processor.fixed;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.springframework.stereotype.Component;
import won.node.camel.processor.AbstractFromOwnerCamelProcessor;
import won.node.camel.processor.annotation.FixedMessageProcessor;
import won.protocol.exception.IllegalMessageForConnectionStateException;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.message.processor.exception.MissingMessagePropertyException;
import won.protocol.model.Connection;
import won.protocol.model.ConnectionState;
import won.protocol.vocabulary.WONMSG;

import java.net.URI;

/**
 * User: syim
 * Date: 02.03.2015
 */
@Component
@FixedMessageProcessor(direction= WONMSG.TYPE_FROM_OWNER_STRING,messageType = WONMSG.TYPE_CONNECTION_MESSAGE_STRING)
public class SendMessageFromOwnerProcessor extends AbstractFromOwnerCamelProcessor
{

  public void process(final Exchange exchange) throws Exception {
    Message message = exchange.getIn();
    WonMessage wonMessage = (WonMessage) message.getHeader(WonCamelConstants.MESSAGE_HEADER);
    URI connectionUri = wonMessage.getSenderURI();
    if (connectionUri == null){
      throw new MissingMessagePropertyException(URI.create(WONMSG.SENDER_PROPERTY.toString()));
    }
    Connection con = connectionRepository.findOneByConnectionURI(connectionUri);
    if (con.getState() != ConnectionState.CONNECTED) {
      throw new IllegalMessageForConnectionStateException(connectionUri, "CONNECTION_MESSAGE", con.getState());
    }
    WonMessage newWonMessage = createMessageToSendToRemoteNode(wonMessage, con);
    //add the information about the remote message to the locally stored one
    WonMessageBuilder builder = WonMessageBuilder.wrap(wonMessage)
            .setCorrespondingRemoteMessageURI(newWonMessage.getMessageURI());
    if (wonMessage.getReceiverURI() == null){
       builder.setReceiverURI(con.getRemoteConnectionURI());
    }
    wonMessage = builder.build();
    //put it into the header so the persister will pick it up later
    message.setHeader(WonCamelConstants.MESSAGE_HEADER,wonMessage);

    exchange.getIn().setHeader(WonCamelConstants.OUTBOUND_MESSAGE_HEADER,newWonMessage);
  }

  private WonMessage createMessageToSendToRemoteNode(WonMessage wonMessage, final Connection con) {
    //create the message to send to the remote node
    return WonMessageBuilder
      .setPropertiesForPassingMessageToRemoteNode(
        wonMessage,
        wonNodeInformationService
          .generateEventURI(wonMessage.getReceiverNodeURI()))
      .setReceiverURI(con.getRemoteConnectionURI())
      .build();
  }


}

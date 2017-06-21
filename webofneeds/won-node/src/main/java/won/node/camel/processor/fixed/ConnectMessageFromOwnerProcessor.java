package won.node.camel.processor.fixed;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.springframework.stereotype.Component;
import won.node.camel.processor.AbstractFromOwnerCamelProcessor;
import won.node.camel.processor.annotation.FixedMessageProcessor;
import won.protocol.exception.NoSuchConnectionException;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.model.Connection;
import won.protocol.model.ConnectionEventType;
import won.protocol.model.ConnectionState;
import won.protocol.model.MessageEventPlaceholder;
import won.protocol.util.WonRdfUtils;
import won.protocol.vocabulary.WONMSG;

import java.net.URI;

/**
 * User: syim
 * Date: 02.03.2015
 */
@Component
@FixedMessageProcessor(direction= WONMSG.TYPE_FROM_OWNER_STRING,messageType = WONMSG.TYPE_CONNECT_STRING)
public class ConnectMessageFromOwnerProcessor extends AbstractFromOwnerCamelProcessor
{
  public void process(final Exchange exchange) throws Exception {
    Message message = exchange.getIn();
    WonMessage wonMessage = (WonMessage) message.getHeader(WonCamelConstants.MESSAGE_HEADER);
    URI senderNeedURI = wonMessage.getSenderNeedURI();
    URI senderNodeURI = wonMessage.getSenderNodeURI();
    URI receiverNeedURI = wonMessage.getReceiverNeedURI();
    URI facetURI = WonRdfUtils.FacetUtils.getFacet(wonMessage);
    URI connectionURI = wonMessage.getSenderURI(); //if the uri is known already, we can load the connection!
    Connection con = null;
    if (connectionURI != null) {
      con = connectionRepository.findOneByConnectionURIForUpdate(connectionURI);
      if (con == null) throw new NoSuchConnectionException(connectionURI);
    } else {
      con = connectionRepository.findOneByNeedURIAndRemoteNeedURIAndTypeURIForUpdate(senderNeedURI, receiverNeedURI, facetURI);
    }
    if (con == null){
      //create Connection in Database
      URI connectionUri = wonNodeInformationService.generateConnectionURI(senderNodeURI);
      con = dataService.createConnection(connectionUri, senderNeedURI, receiverNeedURI, null, facetURI,
                                         ConnectionState.REQUEST_SENT,
                                         ConnectionEventType.OWNER_OPEN);
    }
    con.setState(con.getState().transit(ConnectionEventType.OWNER_OPEN));
    connectionRepository.save(con);
    //prepare the message to pass to the remote node
    final WonMessage newWonMessage = createMessageToSendToRemoteNode(wonMessage, con);
    //set the sender uri in the envelope TODO: TwoMsgs: do not set sender here
    wonMessage.addMessageProperty(WONMSG.SENDER_PROPERTY, con.getConnectionURI());
    //add the information about the new local connection to the original message
    wonMessage.addMessageProperty(WONMSG.HAS_CORRESPONDING_REMOTE_MESSAGE, newWonMessage.getMessageURI());
    //the persister will pick it up later

    //put the new message into the 'modified message' header (so the persister doesn't pick up the wrong one).
    message.setHeader(WonCamelConstants.OUTBOUND_MESSAGE_HEADER, newWonMessage);
  }

  private WonMessage createMessageToSendToRemoteNode(WonMessage wonMessage, Connection con) {
    //create the message to send to the remote node
    return WonMessageBuilder
            .setPropertiesForPassingMessageToRemoteNode(
                    wonMessage,
                    wonNodeInformationService
                            .generateEventURI(wonMessage.getReceiverNodeURI()))
            .setSenderURI(con.getConnectionURI())
            .build();
  }

  @Override
  public void onSuccessResponse(final Exchange exchange) throws Exception {
    WonMessage responseMessage = (WonMessage) exchange.getIn().getHeader(WonCamelConstants.MESSAGE_HEADER);
    MessageEventPlaceholder mep = this.messageEventRepository.findOneByCorrespondingRemoteMessageURI(
      responseMessage
        .getIsResponseToMessageURI());
    //update the connection database: set the remote connection URI just obtained from the response
    Connection con = this.connectionRepository.findOneByConnectionURIForUpdate(mep.getSenderURI());
    con.setRemoteConnectionURI(responseMessage.getSenderURI());
    this.connectionRepository.save(con);
  }

  @Override
  public void onFailureResponse(final Exchange exchange) throws Exception {
    //TODO: define what to do if the connect fails remotely option: create a system message of type CLOSE,
    // and forward it only to the owner. Add an explanation (a reference to the failure response and some
    // expplanation text.
    logger.warn("The remote end responded with a failure message. Our behaviour is now undefined.");
  }
}

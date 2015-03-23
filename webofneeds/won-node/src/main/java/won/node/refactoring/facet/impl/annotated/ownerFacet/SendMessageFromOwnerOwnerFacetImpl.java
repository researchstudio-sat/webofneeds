package won.node.refactoring.facet.impl.annotated.ownerFacet;

import org.apache.camel.Exchange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import won.node.messaging.processors.AbstractInOnlyMessageProcessor;
import won.node.messaging.processors.DefaultFacetMessageProcessor;
import won.node.messaging.processors.FacetMessageProcessor;
import won.protocol.exception.IllegalMessageForConnectionStateException;
import won.protocol.exception.NoSuchConnectionException;
import won.protocol.message.WonMessage;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.model.FacetType;
import won.protocol.repository.ConnectionRepository;
import won.protocol.vocabulary.WON;
import won.protocol.vocabulary.WONMSG;

/**
 * User: syim
 * Date: 05.03.2015
 */
@Component
@DefaultFacetMessageProcessor(direction=WONMSG.TYPE_FROM_OWNER_STRING,messageType = WONMSG
  .TYPE_CONNECTION_MESSAGE_STRING)
@FacetMessageProcessor(facetType = WON.OWNER_FACET_STRING,direction=WONMSG.TYPE_FROM_OWNER_STRING,messageType =
  WONMSG.TYPE_CONNECTION_MESSAGE_STRING)
public class SendMessageFromOwnerOwnerFacetImpl extends AbstractInOnlyMessageProcessor
{

  @Autowired
  ConnectionRepository connectionRepository;
  final FacetType facetType = FacetType.OwnerFacet;


  public FacetType getFacetType() {
    return facetType;
  }

  /**
   * This function is invoked when an owner sends a text message to a won node and usually executes registered facet specific code.
   * It is used to indicate the sending of a chat message with by the specified connection object con
   * to the remote partner.
   *
   *
   * @throws NoSuchConnectionException if connectionURI does not refer to an existing connection
   * @throws IllegalMessageForConnectionStateException if the message is not allowed in the current state of the connection
   */

  @Override
  public void process(final Exchange exchange) {
    WonMessage wonMessage = (WonMessage) exchange.getIn().getHeader(WonCamelConstants.WON_MESSAGE_EXCHANGE_HEADER);
    //just send the message
    this.sendMessageToOwner(wonMessage, wonMessage.getReceiverNeedURI());
  }
}

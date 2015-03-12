package won.node.refactoring.facet.impl.annotated.ownerFacet;

import com.hp.hpl.jena.rdf.model.Model;
import org.springframework.stereotype.Component;
import won.node.annotation.DefaultFacetMessageProcessor;
import won.node.annotation.FacetMessageProcessor;
import won.node.refactoring.FacetCamel;
import won.node.refactoring.facet.impl.annotated.AbstractFacetAnnotated;
import won.protocol.exception.IllegalMessageForConnectionStateException;
import won.protocol.exception.NoSuchConnectionException;
import won.protocol.message.WonMessage;
import won.protocol.model.Connection;
import won.protocol.model.FacetType;
import won.protocol.vocabulary.WONMSG;

/**
 * User: syim
 * Date: 05.03.2015
 */
@Component
@DefaultFacetMessageProcessor(direction=WONMSG.TYPE_FROM_NODE_STRING,messageType = WONMSG.TYPE_CONNECTION_MESSAGE_STRING)
@FacetMessageProcessor(facetType = WONMSG.OWNER_FACET_STRING,direction=WONMSG.TYPE_FROM_NODE_STRING,messageType =
  WONMSG.TYPE_CONNECTION_MESSAGE_STRING)
public class SendMessageFromNodeOwnerFacetImpl extends AbstractFacetAnnotated implements FacetCamel
{

  final FacetType facetType = FacetType.OwnerFacet;


  public FacetType getFacetType() {
    return facetType;
  }

  /**
   * This function is invoked when a won node sends a text message to another won node and usually executes registered facet specific code.
   * It is used to indicate the sending of a chat message with by the specified connection object con
   * to the remote partner.
   *
   *
   * @param con the connection object
   * @param message  the chat message
   * @throws NoSuchConnectionException if connectionURI does not refer to an existing connection
   * @throws IllegalMessageForConnectionStateException if the message is not allowed in the current state of the connection
   */
  @Override
  public void sendMessageFromNeed(final Connection con, final Model message, final WonMessage wonMessage)
    throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
    //send to the need side
    executorService.execute(new Runnable() {
      @Override
      public void run() {
        try {
          ownerFacingConnectionClient.sendMessage(con.getConnectionURI(), message, wonMessage);
        } catch (Exception e) {
          logger.warn("caught Exception in textMessageFromNeed:", e);
        }
      }
    });
  }


  @Override
  public void process(WonMessage wonMessage) {

  }
}

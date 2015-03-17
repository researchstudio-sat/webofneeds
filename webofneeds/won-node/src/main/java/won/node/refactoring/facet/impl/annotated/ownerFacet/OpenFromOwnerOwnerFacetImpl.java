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
import won.protocol.vocabulary.WON;
import won.protocol.vocabulary.WONMSG;

/**
 * User: syim
 * Date: 05.03.2015
 */
@Component
@DefaultFacetMessageProcessor(direction=WONMSG.TYPE_FROM_OWNER_STRING,messageType = WONMSG.TYPE_OPEN_STRING)
@FacetMessageProcessor(facetType = WON.OWNER_FACET_STRING,direction=WONMSG.TYPE_FROM_OWNER_STRING,messageType =
  WONMSG.TYPE_OPEN_STRING)
public class OpenFromOwnerOwnerFacetImpl extends AbstractFacetAnnotated implements FacetCamel
{

  final FacetType facetType = FacetType.OwnerFacet;


  public FacetType getFacetType() {
    return facetType;
  }

  /**
   *
   * This function is invoked when an owner sends an open message to a won node and usually executes registered facet specific code.
   * It is used to open a connection which is identified by the connection object con. A rdf graph can be sent along with the request.
   *
   * @throws NoSuchConnectionException if connectionURI does not refer to an existing connection
   * @throws IllegalMessageForConnectionStateException if the message is not allowed in the current state of the connection
   */

  @Override
  public void process(final WonMessage wonMessage) {
    //inform the other side
    //in an 'open' call the local and the remote connection URI are always known and must be present
    //in the con object.

    final Connection con = connectionRepository.findOneByConnectionURI(wonMessage.getSenderURI());
    //TODO: only for old messaging protocol. remove it when transition is finished.
    final Model content = wonMessage.getMessageContent().getNamedModel(wonMessage.getMessageContent().listNames().next
      ());
    if (wonMessage.getReceiverURI() != null) {
      executorService.execute(new Runnable()
      {
        @Override
        public void run() {
          try {
            needFacingConnectionClient.open(con, content, wonMessage);
          } catch (Exception e) {
            logger.warn("caught Exception in openFromOwner", e);
          }
        }
      });
    }
  }
}

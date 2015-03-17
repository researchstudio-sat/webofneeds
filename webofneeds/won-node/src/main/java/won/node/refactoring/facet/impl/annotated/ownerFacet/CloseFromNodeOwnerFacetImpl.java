package won.node.refactoring.facet.impl.annotated.ownerFacet;

import com.hp.hpl.jena.rdf.model.Model;
import org.springframework.stereotype.Component;
import won.node.annotation.DefaultFacetMessageProcessor;
import won.node.annotation.FacetMessageProcessor;
import won.node.refactoring.FacetCamel;
import won.node.refactoring.facet.impl.annotated.AbstractFacetAnnotated;
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
@DefaultFacetMessageProcessor(direction=WONMSG.TYPE_FROM_EXTERNAL_STRING,messageType = WONMSG.TYPE_CLOSE_STRING)
@FacetMessageProcessor(facetType = WON.OWNER_FACET_STRING,direction=WONMSG.TYPE_FROM_EXTERNAL_STRING,messageType =
  WONMSG.TYPE_CLOSE_STRING)
public class CloseFromNodeOwnerFacetImpl extends AbstractFacetAnnotated implements FacetCamel
{
  final FacetType facetType = FacetType.OwnerFacet;


  public FacetType getFacetType() {
    return facetType;
  }




  @Override
  public void process(final WonMessage wonMessage) {
    final Connection con = connectionRepository.findOneByConnectionURI(wonMessage.getReceiverURI());
    //TODO: only for old messaging protocol. remove it when transition is finished.
    final Model content = wonMessage.getMessageContent().getNamedModel(wonMessage.getMessageContent().listNames().next
      ());
    //inform the need side
    executorService.execute(new Runnable()
    {
      @Override
      public void run() {
        try {
          ownerFacingConnectionClient.close(con.getConnectionURI(), content, wonMessage);
        } catch (Exception e) {
          logger.warn("caught Exception in closeFromNeed:", e);
        }
      }
    });
  }
}

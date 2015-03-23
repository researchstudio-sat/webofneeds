package won.node.refactoring.facet.impl.annotated.ownerFacet;

import com.hp.hpl.jena.rdf.model.Model;
import org.springframework.stereotype.Component;
import won.node.messaging.processors.DefaultFacetMessageProcessor;
import won.node.messaging.processors.FacetMessageProcessor;
import won.node.refactoring.FacetCamel;
import won.node.refactoring.facet.impl.annotated.AbstractFacetAnnotated;
import won.protocol.message.WonMessage;
import won.protocol.model.Connection;
import won.protocol.model.FacetType;
import won.protocol.vocabulary.WON;
import won.protocol.vocabulary.WONMSG;

import java.net.URI;

/**
 * User: syim
 * Date: 05.03.2015
 */
@Component
@DefaultFacetMessageProcessor(direction=WONMSG.TYPE_FROM_OWNER_STRING,messageType = WONMSG.TYPE_CLOSE_STRING)
@FacetMessageProcessor(facetType = WON.OWNER_FACET_STRING,direction=WONMSG.TYPE_FROM_OWNER_STRING,messageType =
  WONMSG.TYPE_CLOSE_STRING)
public class CloseFromOwnerOwnerFacetImpl extends AbstractFacetAnnotated implements FacetCamel
{
  final FacetType facetType = FacetType.OwnerFacet;


  public FacetType getFacetType() {
    return facetType;
  }



  @Override
  public void process(final WonMessage wonMessage) {
    //inform the other side
    //TODO: don't inform the other side if there is none (suggested, request_sent states)
    //TODO: only for old messaging protocol. remove it when transition is finished.
    final Model content = wonMessage.getMessageContent().getNamedModel(wonMessage.getMessageContent().listNames().next
      ());
    final Connection con = connectionRepository.findOneByConnectionURI(wonMessage.getSenderURI());
    URI remoteURI = con.getRemoteConnectionURI();
    if (remoteURI != null) {
      executorService.execute(new Runnable()
      {
        @Override
        public void run()
        {
          try {
            needFacingConnectionClient.close(con, content, wonMessage);
          } catch (Exception e) {
            logger.warn("caught Exception in closeFromOwner: ",e);
          }
        }
      });
    }
  }
}

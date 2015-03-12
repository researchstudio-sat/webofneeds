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
@DefaultFacetMessageProcessor(direction=WONMSG.TYPE_FROM_OWNER_STRING,messageType = WONMSG.TYPE_CLOSE_STRING)
@FacetMessageProcessor(facetType = WONMSG.OWNER_FACET_STRING,direction=WONMSG.TYPE_FROM_OWNER_STRING,messageType =
  WONMSG.TYPE_CLOSE_STRING)
public class CloseFromOwnerOwnerFacetImpl extends AbstractFacetAnnotated implements FacetCamel
{

  final FacetType facetType = FacetType.OwnerFacet;


  public FacetType getFacetType() {
    return facetType;
  }

  @Override
  public void closeFromOwner(final Connection con, final Model content, final WonMessage wonMessage)
    throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
    //inform the other side
    //TODO: don't inform the other side if there is none (suggested, request_sent states)
    if (con.getRemoteConnectionURI() != null) {
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


  @Override
  public void process(WonMessage wonMessage) {

  }
}

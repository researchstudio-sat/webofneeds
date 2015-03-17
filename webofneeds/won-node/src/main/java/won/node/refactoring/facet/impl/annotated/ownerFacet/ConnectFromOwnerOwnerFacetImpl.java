package won.node.refactoring.facet.impl.annotated.ownerFacet;

import com.hp.hpl.jena.rdf.model.Model;
import org.apache.camel.Header;
import org.springframework.stereotype.Component;
import won.node.annotation.FacetMessageProcessor;
import won.node.refactoring.FacetCamel;
import won.node.refactoring.facet.impl.annotated.AbstractFacetAnnotated;
import won.protocol.exception.ConnectionAlreadyExistsException;
import won.protocol.exception.IllegalMessageForNeedStateException;
import won.protocol.exception.NoSuchNeedException;
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
@FacetMessageProcessor(facetType = WON.OWNER_FACET_STRING,direction=WONMSG.TYPE_FROM_OWNER_STRING,messageType =
  WONMSG.TYPE_CONNECT_STRING)
public class ConnectFromOwnerOwnerFacetImpl extends AbstractFacetAnnotated implements FacetCamel
{

  public FacetType getFacetType() {
    return FacetType.OwnerFacet;
  }



  @Override
  public void process(@Header("wonMessage")final WonMessage wonMessage)
    throws NoSuchNeedException, IllegalMessageForNeedStateException, ConnectionAlreadyExistsException {
    //TODO: only for old messaging protocol. remove it when transition is finished.
    final Model content = wonMessage.getMessageContent().getNamedModel(wonMessage.getMessageContent().listNames().next
      ());
    final Connection con = connectionRepository.findOneByConnectionURI(wonMessage.getSenderURI());
    super.connectFromOwner(con, content, wonMessage);
    /* when connected change linked data*/

  }
}

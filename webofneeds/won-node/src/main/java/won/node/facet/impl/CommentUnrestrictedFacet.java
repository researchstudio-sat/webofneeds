package won.node.facet.impl;

import com.hp.hpl.jena.rdf.model.Model;
import won.protocol.exception.*;
import won.protocol.model.Connection;
import won.protocol.model.FacetType;

/**
 * User: gabriel
 * Date: 17/01/14
 */
public class CommentUnrestrictedFacet extends Facet {
  @Override
  public FacetType getFacetType() {
    return FacetType.CommentUnrestrictedFacet;
  }

  @Override
  public void connectFromNeed(Connection con, Model content) throws NoSuchNeedException, IllegalMessageForNeedStateException, ConnectionAlreadyExistsException {
    super.connectFromNeed(con, content);
    /* send a connect back */
    try {
      needFacingConnectionClient.open(con, content);
    } catch (NoSuchConnectionException e) {
      e.printStackTrace();
    } catch (IllegalMessageForConnectionStateException e) {
      e.printStackTrace();
    }

    /* when connected change linked data*/
  }
}

package won.node.facet.impl;

import com.hp.hpl.jena.rdf.model.Model;
import won.protocol.exception.ConnectionAlreadyExistsException;
import won.protocol.exception.IllegalMessageForNeedStateException;
import won.protocol.exception.NoSuchNeedException;
import won.protocol.model.Connection;
import won.protocol.model.FacetType;

/**
 * User: gabriel
 * Date: 17/01/14
 */
public class CommentFacet extends Facet  {
  @Override
  public FacetType getFacetType() {
    return null;
  }

  @Override
  public void connectFromNeed(Connection con, Model content) throws NoSuchNeedException, IllegalMessageForNeedStateException, ConnectionAlreadyExistsException {
    super.connectFromNeed(con, content);
    /* when connected change linked data*/
  }
}

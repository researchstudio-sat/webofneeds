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
public class CommentModeratedFacet extends AbstractFacet
{
  @Override
  public FacetType getFacetType() {
    return FacetType.CommentModeratedFacet;
  }

  @Override
  public void connectFromOwner(Connection con, Model content) throws NoSuchNeedException, IllegalMessageForNeedStateException, ConnectionAlreadyExistsException {
    super.connectFromOwner(con, content);
    /* when connected change linked data*/
  }
}

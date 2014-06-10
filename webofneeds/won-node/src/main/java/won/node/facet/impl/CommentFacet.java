package won.node.facet.impl;

import com.hp.hpl.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.protocol.exception.ConnectionAlreadyExistsException;
import won.protocol.exception.IllegalMessageForNeedStateException;
import won.protocol.exception.NoSuchNeedException;
import won.protocol.model.Connection;
import won.protocol.model.FacetType;

/**
 * User: gabriel
 * Date: 17/01/14
 */
public class CommentFacet extends AbstractFacet
{
  private Logger logger = LoggerFactory.getLogger(this.getClass());

  @Override
  public FacetType getFacetType() {
    return FacetType.CommentFacet;
  }

  @Override
  public void connectFromNeed(final Connection con, final Model content) throws NoSuchNeedException,
    IllegalMessageForNeedStateException, ConnectionAlreadyExistsException {


    super.connectFromNeed(con, content);

  }

}

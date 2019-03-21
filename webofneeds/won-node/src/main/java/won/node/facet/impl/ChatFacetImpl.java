package won.node.facet.impl;

import org.apache.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.protocol.exception.ConnectionAlreadyExistsException;
import won.protocol.exception.IllegalMessageForNeedStateException;
import won.protocol.exception.NoSuchNeedException;
import won.protocol.message.WonMessage;
import won.protocol.model.Connection;
import won.protocol.model.FacetType;

/**
 * Created with IntelliJ IDEA.
 * User: gabriel
 * Date: 16.09.13
 * Time: 18:42
 * To change this template use File | Settings | File Templates.
 */
public class ChatFacetImpl extends AbstractFacet {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Override public FacetType getFacetType() {
    return FacetType.ChatFacet;
  }

  @Override public void connectFromNeed(final Connection con, final Model content, final WonMessage wonMessage)
      throws NoSuchNeedException, IllegalMessageForNeedStateException, ConnectionAlreadyExistsException {

    super.connectFromNeed(con, content, wonMessage);
    /* when connected change linked data*/

  }

}

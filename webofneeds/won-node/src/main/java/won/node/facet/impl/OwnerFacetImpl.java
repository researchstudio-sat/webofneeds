package won.node.facet.impl;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.shared.PrefixMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.protocol.exception.ConnectionAlreadyExistsException;
import won.protocol.exception.IllegalMessageForNeedStateException;
import won.protocol.exception.NoSuchNeedException;
import won.protocol.model.Connection;
import won.protocol.model.FacetType;
import won.protocol.util.RdfUtils;
import won.protocol.vocabulary.SIOC;

/**
 * Created with IntelliJ IDEA.
 * User: gabriel
 * Date: 16.09.13
 * Time: 18:42
 * To change this template use File | Settings | File Templates.
 */
public class OwnerFacetImpl extends AbstractFacet
{
    private final Logger logger = LoggerFactory.getLogger(getClass());

  @Override
  public FacetType getFacetType() {
      return FacetType.OwnerFacet;
  }

  @Override
  public void connectFromNeed(final Connection con, final Model content) throws NoSuchNeedException,
    IllegalMessageForNeedStateException, ConnectionAlreadyExistsException {

    super.connectFromNeed(con, content);
    /* when connected change linked data*/
    Model needContent = rdfStorageService.loadContent(con.getNeedURI());
    PrefixMapping prefixMapping = PrefixMapping.Factory.create();
//    prefixMapping.setNsPrefix(SIOC.getURI(),"sioc");
    needContent.withDefaultMappings(prefixMapping);
    needContent.setNsPrefix("sioc", SIOC.getURI());
    Resource post = needContent.createResource(con.getNeedURI().toString(), SIOC.POST);
    Resource reply = needContent.createResource(con.getRemoteNeedURI().toString(),SIOC.POST);
    needContent.add(needContent.createStatement(needContent.getResource(con.getNeedURI().toString()), SIOC.HAS_REPLY,
                                                needContent.getResource(con.getRemoteNeedURI().toString())));

    // add WON node link
    logger.debug("linked data:"+ RdfUtils.toString(needContent));
    rdfStorageService.storeContent(con.getNeedURI(),needContent);

  }
}

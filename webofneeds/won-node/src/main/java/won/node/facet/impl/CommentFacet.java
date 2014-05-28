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
    /* when connected change linked data*/
    Model needContent = rdfStorageService.loadContent(con.getNeedURI());
    PrefixMapping prefixMapping = PrefixMapping.Factory.create();
//    prefixMapping.setNsPrefix(SIOC.getURI(),"sioc");
    needContent.withDefaultMappings(prefixMapping);
    needContent.setNsPrefix("sioc",SIOC.getURI());
    Resource post = needContent.createResource(con.getNeedURI().toString(), SIOC.POST);
    Resource reply = needContent.createResource(con.getRemoteNeedURI().toString(),SIOC.POST);
    needContent.add(needContent.createStatement(needContent.getResource(con.getNeedURI().toString()), SIOC.HAS_REPLY,
                                                needContent.getResource(con.getRemoteNeedURI().toString())));

    // add WON node link
    logger.debug("linked data:"+ RdfUtils.toString(needContent));
    rdfStorageService.storeContent(con.getNeedURI(),needContent);

  }
  /*
  @Override
  public void connectFromOwner(final Connection con, final Model content) throws NoSuchNeedException, IllegalMessageForNeedStateException, ConnectionAlreadyExistsException {
    //TODO: disallow this call - as it would mean that the need asks the remote need if it wants to be
    //a comment - that would be too strange
    super.connectFromOwner(con, content);
    Model needContent = rdfStorageService.loadContent(con.getNeedURI());
    PrefixMapping prefixMapping = PrefixMapping.Factory.create();
//    prefixMapping.setNsPrefix(SIOC.getURI(),"sioc");
    needContent.withDefaultMappings(prefixMapping);
    needContent.setNsPrefix("sioc",SIOC.getURI());
    Resource post = needContent.createResource(con.getNeedURI().toString(), SIOC.POST);
    Resource reply = needContent.createResource(con.getRemoteNeedURI().toString(),SIOC.POST);
    needContent.add(needContent.createStatement(needContent.getResource(con.getNeedURI().toString()), SIOC.HAS_REPLY,
                                            needContent.getResource(con.getRemoteNeedURI().toString())));

    // add WON node link
    logger.debug("linked data:"+ RdfUtils.toString(needContent));
    rdfStorageService.storeContent(con.getNeedURI(),needContent);

  }
      */
}
